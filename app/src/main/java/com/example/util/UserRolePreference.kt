package com.example.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.viewmodel.UserMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.userRoleDataStore: DataStore<Preferences> by preferencesDataStore(name = "drigo_user_role_preferences")

class UserRolePreference(private val context: Context) {

    companion object {
        private val USER_MODE_KEY = stringPreferencesKey("app_user_mode")
    }

    val userModeFlow: Flow<UserMode> = context.userRoleDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val modeName = preferences[USER_MODE_KEY] ?: UserMode.PASSENGER.name
            try {
                UserMode.valueOf(modeName)
            } catch (_: IllegalArgumentException) {
                UserMode.PASSENGER
            }
        }

    suspend fun setUserMode(mode: UserMode) {
        context.userRoleDataStore.edit { preferences ->
            preferences[USER_MODE_KEY] = mode.name
        }
    }
}
