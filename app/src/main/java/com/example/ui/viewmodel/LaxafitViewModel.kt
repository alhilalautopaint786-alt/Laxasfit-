package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.YogaAiGenerator
import com.example.data.catalog.BeginnerYogaCatalog
import com.example.data.catalog.SportMode
import com.example.data.model.UserProfile
import com.example.data.model.WatchSyncRecord
import com.example.data.model.YogaRoutine
import com.example.data.model.YogaSessionLog
import com.example.data.repository.LaxafitRepository
import com.example.watch.WatchH9BleManager
import com.example.watch.WatchH9State
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiGenerationState {
    data object Idle : AiGenerationState
    data object Generating : AiGenerationState
    data class Ready(val routine: YogaRoutine, val savedRoutineId: String? = null) : AiGenerationState
    data class Error(val message: String) : AiGenerationState
}

data class LaxafitDashboardData(
    val profile: UserProfile? = null,
    val savedRoutines: List<YogaRoutine> = emptyList(),
    val curatedRoutines: List<YogaRoutine> = BeginnerYogaCatalog.curatedRoutines,
    val sessions: List<YogaSessionLog> = emptyList(),
    val watchSyncs: List<WatchSyncRecord> = emptyList(),
)

class LaxafitViewModel(
    private val repository: LaxafitRepository,
    private val aiGenerator: YogaAiGenerator,
    val watchManager: WatchH9BleManager,
    private val currentUserId: String,
    initialDisplayName: String,
) : ViewModel() {

    private val _aiState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val aiState: StateFlow<AiGenerationState> = _aiState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _selectedRoutine = MutableStateFlow<YogaRoutine?>(null)
    val selectedRoutine: StateFlow<YogaRoutine?> = _selectedRoutine.asStateFlow()

    private val _selectedSportMode = MutableStateFlow<SportMode?>(null)
    val selectedSportMode: StateFlow<SportMode?> = _selectedSportMode.asStateFlow()

    val watchState: StateFlow<WatchH9State> = watchManager.state

    val dashboardState: StateFlow<UiState<LaxafitDashboardData>> = combine(
        repository.observeProfile(currentUserId),
        repository.observeRoutines(currentUserId),
        repository.observeSessions(currentUserId),
        repository.observeWatchSyncs(currentUserId),
    ) { profile, routines, sessions, watchSyncs ->
        UiState.Success(
            LaxafitDashboardData(
                profile = profile,
                savedRoutines = routines,
                curatedRoutines = BeginnerYogaCatalog.curatedRoutines,
                sessions = sessions,
                watchSyncs = watchSyncs,
            )
        ) as UiState<LaxafitDashboardData>
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing dashboard streams", rootError)
        emit(UiState.Error(rootError.message ?: "Unable to sync your Laxafit data."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
    )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(initialDisplayName)
                .onFailure { e ->
                    Log.w(TAG, "Failed to ensure profile", e)
                }
        }
    }

    fun selectRoutine(routine: YogaRoutine) {
        _selectedRoutine.value = routine
    }

    fun selectSportMode(mode: SportMode) {
        _selectedSportMode.value = mode
    }

    fun findRoutineById(routineId: String): YogaRoutine? {
        val data = (dashboardState.value as? UiState.Success)?.data
        return data?.savedRoutines?.firstOrNull { it.id == routineId }
            ?: data?.curatedRoutines?.firstOrNull { it.id == routineId }
            ?: (_aiState.value as? AiGenerationState.Ready)?.routine?.takeIf { it.id == routineId }
            ?: _selectedRoutine.value?.takeIf { it.id == routineId }
    }

    fun generatePersonalizedRoutine(
        focusArea: String,
        experienceLevel: String,
        durationMinutes: Int,
        userNote: String,
        useWatchH9Metrics: Boolean,
    ) {
        viewModelScope.launch {
            _aiState.value = AiGenerationState.Generating
            val dash = (dashboardState.value as? UiState.Success)?.data
            val liveHr = watchState.value.liveHeartRateBpm
                ?: dash?.profile?.lastRestingHr
                ?: dash?.watchSyncs?.firstOrNull()?.heartRateBpm
            val recoveryStatus = dash?.watchSyncs?.firstOrNull()?.recoveryStatus

            val result = aiGenerator.generatePersonalizedRoutine(
                userId = currentUserId,
                focusArea = focusArea,
                experienceLevel = experienceLevel,
                durationMinutes = durationMinutes,
                userNote = userNote,
                watchH9HeartRate = if (useWatchH9Metrics) liveHr else null,
                watchH9RecoveryStatus = if (useWatchH9Metrics) recoveryStatus else null,
            )

            result.fold(
                onSuccess = { generated ->
                    val savedId = repository.saveRoutine(generated).getOrNull()
                    val finalRoutine = if (savedId != null) generated.copy(id = savedId) else generated
                    _selectedRoutine.value = finalRoutine
                    _aiState.value = AiGenerationState.Ready(finalRoutine, savedId)
                    _statusBanner.value = "Personalized routine saved to your library."
                },
                onFailure = { error ->
                    _aiState.value = AiGenerationState.Error(
                        error.message ?: "Failed to personalize routine."
                    )
                }
            )
        }
    }

    fun resetAiState() {
        _aiState.value = AiGenerationState.Idle
    }

    fun toggleFavorite(routine: YogaRoutine) {
        if (routine.id.startsWith("curated_")) {
            viewModelScope.launch {
                val copyToSave = routine.copy(id = "", isFavorite = !routine.isFavorite)
                repository.saveRoutine(copyToSave)
                    .onSuccess { _statusBanner.value = "Added to your saved routines." }
                    .onFailure { _statusBanner.value = it.localizedMessage }
            }
            return
        }
        viewModelScope.launch {
            repository.toggleFavoriteRoutine(routine.id, !routine.isFavorite)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun deleteSavedRoutine(routineId: String) {
        if (routineId.startsWith("curated_") || routineId.isBlank()) return
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
                .onSuccess { _statusBanner.value = "Routine removed." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun updateBeginnerPreferences(
        displayName: String,
        experienceLevel: String,
        primaryGoal: String,
        preferredMinutes: Int,
    ) {
        viewModelScope.launch {
            repository.updateProfilePreferences(
                experienceLevel = experienceLevel,
                primaryGoal = primaryGoal,
                preferredMinutes = preferredMinutes,
                displayName = displayName,
            ).onSuccess {
                _statusBanner.value = "Beginner preferences updated."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to save preferences."
            }
        }
    }

    fun syncWatchH9ToCloud(
        deviceName: String,
        deviceAddress: String,
        heartRateBpm: Int,
        batteryPercent: Int,
        stepsToday: Int,
        source: String,
    ) {
        val recovery = when {
            heartRateBpm <= 68 -> "Deep Calm · Ideal for Restorative Flow"
            heartRateBpm <= 82 -> "Balanced · Ready for Gentle Beginner Yoga"
            else -> "Elevated · Focus on Breath & Slow Mobility"
        }
        viewModelScope.launch {
            repository.recordWatchH9Sync(
                WatchSyncRecord(
                    userId = currentUserId,
                    deviceName = deviceName.ifBlank { "Watch H9" },
                    deviceAddress = deviceAddress.ifBlank { "H9-WATCH" },
                    heartRateBpm = heartRateBpm.coerceIn(35, 220),
                    batteryPercent = batteryPercent.coerceIn(0, 100),
                    stepsToday = stepsToday.coerceIn(0, 150000),
                    recoveryStatus = recovery,
                    source = source,
                )
            ).onSuccess {
                _statusBanner.value = "Watch H9 synced ($heartRateBpm BPM) to cloud."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to sync Watch H9."
            }
        }
    }

    fun pushRoutineToWatchH9(routine: YogaRoutine) {
        val pushed = watchManager.pushYogaCueToWatch(
            routineTitle = routine.title,
            targetHrMin = routine.targetHrMin,
            targetHrMax = routine.targetHrMax,
        )
        _statusBanner.value = if (pushed) {
            "Pushed '${routine.title}' target zone to Laxasfit watch."
        } else {
            watchManager.state.value.lastSyncMessage
                ?: "Connect a Laxasfit watch via BLE to push live cues directly."
        }
    }

    fun pushSportModeToWatch(mode: SportMode) {
        val pushed = watchManager.pushSportModeToWatch(
            sportName = mode.name,
            watchCode = mode.watchCode,
            targetHrMin = mode.targetHrMin,
            targetHrMax = mode.targetHrMax,
        )
        if (pushed) {
            _statusBanner.value = "Synced '${mode.name}' (#${mode.watchCode}) to watch."
        }
    }

    fun completeSportWorkout(
        mode: SportMode,
        durationMinutes: Int,
        avgHr: Int?,
        peakHr: Int?,
        syncedFromWatch: Boolean,
        feeling: String,
        onSaved: () -> Unit,
    ) {
        val currentProfile = (dashboardState.value as? UiState.Success)?.data?.profile
        viewModelScope.launch {
            repository.logCompletedSession(
                session = YogaSessionLog(
                    userId = currentUserId,
                    routineId = mode.id,
                    routineTitle = mode.name,
                    focusArea = mode.category,
                    durationMinutes = durationMinutes.coerceAtLeast(1),
                    posesCompleted = 1,
                    avgHeartRate = avgHr,
                    peakHeartRate = peakHr,
                    syncedFromWatchH9 = syncedFromWatch,
                    feeling = feeling,
                ),
                currentProfile = currentProfile,
            ).onSuccess {
                _statusBanner.value = "${mode.name} logged to your cloud progress!"
                onSaved()
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Could not save workout."
            }
        }
    }

    fun completePracticeSession(
        routine: YogaRoutine,
        actualMinutes: Int,
        posesCompleted: Int,
        avgHr: Int?,
        peakHr: Int?,
        syncedFromWatch: Boolean,
        feeling: String,
        onSaved: () -> Unit,
    ) {
        val currentProfile = (dashboardState.value as? UiState.Success)?.data?.profile
        viewModelScope.launch {
            repository.logCompletedSession(
                session = YogaSessionLog(
                    userId = currentUserId,
                    routineId = routine.id.ifBlank { "personalized" },
                    routineTitle = routine.title,
                    focusArea = routine.focusArea,
                    durationMinutes = actualMinutes.coerceAtLeast(1),
                    posesCompleted = posesCompleted.coerceAtLeast(1),
                    avgHeartRate = avgHr,
                    peakHeartRate = peakHr,
                    syncedFromWatchH9 = syncedFromWatch,
                    feeling = feeling,
                ),
                currentProfile = currentProfile,
            ).onSuccess {
                _statusBanner.value = "Session logged! Great work on your practice."
                onSaved()
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Could not save session."
            }
        }
    }

    fun clearBanner() {
        _statusBanner.update { null }
    }

    private companion object {
        const val TAG = "LaxafitVM"
    }
}
