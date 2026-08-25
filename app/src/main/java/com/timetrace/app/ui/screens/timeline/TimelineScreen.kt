package com.timetrace.app.ui.screens.timeline

import androidx.compose.foundation.clickable
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
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.domain.model.UsageSession
import com.timetrace.app.ui.components.AppIcon
import com.timetrace.app.ui.components.DaySelector
import com.timetrace.app.util.formatClockTime
import com.timetrace.app.util.formatDuration
import java.time.LocalDate

@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    onEntryClick: (packageName: String, date: LocalDate) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        DaySelector(
            selectedDate = uiState.selectedDate,
            onDateSelected = viewModel::onDateSelected
        )

        when {
            uiState.isLoading -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            uiState.error -> com.timetrace.app.ui.components.ErrorState(onRetry = { viewModel.refresh() })

            uiState.usageAccessState != UsageAccessState.GRANTED -> CenteredMessage(
                "Grant Usage Access from Settings to see your timeline."
            )

            uiState.entries.isEmpty() -> CenteredMessage("No activity recorded for this day.")

            else -> LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
                items(uiState.entries, key = { "${it.packageName}-${it.startTimeMillis}" }) { session ->
                    TimelineRow(
                        session = session,
                        appName = uiState.appNames[session.packageName] ?: session.packageName,
                        onClick = { onEntryClick(session.packageName, uiState.selectedDate) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(session: UsageSession, appName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = session.startTimeMillis.formatClockTime(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp)
        )
        AppIcon(packageName = session.packageName, size = 32.dp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(appName, style = MaterialTheme.typography.titleMedium)
            Text(
                session.durationMillis.formatDuration(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp)
    )
}
