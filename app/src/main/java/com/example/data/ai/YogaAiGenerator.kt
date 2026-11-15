package com.example.data.ai

import android.util.Log
import com.example.data.model.YogaPose
import com.example.data.model.YogaRoutine
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class YogaAiGenerator {

    private val routineSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Calm, inviting title for the beginner yoga routine"),
            "subtitle" to Schema.string(description = "One supportive sentence summarizing the benefit"),
            "targetHrMin" to Schema.integer(description = "Target Watch H9 minimum heart rate BPM during flow"),
            "targetHrMax" to Schema.integer(description = "Target Watch H9 maximum heart rate BPM during flow"),
            "poses" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Clear English pose name"),
                        "sanskritName" to Schema.string(description = "Traditional Sanskrit pose name"),
                        "durationSeconds" to Schema.integer(description = "Hold duration in seconds (30 to 120)"),
                        "instruction" to Schema.string(description = "Beginner-safe step-by-step cue"),
                        "modification" to Schema.string(description = "Easier prop or knee/back-friendly modification"),
                        "breathCue" to Schema.string(description = "Short inhale/exhale rhythm cue"),
                    )
                ),
                description = "4 to 8 beginner-safe poses ordered from warm-up to relaxation",
            ),
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = routineSchema
                temperature = 0.7f
            }
        )
    }

    suspend fun generatePersonalizedRoutine(
        userId: String,
        focusArea: String,
        experienceLevel: String,
        durationMinutes: Int,
        userNote: String,
        watchH9HeartRate: Int?,
        watchH9RecoveryStatus: String?,
    ): Result<YogaRoutine> {
        val watchContext = if (watchH9HeartRate != null) {
            "Watch H9 latest resting/current heart rate is $watchH9HeartRate BPM" +
                (if (!watchH9RecoveryStatus.isNullOrBlank()) " (recovery status: $watchH9RecoveryStatus)" else "") +
                ". Calibrate pose intensity and targetHrMin/targetHrMax safely for this beginner's heart rate."
        } else {
            "No live Watch H9 heart rate provided; assume a gentle beginner resting heart rate around 72 BPM."
        }

        val poseCount = when {
            durationMinutes <= 10 -> 4
            durationMinutes <= 15 -> 5
            durationMinutes <= 20 -> 6
            else -> 7
        }

        val prompt = """
            Design a safe, welcoming, beginner-friendly yoga routine for the Laxafit app.
            - Beginner tier: $experienceLevel
            - Focus area: $focusArea
            - Total duration target: $durationMinutes minutes (include $poseCount poses)
            - Personal note from user: ${userNote.ifBlank { "No special pain notes, keep transitions slow and accessible." }}
            - Biometric context: $watchContext

            Keep language encouraging, clear, and accessible for someone new to yoga.
            Always include a gentle prop/knee/hamstring modification for each pose.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonText = response.text?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Gemini returned an empty routine."))

            val json = JSONObject(jsonText)
            val title = json.optString("title", "$focusArea Gentle Flow").take(100)
            val subtitle = json.optString("subtitle", "Personalized beginner sequence.").take(200)
            val hrMin = json.optInt("targetHrMin", (watchH9HeartRate ?: 70).coerceIn(55, 110))
                .coerceIn(45, 150)
            val hrMax = json.optInt("targetHrMax", hrMin + 24)
                .coerceIn(hrMin + 8, 175)

            val posesJson = json.getJSONArray("poses")
            val poses = buildList {
                for (i in 0 until posesJson.length().coerceAtMost(12)) {
                    val item = posesJson.getJSONObject(i)
                    add(
                        YogaPose(
                            name = item.optString("name", "Mindful Pose").take(80),
                            sanskritName = item.optString("sanskritName", "Asana").take(80),
                            durationSeconds = item.optInt("durationSeconds", 60).coerceIn(30, 180),
                            instruction = item.optString("instruction", "Move slowly with your breath.").take(280),
                            modification = item.optString("modification", "Use a folded cushion for comfort.").take(220),
                            breathCue = item.optString("breathCue", "Slow steady inhales and exhales").take(140),
                        )
                    )
                }
            }

            if (poses.isEmpty()) {
                return Result.failure(IllegalStateException("Generated routine contained no poses."))
            }

            Result.success(
                YogaRoutine(
                    userId = userId,
                    title = title,
                    subtitle = subtitle,
                    focusArea = focusArea,
                    level = experienceLevel,
                    durationMinutes = durationMinutes,
                    targetHrMin = hrMin,
                    targetHrMax = hrMax,
                    isAiGenerated = true,
                    isFavorite = false,
                    poses = poses,
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Routine generation timed out", e)
            Result.failure(Exception("Routine generation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI routine generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to personalize yoga routine."))
        }
    }

    private companion object {
        const val TAG = "YogaAiGenerator"
    }
}
