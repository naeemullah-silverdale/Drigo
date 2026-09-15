package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DrigoTheme
import com.example.util.NotificationPreferencesManager
import com.example.util.ThemeManager

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            ThemeManager.init(applicationContext)
        } catch (_: Exception) {}
        try {
            NotificationPreferencesManager.init(applicationContext)
        } catch (_: Exception) {}

        enableEdgeToEdge()

        setContent {
            val themeMode by ThemeManager.themeMode.collectAsState()
            DrigoTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}
