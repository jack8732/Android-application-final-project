package com.example.fitpick.data

import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.io.IOException
import retrofit2.HttpException

class FitRepository(private val dao: FitDao) {
    private val gson = Gson()

    val routines = dao.routines()
    val logs = dao.logs()

    suspend fun saveRoutine(r: RoutineEntity) = dao.insertRoutine(r)
    suspend fun deleteRoutine(r: RoutineEntity) = dao.deleteRoutine(r)
    suspend fun saveLog(l: WorkoutLogEntity) = dao.insertLog(l)
    suspend fun clearLogs() = dao.clearLogs()

    // ---------- Step 1: wger API (parallel coroutines, one per muscle) ----------
    suspend fun fetchCandidates(muscles: List<Muscle>): Map<Muscle, List<Candidate>> = coroutineScope {
        muscles.map { m -> m to async { fetchForMuscle(m) } }
            .map { (m, d) -> m to d.await() }
            .toMap()
    }

    private suspend fun fetchForMuscle(m: Muscle): List<Candidate> = coroutineScope {
        m.wgerIds.map { id ->
            async {
                try {
                    val res = Net.wger.exercises(muscles = id).results.orEmpty()
                    val filtered = res.filter { ex -> ex.muscles.orEmpty().any { it.id == id } }
                    (if (filtered.isEmpty()) res else filtered).mapNotNull { ex ->
                        val tr = ex.translations.orEmpty()
                        val name = (tr.firstOrNull { it.language == 2 } ?: tr.firstOrNull())?.name
                        if (name.isNullOrBlank()) null
                        else Candidate(name.trim(), ex.images.orEmpty().firstOrNull()?.image)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }.awaitAll().flatten().distinctBy { it.name.lowercase() }.take(12)
    }

    // ---------- Step 2: AI builds the routine ----------
    suspend fun generate(req: RoutineRequest, apiKey: String, model: String): GeneratedRoutine {
        val byMuscle = fetchCandidates(req.muscles)
        val count = (req.minutes / 8).coerceIn(3, 8)
        val all = byMuscle.values.flatten()
        var source = "Offline plan - add a Gemini API key in Settings to use AI"

        if (apiKey.isNotBlank()) {
            try {
                return askGemini(req, all, count, apiKey, model)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                source = "Offline plan - the AI request failed (${e.message ?: "unknown error"})"
            }
        }
        return offline(req, byMuscle, count, source)
    }

    private val fallbackModels = listOf("gemini-3.7-flash", "gemini-flash-latest", "gemini-3.5-flash-lite")

    private fun aiError(code: Int?): IllegalStateException = IllegalStateException(
        when (code) {
            404 -> "no Gemini model was found - type a current model name in Settings"
            400, 401, 403 -> "API key rejected (HTTP $code) - check the key in Settings"
            429 -> "rate limit reached - try again in a minute"
            500, 502, 503, 504 -> "Google's AI servers are busy (HTTP $code) - try again in a moment"
            null -> "no connection to the AI service - check your internet"
            else -> "request failed (HTTP $code)"
        }
    )

    private suspend fun callGemini(prompt: String, apiKey: String, model: String): String {
        val body = GeminiRequest(listOf(GContent(listOf(GPart(prompt)))))
        var lastCode: Int? = null
        for (m in (listOf(model.trim()) + fallbackModels).filter { it.isNotBlank() }.distinct()) {
            for (attempt in 0..1) {
                try {
                    val res = Net.gemini.generate(m, apiKey, body)
                    val text = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    require(!text.isNullOrBlank()) { "Empty AI response" }
                    return text.replace("```json", "").replace("```", "").trim()
                } catch (e: HttpException) {
                    lastCode = e.code()
                    if (lastCode == 400 || lastCode == 401 || lastCode == 403) throw aiError(lastCode)
                    if (lastCode == 404) break              // model not available: try the next model
                    if (attempt == 0) delay(2000)           // busy (429/5xx): wait, retry once
                } catch (e: IOException) {
                    lastCode = null                         // timeout / no internet
                    if (attempt == 0) delay(2000)
                }
            }
        }
        throw aiError(lastCode)
    }

    private suspend fun askGemini(
        req: RoutineRequest,
        candidates: List<Candidate>,
        count: Int,
        apiKey: String,
        model: String
    ): GeneratedRoutine {
        val prompt = buildString {
            appendLine("You are an expert personal trainer. Create a gym workout.")
            appendLine("Target muscles: ${req.muscles.joinToString { it.label }}")
            appendLine("Level: ${req.level}. Equipment: ${req.equipment}. Goal: ${req.goal}. Duration: ${req.minutes} minutes.")
            appendLine("Use exactly $count exercises, balanced across the target muscles.")
            appendLine("Only use exercises possible with the stated equipment.")
            if (candidates.isNotEmpty()) {
                appendLine("Prefer names from this list when suitable: ${candidates.take(40).joinToString { it.name }}")
            }
            appendLine("Reply with JSON only, in this shape:")
            appendLine("{\"title\":string,\"exercises\":[{\"name\":string,\"sets\":number,\"reps\":string,\"restSec\":number,\"tip\":string}]}")
            appendLine("tip = one short form cue.")
        }
        val ai = gson.fromJson(callGemini(prompt, apiKey, model), AiRoutine::class.java)
        val list = ai.exercises.orEmpty().filter { !it.name.isNullOrBlank() }
        require(list.isNotEmpty()) { "AI returned no exercises" }

        return GeneratedRoutine(
            title = ai.title?.takeIf { it.isNotBlank() } ?: "${req.muscles.joinToString(" + ") { it.label }} Workout",
            muscles = req.muscles.joinToString(", ") { it.label },
            level = req.level,
            goal = req.goal,
            minutes = req.minutes,
            exercises = list.map { e ->
                ExerciseItem(
                    name = e.name!!.trim(),
                    sets = e.sets ?: 3,
                    reps = e.reps ?: "10",
                    restSec = e.restSec ?: 60,
                    tip = e.tip.orEmpty(),
                    imageUrl = candidates.firstOrNull { it.name.equals(e.name.trim(), true) }?.imageUrl
                )
            },
            source = "Generated by Gemini AI"
        )
    }

    private fun offline(
        req: RoutineRequest,
        byMuscle: Map<Muscle, List<Candidate>>,
        count: Int,
        source: String
    ): GeneratedRoutine {
        val pools = req.muscles.map { m ->
            (byMuscle[m].orEmpty().map { it.name to it.imageUrl } + m.fallback.map { it to null })
                .distinctBy { it.first.lowercase() }
        }
        val picked = mutableListOf<Pair<String, String?>>()
        var i = 0
        while (picked.size < count && pools.any { it.size > i }) {
            pools.forEach { p ->
                if (picked.size < count && i < p.size) {
                    val e = p[i]
                    if (picked.none { it.first.equals(e.first, true) }) picked.add(e)
                }
            }
            i++
        }
        val sets = if (req.level == "Beginner") 3 else 4
        val reps = when (req.goal) { "Strength" -> "5-6"; "Fat loss" -> "15"; else -> "10-12" }
        val rest = when (req.goal) { "Strength" -> 120; "Fat loss" -> 30; else -> 60 }
        return GeneratedRoutine(
            title = "${req.muscles.joinToString(" + ") { it.label }} Workout",
            muscles = req.muscles.joinToString(", ") { it.label },
            level = req.level,
            goal = req.goal,
            minutes = req.minutes,
            exercises = picked.map { ExerciseItem(it.first, sets, reps, rest, "", it.second) },
            source = source
        )
    }

    // ---------- Swap one exercise with AI ----------
    suspend fun swap(r: GeneratedRoutine, index: Int, apiKey: String, model: String): ExerciseItem {
        require(apiKey.isNotBlank()) { "Add a Gemini API key in Settings to swap exercises." }
        val old = r.exercises[index]
        val others = r.exercises.filterIndexed { i, _ -> i != index }.joinToString { it.name }
        val prompt = buildString {
            appendLine("Suggest ONE replacement for the gym exercise \"${old.name}\" (muscles: ${r.muscles}, level: ${r.level}).")
            appendLine("It must be different from: $others, ${old.name}.")
            appendLine("Reply with JSON only: {\"name\":string,\"sets\":number,\"reps\":string,\"restSec\":number,\"tip\":string}")
        }
        val e = gson.fromJson(callGemini(prompt, apiKey, model), AiExercise::class.java)
        require(!e.name.isNullOrBlank()) { "AI returned no exercise" }
        return ExerciseItem(e.name.trim(), e.sets ?: old.sets, e.reps ?: old.reps, e.restSec ?: old.restSec, e.tip.orEmpty(), null)
    }
}
