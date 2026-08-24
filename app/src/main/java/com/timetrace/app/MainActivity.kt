package com.timetrace.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.timetrace.app.ui.navigation.TimeTraceNavHost
import com.timetrace.app.ui.theme.TimeTraceTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = TimeTraceApplication.from(this)

        setContent {
            TimeTraceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimeTraceNavHost(usageRepository = app.usageRepository)
                }
            }
        }
    }
}
