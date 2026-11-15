package com.example.data.catalog

data class SportMode(
    val id: String,
    val name: String,
    val category: String,
    val metEstimate: Double,
    val targetHrMin: Int,
    val targetHrMax: Int,
    val beginnerTip: String,
    val watchCode: Int,
)

object SportModesCatalog {

    val categories: List<String> = listOf(
        "All",
        "Yoga & Mindful",
        "Walk & Run",
        "Cycling",
        "Gym & Strength",
        "Water & Swim",
        "Racket & Ball",
        "Outdoor",
        "Dance & Cardio",
    )

    val allModes: List<SportMode> = buildList {
        var code = 101
        fun addCategory(
            category: String,
            met: Double,
            hrMin: Int,
            hrMax: Int,
            tip: String,
            names: List<String>,
        ) {
            names.forEachIndexed { idx, name ->
                val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                add(
                    SportMode(
                        id = "sport_$slug",
                        name = name,
                        category = category,
                        metEstimate = met + (idx % 3) * 0.4,
                        targetHrMin = hrMin,
                        targetHrMax = hrMax,
                        beginnerTip = tip,
                        watchCode = code++,
                    )
                )
            }
        }

        // 14 modes
        addCategory(
            category = "Yoga & Mindful",
            met = 2.6,
            hrMin = 65,
            hrMax = 98,
            tip = "Move slowly with nasal breathing and prioritize comfort over depth.",
            names = listOf(
                "Hatha Beginner Yoga",
                "Yin Restorative Yoga",
                "Gentle Vinyasa Flow",
                "Chair Yoga",
                "Morning Spine Mobility",
                "Guided Breathwork",
                "Mat Pilates",
                "Tai Chi Flow",
                "Mindful Meditation",
                "Full-Body Stretching",
                "Desk Posture Reset",
                "Prenatal Gentle Yoga",
                "Bedtime Wind-Down",
                "Balance & Ankle Flow",
            ),
        )

        // 14 modes
        addCategory(
            category = "Walk & Run",
            met = 4.2,
            hrMin = 88,
            hrMax = 132,
            tip = "Keep a conversational pace where you can speak full sentences easily.",
            names = listOf(
                "Outdoor Walk",
                "Brisk Power Walk",
                "Indoor Treadmill Walk",
                "Trail Walk",
                "Easy Beginner Jog",
                "Walk-Run Intervals",
                "Nordic Pole Walking",
                "Stair Climbing",
                "Recovery Stroll",
                "5K Beginner Pace",
                "Track Easy Run",
                "Hill Incline Walk",
                "Morning Dog Walk",
                "Tempo Stride",
            ),
        )

        // 12 modes
        addCategory(
            category = "Cycling",
            met = 5.4,
            hrMin = 94,
            hrMax = 138,
            tip = "Choose a light gear with smooth cadence (70–85 RPM) to protect knees.",
            names = listOf(
                "Outdoor Road Cycling",
                "Stationary Bike",
                "Beginner Spin Class",
                "Gravel Bike Ride",
                "E-Bike Commute",
                "Recumbent Bike",
                "Park Path Cruise",
                "Roller Skating",
                "Inline Skating",
                "Skateboarding",
                "Kick Scooter Cruise",
                "Mountain Trail Easy",
            ),
        )

        // 18 modes
        addCategory(
            category = "Gym & Strength",
            met = 4.6,
            hrMin = 90,
            hrMax = 135,
            tip = "Control every lowering phase and rest 45–60s between beginner sets.",
            names = listOf(
                "Bodyweight Circuit",
                "Light Dumbbell Tone",
                "Resistance Band Flow",
                "Core Conditioning",
                "Kettlebell Basics",
                "Functional Mobility",
                "Foam Roller Recovery",
                "Beginner Calisthenics",
                "Suspension Strap Workout",
                "Barbell Technique",
                "Glute & Hip Strength",
                "Upper Body Sculpt",
                "Lower Body Stability",
                "Push-Up Progression",
                "Wall Sit & Plank Holds",
                "Cross-Training Lite",
                "Joint Rehab Routine",
                "Gym Warm-Up & Cooldown",
            ),
        )

        // 12 modes
        addCategory(
            category = "Water & Swim",
            met = 5.6,
            hrMin = 92,
            hrMax = 136,
            tip = "Exhale steadily underwater and rest at the wall whenever needed.",
            names = listOf(
                "Pool Lap Swim",
                "Aqua Aerobics",
                "Water Walking",
                "Calm Kayaking",
                "Stand-Up Paddleboard",
                "Indoor Rowing Machine",
                "Lake Canoeing",
                "Snorkeling",
                "Beginner Surfing",
                "Open Water Easy Swim",
                "Dragon Boat Paddling",
                "Pool Hydrotherapy",
            ),
        )

        // 16 modes
        addCategory(
            category = "Racket & Ball",
            met = 5.2,
            hrMin = 96,
            hrMax = 144,
            tip = "Warm up ankles and shoulders for 3 minutes before quick court sprints.",
            names = listOf(
                "Pickleball",
                "Badminton",
                "Table Tennis",
                "Tennis Singles",
                "Tennis Doubles",
                "Squash",
                "Padel",
                "Basketball Shootaround",
                "Half-Court Basketball",
                "Soccer / Football",
                "Indoor Futsal",
                "Volleyball",
                "Beach Volleyball",
                "Softball",
                "Cricket Practice",
                "Walking Golf",
            ),
        )

        // 14 modes
        addCategory(
            category = "Outdoor",
            met = 5.0,
            hrMin = 90,
            hrMax = 136,
            tip = "Pace uphill climbs steadily and check Watch H9 heart rate every 10 min.",
            names = listOf(
                "Day Hiking",
                "Nature Trekking",
                "Indoor Rock Climbing",
                "Beginner Bouldering",
                "Horseback Riding",
                "Archery Focus",
                "Park Orienteering",
                "Active Gardening",
                "Snowshoeing",
                "Cross-Country Skiing",
                "Downhill Skiing",
                "Ice Skating",
                "Frisbee / Disc Golf",
                "Outdoor Bootcamp",
            ),
        )

        // 14 modes
        addCategory(
            category = "Dance & Cardio",
            met = 5.8,
            hrMin = 98,
            hrMax = 145,
            tip = "Land softly on the balls of your feet and stay in your comfortable HR zone.",
            names = listOf(
                "Low-Impact HIIT",
                "Dance Cardio",
                "Zumba Fitness",
                "Jump Rope Intervals",
                "Step Aerobics",
                "Shadow Boxing",
                "Cardio Kickboxing",
                "Elliptical Trainer",
                "Ballroom Dance",
                "Latin Salsa Dance",
                "Weighted Hula Hoop",
                "Rebounder Trampoline",
                "Indoor Marching Cardio",
                "Hip-Hop Groove",
            ),
        )
    }

    fun findById(id: String): SportMode? = allModes.firstOrNull { it.id == id }
}
