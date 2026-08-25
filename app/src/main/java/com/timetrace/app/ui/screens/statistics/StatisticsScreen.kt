package com.timetrace.app.ui.screens.statistics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Placeholder for Phase 4 (brief section 8): daily/weekly stats with simple
 * Canvas-based charts (no external charting library, to keep the APK small).
 */
@Composable
fun StatisticsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Statistics — coming in Phase 4", style = MaterialTheme.typography.bodyLarge)
    }
}
