package com.example.fitpick.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitpick.data.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val MAX_SWAPS = 2

sealed interface GenState {
    data object Idle : GenState
    data object Loading : GenState
    data class Error(val msg: String) : GenState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsStore(app)
    private val repo = FitRepository(AppDatabase.get(app).dao())
    private val gson = Gson()

    val routines = repo.routines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val logs = repo.logs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val apiKey = settings.apiKey.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val weeklyGoal = settings.weeklyGoal.stateIn(viewModelScope, SharingStarted.Eagerly, 3)
    val model = settings.model.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_MODEL)

    // create-screen choices
    var selected by mutableStateOf(setOf<Muscle>())
    var level by mutableStateOf("Beginner")
    var equipment by mutableStateOf("Gym")
    var goal by mutableStateOf("Muscle")
    var minutes by mutableIntStateOf(40)

    var state by mutableStateOf<GenState>(GenState.Idle)
    var current by mutableStateOf<GeneratedRoutine?>(null)
    var currentSaved by mutableStateOf(false)
    var swapping by mutableIntStateOf(-1)
    var swapsUsed by mutableIntStateOf(0)
    var message by mutableStateOf<String?>(null)

    fun toggle(m: Muscle) {
        selected = if (m in selected) selected - m else selected + m
    }

    fun generate(onDone: () -> Unit) {
        if (selected.isEmpty()) return
        state = GenState.Loading
        viewModelScope.launch {
            try {
                val req = RoutineRequest(Muscle.values().filter { it in selected }, level, equipment, goal, minutes)
                current = repo.generate(req, apiKey.value, model.value)
                currentSaved = false
                swapsUsed = 0
                state = GenState.Idle
                onDone()
            } catch (e: Exception) {
                state = GenState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun swap(index: Int) {
        val r = current ?: return
        if (swapping != -1) return
        if (swapsUsed >= MAX_SWAPS) {
            message = "You have used all $MAX_SWAPS swaps for this routine"
            return
        }
        swapping = index
        viewModelScope.launch {
            try {
                val item = repo.swap(r, index, apiKey.value, model.value)
                current = r.copy(exercises = r.exercises.toMutableList().also { it[index] = item })
                currentSaved = false
                swapsUsed += 1
            } catch (e: Exception) {
                message = e.message ?: "Swap failed"
            }
            swapping = -1
        }
    }

    fun save() {
        val r = current ?: return
        viewModelScope.launch {
            repo.saveRoutine(
                RoutineEntity(
                    title = r.title, muscles = r.muscles, level = r.level, minutes = r.minutes,
                    goal = r.goal, exercisesJson = gson.toJson(r.exercises)
                )
            )
            currentSaved = true
            message = "Routine saved"
        }
    }

    fun openSaved(e: RoutineEntity) {
        val type = object : TypeToken<List<ExerciseItem>>() {}.type
        val list: List<ExerciseItem> = gson.fromJson(e.exercisesJson, type)
        current = GeneratedRoutine(e.title, e.muscles, e.level, e.goal, e.minutes, list, "Saved routine")
        currentSaved = true
        swapsUsed = 0
    }

    fun delete(e: RoutineEntity) = viewModelScope.launch { repo.deleteRoutine(e) }

    fun finishWorkout(done: Int, total: Int, seconds: Int) {
        val r = current ?: return
        viewModelScope.launch {
            repo.saveLog(WorkoutLogEntity(title = r.title, muscles = r.muscles, completedExercises = done, totalExercises = total, durationSec = seconds))
            message = "Workout saved to progress"
        }
    }

    fun clearHistory() = viewModelScope.launch { repo.clearLogs() }

    fun saveSettings(key: String, goal: Int, model: String) = viewModelScope.launch {
        settings.save(key, goal, model)
        message = "Settings saved"
    }

    fun shareText(r: GeneratedRoutine): String = buildString {
        appendLine("${r.title} (${r.minutes} min, ${r.level})")
        r.exercises.forEachIndexed { i, e ->
            appendLine("${i + 1}. ${e.name} - ${e.sets} x ${e.reps}, rest ${e.restSec}s")
        }
        append("Made with FitPick")
    }
}
