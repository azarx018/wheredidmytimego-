package com.timetrace.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.timetrace.app.data.local.ThemeMode
import com.timetrace.app.ui.navigation.TimeTraceNavHost
import com.timetrace.app.ui.theme.TimeTraceTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = TimeTraceApplication.from(this)

        setContent {
            val themeMode by app.settingsDataStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> null // TimeTraceTheme falls back to isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            TimeTraceTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimeTraceNavHost(
                        usageRepository = app.usageRepository,
                        settingsDataStore = app.settingsDataStore
                    )
                }
            }
        }
    }
}
