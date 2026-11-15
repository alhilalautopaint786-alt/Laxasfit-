package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.model.WatchSyncRecord
import com.example.data.model.YogaRoutine
import com.example.data.model.YogaSessionLog
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LaxafitRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, docRef.path, userId)
                    throw error
                }
        )
    }

    fun observeRoutines(userId: String): Flow<List<YogaRoutine>> = flow {
        val colRef = db.collection("users").document(userId).collection("routines")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            YogaRoutine::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    fun observeSessions(userId: String): Flow<List<YogaSessionLog>> = flow {
        val colRef = db.collection("users").document(userId).collection("sessions")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            YogaSessionLog::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    fun observeWatchSyncs(userId: String): Flow<List<WatchSyncRecord>> = flow {
        val colRef = db.collection("users").document(userId).collection("watch_syncs")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            WatchSyncRecord::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(defaultDisplayName: String): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(id = uid, userId = uid, displayName = defaultDisplayName)
            } else {
                val initial = UserProfile(
                    userId = uid,
                    displayName = defaultDisplayName.take(80).ifBlank { "Beginner Yogi" },
                    experienceLevel = "brand_new",
                    primaryGoal = "Back & Posture",
                    preferredMinutes = 15,
                )
                docRef.set(initial.toCreateMap()).await()
                initial.copy(id = uid)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateProfilePreferences(
        experienceLevel: String,
        primaryGoal: String,
        preferredMinutes: Int,
        displayName: String,
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val updates = mapOf(
            "experienceLevel" to experienceLevel,
            "primaryGoal" to primaryGoal.take(80).ifBlank { "Back & Posture" },
            "preferredMinutes" to preferredMinutes.coerceIn(5, 60),
            "displayName" to displayName.take(80).ifBlank { "Beginner Yogi" },
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun saveRoutine(routine: YogaRoutine): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("routines").document()
        try {
            docRef.set(routine.toCreateMap(uid)).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleFavoriteRoutine(routineId: String, isFavorite: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(routineId.isNotBlank()) { "Routine ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("routines").document(routineId)
        try {
            docRef.update(
                mapOf(
                    "isFavorite" to isFavorite,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteRoutine(routineId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(routineId.isNotBlank()) { "Routine ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("routines").document(routineId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun logCompletedSession(
        session: YogaSessionLog,
        currentProfile: UserProfile?,
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)
        val sessionDoc = userDoc.collection("sessions").document()
        try {
            val batch = db.batch()
            batch.set(sessionDoc, session.toCreateMap(uid))

            val nextStreak = ((currentProfile?.streakDays ?: 0) + 1).coerceAtMost(10000)
            val nextTotalMinutes = ((currentProfile?.totalMinutes ?: 0) + session.durationMinutes)
                .coerceAtMost(1000000)
            val nextTotalSessions = ((currentProfile?.totalSessions ?: 0) + 1)
                .coerceAtMost(100000)

            batch.update(
                userDoc,
                mapOf(
                    "streakDays" to nextStreak,
                    "totalMinutes" to nextTotalMinutes,
                    "totalSessions" to nextTotalSessions,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            sessionDoc.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, sessionDoc.path)
            throw e
        }
    }

    suspend fun recordWatchH9Sync(record: WatchSyncRecord): Result<String> = runCatching {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)
        val syncDoc = userDoc.collection("watch_syncs").document()
        try {
            val batch = db.batch()
            batch.set(syncDoc, record.toCreateMap(uid))
            batch.update(
                userDoc,
                mapOf(
                    "watchH9PairedName" to record.deviceName.take(80),
                    "watchH9MacAddress" to record.deviceAddress.take(40),
                    "lastRestingHr" to record.heartRateBpm.coerceIn(30, 240),
                    "lastWatchBattery" to record.batteryPercent.coerceIn(0, 100),
                    "lastWatchSyncEpochMs" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            syncDoc.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, syncDoc.path)
            throw e
        }
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String,
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
