package com.d1ff.moodtrack.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val THEME = stringPreferencesKey("theme") // SYSTEM, LIGHT, DARK
        val LANGUAGE = stringPreferencesKey("language") // SYSTEM, RU, EN
    }

    val reminderEnabled: Flow<Boolean> = context.dataStore.data.map { it[REMINDER_ENABLED] ?: false }
    val reminderHour: Flow<Int> = context.dataStore.data.map { it[REMINDER_HOUR] ?: 20 }
    val reminderMinute: Flow<Int> = context.dataStore.data.map { it[REMINDER_MINUTE] ?: 0 }
    val theme: Flow<String> = context.dataStore.data.map { it[THEME] ?: "SYSTEM" }
    val language: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "SYSTEM" }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[REMINDER_HOUR] = hour
            it[REMINDER_MINUTE] = minute
        }
    }

    suspend fun setTheme(themeStr: String) {
        context.dataStore.edit { it[THEME] = themeStr }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE] = lang }
    }
}
