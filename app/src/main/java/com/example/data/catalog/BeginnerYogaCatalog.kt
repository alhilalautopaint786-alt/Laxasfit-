package com.example.data.catalog

import com.example.data.model.YogaPose
import com.example.data.model.YogaRoutine

object BeginnerYogaCatalog {

    val focusAreas = listOf(
        "Back & Posture",
        "Neck & Shoulders",
        "Morning Mobility",
        "Deep Sleep & Calm",
        "Hip Flexibility",
        "Core & Balance",
    )

    val experienceLevels = listOf(
        "brand_new" to "Brand New (Day 1)",
        "gentle_beginner" to "Gentle Beginner",
        "steady_beginner" to "Steady Beginner",
    )

    fun levelLabel(levelKey: String): String = when (levelKey) {
        "brand_new" -> "Brand New"
        "gentle_beginner" -> "Gentle Beginner"
        "steady_beginner" -> "Steady Beginner"
        else -> "Beginner"
    }

    val curatedRoutines: List<YogaRoutine> = listOf(
        YogaRoutine(
            id = "curated_spine_wakeup",
            title = "Morning Spine Awakening",
            subtitle = "Unhurried breath-led stretches to melt away overnight stiffness.",
            focusArea = "Back & Posture",
            level = "brand_new",
            durationMinutes = 10,
            targetHrMin = 70,
            targetHrMax = 96,
            isAiGenerated = false,
            isFavorite = true,
            poses = listOf(
                YogaPose(
                    name = "Easy Seated Breath",
                    sanskritName = "Sukhasana",
                    durationSeconds = 60,
                    instruction = "Sit tall with relaxed shoulders and let your hands rest gently on your knees.",
                    modification = "Sit on the edge of a folded pillow if your hips feel tight.",
                    breathCue = "Inhale 4 counts, exhale 6 counts",
                ),
                YogaPose(
                    name = "Cat-Cow Flow",
                    sanskritName = "Marjaryasana-Bitilasana",
                    durationSeconds = 90,
                    instruction = "On hands and knees, gently arch your back as you look forward, then round your spine toward the ceiling.",
                    modification = "Place a folded towel under your knees for extra cushion.",
                    breathCue = "Inhale chest forward, exhale navel in",
                ),
                YogaPose(
                    name = "Child's Pose",
                    sanskritName = "Balasana",
                    durationSeconds = 75,
                    instruction = "Sink your hips back toward your heels and reach your arms comfortably forward on the mat.",
                    modification = "Keep knees wide and rest your forehead on stacked fists.",
                    breathCue = "Breathe into the back of your ribs",
                ),
                YogaPose(
                    name = "Gentle Seated Twist",
                    sanskritName = "Bharadvajasana",
                    durationSeconds = 60,
                    instruction = "Sit upright and rotate gently to the right, resting your left hand on your right knee.",
                    modification = "Keep the twist subtle—turn from your ribs rather than pulling with your arms.",
                    breathCue = "Inhale grow taller, exhale soften into the twist",
                ),
                YogaPose(
                    name = "Mountain Pose Reset",
                    sanskritName = "Tadasana",
                    durationSeconds = 45,
                    instruction = "Stand hip-width apart, press evenly through both feet, and draw your shoulder blades down.",
                    modification = "Stand near a wall if you want extra balance reassurance.",
                    breathCue = "Three slow grounding breaths",
                ),
            ),
        ),
        YogaRoutine(
            id = "curated_desk_neck_relief",
            title = "Desk & Neck Tension Release",
            subtitle = "Targeted beginner openers for tight shoulders, upper back, and screen fatigue.",
            focusArea = "Neck & Shoulders",
            level = "brand_new",
            durationMinutes = 12,
            targetHrMin = 68,
            targetHrMax = 92,
            isAiGenerated = false,
            isFavorite = false,
            poses = listOf(
                YogaPose(
                    name = "Neck Side Lengthening",
                    sanskritName = "Kantha Sanchalana",
                    durationSeconds = 60,
                    instruction = "Tilt your right ear softly toward your right shoulder while keeping the left shoulder heavy.",
                    modification = "Keep hands relaxed in your lap instead of adding hand pressure.",
                    breathCue = "Exhale to let the shoulders drop",
                ),
                YogaPose(
                    name = "Eagle Arms Stretch",
                    sanskritName = "Garudasana Arms",
                    durationSeconds = 60,
                    instruction = "Cross one elbow over the other in front of your chest and gently lift elbows to feel a wide upper-back stretch.",
                    modification = "Give yourself a bear hug holding opposite shoulders if wrapping feels tight.",
                    breathCue = "Inhale expand between shoulder blades",
                ),
                YogaPose(
                    name = "Puppy Heart Opener",
                    sanskritName = "Uttana Shishosana",
                    durationSeconds = 75,
                    instruction = "From tabletop, walk your hands forward and let your chest melt toward the mat while hips stay over knees.",
                    modification = "Rest your chest or forehead on a firm cushion.",
                    breathCue = "Slow steady nasal breathing",
                ),
                YogaPose(
                    name = "Thread the Needle",
                    sanskritName = "Parsva Balasana",
                    durationSeconds = 90,
                    instruction = "Slide your right arm underneath your left arm, resting your right shoulder and cheek lightly on the mat.",
                    modification = "Support your head with a folded blanket for comfort.",
                    breathCue = "Exhale soften the upper back",
                ),
            ),
        ),
        YogaRoutine(
            id = "curated_hip_ease",
            title = "Gentle Hip & Hamstring Ease",
            subtitle = "Low-impact floor poses to unlock stiffness after long sitting or walking.",
            focusArea = "Hip Flexibility",
            level = "gentle_beginner",
            durationMinutes = 15,
            targetHrMin = 74,
            targetHrMax = 104,
            isAiGenerated = false,
            isFavorite = false,
            poses = listOf(
                YogaPose(
                    name = "Reclined Knee-to-Chest",
                    sanskritName = "Apanasana",
                    durationSeconds = 60,
                    instruction = "Lie on your back and hug both knees lightly toward your chest, rocking softly side to side.",
                    modification = "Hold behind your thighs rather than over your shins.",
                    breathCue = "Exhale draw knees softly closer",
                ),
                YogaPose(
                    name = "Supine Figure-Four",
                    sanskritName = "Supta Kapotasana",
                    durationSeconds = 90,
                    instruction = "Cross your right ankle over your left thigh and draw the left thigh gently toward you.",
                    modification = "Keep your bottom foot planted on the floor if lifting feels intense.",
                    breathCue = "Relax your jaw as you breathe out",
                ),
                YogaPose(
                    name = "Supported Low Lunge",
                    sanskritName = "Anjaneyasana",
                    durationSeconds = 75,
                    instruction = "Step one foot forward between your hands, keeping the back knee down and torso upright.",
                    modification = "Rest hands on your front thigh or use yoga blocks under your palms.",
                    breathCue = "Inhale lengthen spine, exhale sink hips slightly",
                ),
                YogaPose(
                    name = "Bound Angle Rest",
                    sanskritName = "Baddha Konasana",
                    durationSeconds = 75,
                    instruction = "Bring the soles of your feet together while seated and let your knees open naturally toward the sides.",
                    modification = "Slide pillows under each outer knee for effortless support.",
                    breathCue = "Smooth 5-second inhales and exhales",
                ),
            ),
        ),
        YogaRoutine(
            id = "curated_evening_blue_calm",
            title = "Evening Blue-Calm Wind Down",
            subtitle = "Restorative slow-tempo flow designed to lower heart rate before sleep.",
            focusArea = "Deep Sleep & Calm",
            level = "brand_new",
            durationMinutes = 15,
            targetHrMin = 60,
            targetHrMax = 84,
            isAiGenerated = false,
            isFavorite = true,
            poses = listOf(
                YogaPose(
                    name = "Box Breathing Sit",
                    sanskritName = "Sama Vritti",
                    durationSeconds = 60,
                    instruction = "Sit comfortably with eyes soft or closed, matching the length of each inhale and exhale.",
                    modification = "Lean your back gently against a wall or sofa.",
                    breathCue = "Inhale 4s · Hold 2s · Exhale 4s",
                ),
                YogaPose(
                    name = "Seated Forward Fold",
                    sanskritName = "Paschimottanasana",
                    durationSeconds = 75,
                    instruction = "Extend legs forward with a soft bend in the knees and hinge comfortably from your hips.",
                    modification = "Bend your knees generously so your belly rests toward your thighs.",
                    breathCue = "Exhale let the head hang heavy",
                ),
                YogaPose(
                    name = "Supine Spinal Twist",
                    sanskritName = "Supta Matsyendrasana",
                    durationSeconds = 90,
                    instruction = "Lie on your back, open arms wide, and let both bent knees drape peacefully to one side.",
                    modification = "Place a pillow between your knees to keep your lower back happy.",
                    breathCue = "Deep belly breaths into the side ribs",
                ),
                YogaPose(
                    name = "Legs-Up-the-Wall Rest",
                    sanskritName = "Viparita Karani",
                    durationSeconds = 120,
                    instruction = "Rest your legs softly upward against a wall or over a chair while your arms relax palms-up.",
                    modification = "Move your hips a few inches away from the wall to ease hamstring stretch.",
                    breathCue = "Effortless slow exhales",
                ),
            ),
        ),
    )
}
