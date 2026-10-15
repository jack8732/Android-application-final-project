package com.example.fitpick.data

enum class Muscle(
    val label: String,
    val emoji: String,
    val wgerIds: List<Int>,       // muscle ids used by the wger API
    val fallback: List<String>    // used when the API is unreachable
) {
    CHEST("Chest", "💪", listOf(4), listOf("Push-up", "Dumbbell Bench Press", "Incline Dumbbell Press")),
    BACK("Back", "🏋️", listOf(12, 9), listOf("Bent-over Row", "Lat Pulldown", "Superman Hold")),
    SHOULDERS("Shoulders", "🙆", listOf(2), listOf("Overhead Press", "Lateral Raise", "Pike Push-up")),
    BICEPS("Biceps", "💪", listOf(1, 13), listOf("Dumbbell Curl", "Hammer Curl", "Chin-up")),
    TRICEPS("Triceps", "🦾", listOf(5), listOf("Triceps Dip", "Overhead Triceps Extension", "Close-grip Push-up")),
    ABS("Abs", "🔥", listOf(6, 14), listOf("Plank", "Crunch", "Leg Raise")),
    LEGS("Legs", "🦵", listOf(10, 11), listOf("Squat", "Lunge", "Romanian Deadlift")),
    GLUTES("Glutes", "🏃", listOf(8), listOf("Hip Thrust", "Glute Bridge", "Step-up")),
    CALVES("Calves", "🦶", listOf(7, 15), listOf("Standing Calf Raise", "Seated Calf Raise", "Jump Rope"))
}

val LEVELS = listOf("Beginner", "Intermediate", "Advanced")
val GOALS = listOf("Muscle", "Strength", "Fat loss")
val EQUIPMENT = listOf("Gym", "Dumbbells", "Bodyweight")
val DURATIONS = listOf(20, 40, 60)

data class RoutineRequest(
    val muscles: List<Muscle>,
    val level: String,
    val equipment: String,
    val goal: String,
    val minutes: Int
)

data class ExerciseItem(
    val name: String,
    val sets: Int,
    val reps: String,
    val restSec: Int,
    val tip: String = "",
    val imageUrl: String? = null
)

data class GeneratedRoutine(
    val title: String,
    val muscles: String,
    val level: String,
    val goal: String,
    val minutes: Int,
    val exercises: List<ExerciseItem>,
    val source: String
)

data class Candidate(val name: String, val imageUrl: String?)

// ---- JSON shapes ----
data class AiExercise(
    val name: String?,
    val sets: Int?,
    val reps: String?,
    val restSec: Int?,
    val tip: String?
)

data class AiRoutine(val title: String?, val exercises: List<AiExercise>?)
