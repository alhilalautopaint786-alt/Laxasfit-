package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.WatchSyncRecord
import com.example.data.model.YogaPose
import com.example.data.model.YogaRoutine
import com.example.data.model.YogaSessionLog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LaxafitRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureProfileAndSaveRoutine_authenticatedOwner_succeedsAndEmits(): Unit = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = LaxafitRepository(firestore, auth)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile("Alice")
        }
        assertTrue(profileResult.isSuccess)
        assertEquals(uid, profileResult.getOrThrow().userId)

        val routine = YogaRoutine(
            userId = uid,
            title = "Blue Mist Beginner Flow",
            subtitle = "Gentle posture reset.",
            focusArea = "Back & Posture",
            level = "brand_new",
            durationMinutes = 12,
            targetHrMin = 70,
            targetHrMax = 98,
            isAiGenerated = true,
            isFavorite = false,
            poses = listOf(
                YogaPose(
                    name = "Cat-Cow",
                    sanskritName = "Marjaryasana",
                    durationSeconds = 60,
                    instruction = "Move gently with breath.",
                    modification = "Towel under knees.",
                    breathCue = "Inhale arch, exhale round",
                )
            ),
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveRoutine(routine) }
        assertTrue(saveResult.isSuccess)
        val routineId = saveResult.getOrThrow()

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeRoutines(uid).first { list -> list.any { it.id == routineId } }
        }
        assertTrue(observed.any { it.id == routineId && it.title == "Blue Mist Beginner Flow" })
    }

    @Test
    fun logSessionAndWatchH9Sync_authenticatedOwner_succeeds(): Unit = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = LaxafitRepository(firestore, auth)
        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile("Alice").getOrThrow()
        }

        val syncRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.recordWatchH9Sync(
                WatchSyncRecord(
                    userId = uid,
                    deviceName = "Watch H9",
                    deviceAddress = "AA:BB:CC:11:22:33",
                    heartRateBpm = 69,
                    batteryPercent = 88,
                    stepsToday = 2450,
                    recoveryStatus = "Calm & Ready",
                    source = "ble_gatt",
                )
            )
        }
        assertTrue(syncRes.isSuccess)

        val sessionRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.logCompletedSession(
                YogaSessionLog(
                    userId = uid,
                    routineId = "curated_spine_wakeup",
                    routineTitle = "Morning Spine Awakening",
                    focusArea = "Back & Posture",
                    durationMinutes = 10,
                    posesCompleted = 5,
                    avgHeartRate = 82,
                    peakHeartRate = 94,
                    syncedFromWatchH9 = true,
                    feeling = "just_right",
                ),
                currentProfile = profile,
            )
        }
        assertTrue(sessionRes.isSuccess)
    }

    @Test
    fun crossUserAccess_readingOtherUserRoutines_failsWithPermissionDenied(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = LaxafitRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.ensureUserProfile("Alice").getOrThrow() }

        signInTestUser(BOB_EMAIL)
        val bobRepo = LaxafitRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeRoutines(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull(caught)
        val fsEx = caught!!.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }

    @Test
    fun unauthenticatedUser_observingRoutines_failsWithPermissionDenied(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repo = LaxafitRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeRoutines(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull(caught)
        val fsEx = caught!!.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice.laxafit@test.com"
        const val BOB_EMAIL = "bob.laxafit@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
