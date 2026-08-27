package com.timetrace.app.ui.screens.appdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timetrace.app.domain.model.UsageSession
import com.timetrace.app.ui.components.AppIcon
import com.timetrace.app.ui.components.StaggeredAppear
import com.timetrace.app.util.formatClockTime
import com.timetrace.app.util.formatDuration
import java.time.LocalDate

@Composable
fun AppDetailScreen(viewModel: AppDetailViewModel, date: LocalDate) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(date) { viewModel.load(date) }

    if (uiState.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) { CircularProgressIndicator() }
        return
    }

    if (uiState.error) {
        com.timetrace.app.ui.components.ErrorState(onRetry = { viewModel.load(date) })
        return
    }

    val stats = uiState.stats ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(packageName = viewModel.packageName, size = 48.dp)
                Spacer(Modifier.width(14.dp))
                Text(uiState.appName, style = MaterialTheme.typography.headlineMedium)
            }
        }
        item {
            Text(
                "Today",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                stats.totalDurationMillis.formatDuration(),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }

        if (stats.sessions.isEmpty()) {
            item {
                Text(
                    "No sessions recorded for this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@LazyColumn
        }

        item {
            Text(
                "Sessions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        itemsIndexed(stats.sessions, key = { _, session -> session.startTimeMillis }) { index, session ->
            StaggeredAppear(index = index) { SessionRow(session) }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp)) }

        item {
            Text(
                "Statistics",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn("Average session", stats.averageSessionMillis.formatDuration())
                StatColumn("Longest session", stats.longestSessionMillis.formatDuration())
                StatColumn("Sessions today", stats.sessions.size.toString())
            }
        }
    }
}

@Composable
private fun SessionRow(session: UsageSession) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "${session.startTimeMillis.formatClockTime()} \u2192 ${session.endTimeMillis.formatClockTime()}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            session.durationMillis.formatDuration(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
