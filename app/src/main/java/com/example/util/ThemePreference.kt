package com.example.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "drigo_theme_preferences")

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

class ThemePreference(private val context: Context) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("app_theme_mode")
    }

    val themeModeFlow: Flow<ThemeMode> = context.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val modeName = preferences[THEME_KEY] ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(modeName)
            } catch (_: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_KEY] = mode.name
        }
    }
}
