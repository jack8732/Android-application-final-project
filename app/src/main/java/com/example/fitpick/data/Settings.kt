package com.example.fitpick.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val DEFAULT_MODEL = "gemini-3.7-flash"

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private val keyApi = stringPreferencesKey("gemini_api_key")
    private val keyGoal = intPreferencesKey("weekly_goal")
    private val keyModel = stringPreferencesKey("gemini_model")

    val apiKey: Flow<String> = context.dataStore.data.map { it[keyApi] ?: "" }
    val weeklyGoal: Flow<Int> = context.dataStore.data.map { it[keyGoal] ?: 3 }
    val model: Flow<String> = context.dataStore.data.map { it[keyModel] ?: DEFAULT_MODEL }

    suspend fun save(apiKey: String, weeklyGoal: Int, model: String) {
        context.dataStore.edit {
            it[keyApi] = apiKey.trim()
            it[keyGoal] = weeklyGoal
            it[keyModel] = model.trim().ifBlank { DEFAULT_MODEL }
        }
    }
}
