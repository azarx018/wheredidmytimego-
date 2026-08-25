package com.timetrace.app.ui.screens.replay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
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
import kotlinx.coroutines.delay

@Composable
fun ReplayScreen(viewModel: ReplayViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.load() }

    // Drives the reveal animation while playing - stops itself as soon as the
    // last entry is shown or the user pauses. Screen-local only, never runs
    // in the background.
    LaunchedEffect(uiState.isPlaying, uiState.entries) {
        if (!uiState.isPlaying) return@LaunchedEffect
        while (true) {
            val hasMore = viewModel.revealNext()
            listState.animateScrollToItem((viewModel.uiState.value.visibleCount - 1).coerceAtLeast(0))
            if (!hasMore) {
                viewModel.setPlaying(false)
                break
            }
            delay(700L)
        }
    }

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

            uiState.error -> com.timetrace.app.ui.components.ErrorState(onRetry = { viewModel.load() })

            uiState.usageAccessState != UsageAccessState.GRANTED -> CenteredMessage(
                "Grant Usage Access from Settings to replay a day."
            )

            uiState.entries.isEmpty() -> CenteredMessage("No activity recorded for this day.")

            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(onClick = {
                        if (uiState.isPlaying) {
                            viewModel.setPlaying(false)
                        } else {
                            if (uiState.visibleCount >= uiState.entries.size) viewModel.reset()
                            viewModel.setPlaying(true)
                        }
                    }) {
                        Icon(
                            if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isPlaying) "Pause" else "Play"
                        )
                    }
                    FilledIconButton(onClick = { viewModel.reset() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart")
                    }
                    Text(
                        "${uiState.visibleCount} / ${uiState.entries.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyColumn(state = listState, contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(
                        uiState.entries.take(uiState.visibleCount),
                        key = { "${it.packageName}-${it.startTimeMillis}" }
                    ) { session ->
                        AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
                            ReplayRow(
                                session = session,
                                appName = uiState.appNames[session.packageName] ?: session.packageName
                            )
                        }
                    }
                    if (uiState.visibleCount == 0) {
                        item {
                            Text(
                                "Tap play to replay this day, application by application.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplayRow(session: UsageSession, appName: String) {
    // Deliberately shows only app name/icon/start-end/duration - never message
    // contents, URLs, screen contents, or typed text. See brief section 13.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            session.startTimeMillis.formatClockTime(),
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
