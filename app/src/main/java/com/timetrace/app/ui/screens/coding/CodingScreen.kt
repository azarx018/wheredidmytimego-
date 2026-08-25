package com.timetrace.app.ui.screens.coding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timetrace.app.ui.components.AppIcon
import com.timetrace.app.util.formatDuration
import com.timetrace.app.util.formatElapsed

@Composable
fun CodingScreen(viewModel: CodingViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.onScreenEntered() }

    if (uiState.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) { CircularProgressIndicator() }
        return
    }

    if (uiState.error) {
        com.timetrace.app.ui.components.ErrorState(onRetry = { viewModel.onScreenEntered() })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "CODING SESSION",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )

        if (uiState.activeSession == null) {
            Spacer(Modifier.height(48.dp))
            Text(
                "No session running",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = viewModel::startSession) { Text("START CODING") }
            Spacer(Modifier.height(24.dp))
            Text(
                "TimeTrace will track which apps you use until you stop the session. " +
                    "Mark your coding apps first from Settings \u2192 Coding apps for an " +
                    "accurate breakdown.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                uiState.elapsedMillis.formatElapsed(),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(vertical = 24.dp)
            )
            Button(
                onClick = viewModel::stopSession,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("STOP SESSION") }

            val breakdown = uiState.breakdown
            if (breakdown != null && breakdown.apps.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text(
                            "Activity",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    val total = (breakdown.codingDurationMillis + breakdown.otherDurationMillis).coerceAtLeast(1)
                    items(breakdown.apps, key = { it.packageName }) { app ->
                        val pct = (app.totalDurationMillis * 100 / total)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(packageName = app.packageName, size = 24.dp)
                                Text(
                                    app.appName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                                if (app.packageName in breakdown.codingPackages) {
                                    Text(
                                        " \u2022 coding",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                            Text(
                                "${app.totalDurationMillis.formatDuration()} \u2022 $pct%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatBlock("Coding time", breakdown.codingDurationMillis.formatDuration())
                            StatBlock("Other apps", breakdown.otherDurationMillis.formatDuration())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
