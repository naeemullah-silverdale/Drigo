package com.example.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object ThemeManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var themePreference: ThemePreference? = null

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun init(context: Context) {
        if (themePreference == null) {
            val pref = ThemePreference(context.applicationContext)
            themePreference = pref
            scope.launch {
                pref.themeModeFlow.collect { mode ->
                    _themeMode.value = mode
                }
            }
        }
    }

    fun getThemeModeFlow(context: Context): Flow<ThemeMode> {
        init(context)
        return themePreference?.themeModeFlow ?: _themeMode
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        init(context)
        _themeMode.value = mode
        scope.launch {
            themePreference?.setThemeMode(mode)
        }
    }
}
