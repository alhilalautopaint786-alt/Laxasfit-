package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Beginner Yogi",
    val experienceLevel: String = "brand_new",
    val primaryGoal: String = "Back & Posture",
    val preferredMinutes: Int = 15,
    val streakDays: Int = 0,
    val totalMinutes: Int = 0,
    val totalSessions: Int = 0,
    val watchH9PairedName: String? = null,
    val watchH9MacAddress: String? = null,
    val lastRestingHr: Int? = null,
    val lastWatchBattery: Int? = null,
    val lastWatchSyncEpochMs: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "displayName" to displayName.take(80).ifBlank { "Beginner Yogi" },
            "experienceLevel" to experienceLevel,
            "primaryGoal" to primaryGoal.take(80).ifBlank { "Back & Posture" },
            "preferredMinutes" to preferredMinutes.coerceIn(5, 60),
            "streakDays" to streakDays.coerceAtLeast(0),
            "totalMinutes" to totalMinutes.coerceAtLeast(0),
            "totalSessions" to totalSessions.coerceAtLeast(0),
            "watchH9PairedName" to watchH9PairedName?.take(80),
            "watchH9MacAddress" to watchH9MacAddress?.take(40),
            "lastRestingHr" to lastRestingHr?.coerceIn(30, 240),
            "lastWatchBattery" to lastWatchBattery?.coerceIn(0, 100),
            "lastWatchSyncEpochMs" to lastWatchSyncEpochMs,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class YogaPose(
    val name: String = "",
    val sanskritName: String = "",
    val durationSeconds: Int = 45,
    val instruction: String = "",
    val modification: String = "",
    val breathCue: String = "",
)

@IgnoreExtraProperties
data class YogaRoutine(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subtitle: String = "",
    val focusArea: String = "Back & Posture",
    val level: String = "brand_new",
    val durationMinutes: Int = 15,
    val targetHrMin: Int = 72,
    val targetHrMax: Int = 102,
    val isAiGenerated: Boolean = false,
    val isFavorite: Boolean = false,
    val poses: List<YogaPose> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        val poseMaps = poses.take(20).map { pose ->
            mapOf(
                "name" to pose.name.take(80),
                "sanskritName" to pose.sanskritName.take(80),
                "durationSeconds" to pose.durationSeconds.coerceIn(15, 300),
                "instruction" to pose.instruction.take(280),
                "modification" to pose.modification.take(220),
                "breathCue" to pose.breathCue.take(140),
            )
        }
        return mapOf(
            "userId" to ownerUid,
            "title" to title.take(100).ifBlank { "Gentle Beginner Flow" },
            "subtitle" to subtitle.take(200).ifBlank { "Calm breath-led movement." },
            "focusArea" to focusArea.take(60).ifBlank { "Back & Posture" },
            "level" to level,
            "durationMinutes" to durationMinutes.coerceIn(1, 90),
            "targetHrMin" to targetHrMin.coerceIn(40, 180),
            "targetHrMax" to targetHrMax.coerceIn(targetHrMin.coerceIn(40, 180), 200),
            "isAiGenerated" to isAiGenerated,
            "isFavorite" to isFavorite,
            "poses" to poseMaps,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
    }
}

@IgnoreExtraProperties
data class YogaSessionLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val routineId: String = "",
    val routineTitle: String = "",
    val focusArea: String = "",
    val durationMinutes: Int = 10,
    val posesCompleted: Int = 1,
    val avgHeartRate: Int? = null,
    val peakHeartRate: Int? = null,
    val syncedFromWatchH9: Boolean = false,
    val feeling: String = "just_right",
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to ownerUid,
            "routineId" to routineId.take(128).ifBlank { "curated" },
            "routineTitle" to routineTitle.take(100).ifBlank { "Beginner Flow" },
            "focusArea" to focusArea.take(60).ifBlank { "Mindfulness" },
            "durationMinutes" to durationMinutes.coerceIn(1, 120),
            "posesCompleted" to posesCompleted.coerceIn(1, 50),
            "avgHeartRate" to avgHeartRate?.coerceIn(0, 240),
            "peakHeartRate" to peakHeartRate?.coerceIn(0, 240),
            "syncedFromWatchH9" to syncedFromWatchH9,
            "feeling" to feeling,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class WatchSyncRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceName: String = "Watch H9",
    val deviceAddress: String = "",
    val heartRateBpm: Int = 72,
    val batteryPercent: Int = 85,
    val stepsToday: Int = 0,
    val recoveryStatus: String = "Calm & Ready",
    val source: String = "ble_gatt",
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        return mapOf(
            "userId" to ownerUid,
            "deviceName" to deviceName.take(80).ifBlank { "Watch H9" },
            "deviceAddress" to deviceAddress.take(40).ifBlank { "H9-MANUAL" },
            "heartRateBpm" to heartRateBpm.coerceIn(30, 240),
            "batteryPercent" to batteryPercent.coerceIn(0, 100),
            "stepsToday" to stepsToday.coerceIn(0, 200000),
            "recoveryStatus" to recoveryStatus.take(60).ifBlank { "Calm & Ready" },
            "source" to source,
            "createdAt" to FieldValue.serverTimestamp(),
        )
    }
}
