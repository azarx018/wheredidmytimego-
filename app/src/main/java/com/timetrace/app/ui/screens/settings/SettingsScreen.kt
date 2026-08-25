package com.timetrace.app.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timetrace.app.data.local.ThemeMode
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.util.PermissionUtils

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onManageCategories: () -> Unit,
    onGoals: () -> Unit,
    onCodingSession: () -> Unit,
    onCodingApps: () -> Unit,
    onReplay: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showClearDataConfirm by remember { mutableStateOf(false) }

    // The manifest declares POST_NOTIFICATIONS, but Android 13+ still requires
    // a runtime prompt the first time a notification is posted. Requesting it
    // when the user flips this switch on is the natural moment to ask.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored - DailySummaryWorker checks the actual permission before posting */ }

    LaunchedEffect(Unit) { viewModel.refreshUsageAccess() }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item { SectionHeader("General") }
        item {
            Text(
                "Theme",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.themeMode == mode,
                        onClick = { viewModel.onThemeModeSelected(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }

        item { SectionHeader("Usage", topPadding = 28.dp) }
        item {
            SettingsRow(
                title = "Usage Access",
                subtitle = if (uiState.usageAccessState == UsageAccessState.GRANTED) "Granted" else "Not granted",
                onClick = { PermissionUtils.openUsageAccessSettings(context) }
            )
        }

        item { SectionHeader("Notifications", topPadding = 28.dp) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Enable notifications", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Daily summary and goal reminders",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.onNotificationsToggled(enabled)
                    }
                )
            }
        }

        item { SectionHeader("Productivity", topPadding = 28.dp) }
        item { SettingsRow(title = "Goals", subtitle = "Daily targets by app or category", onClick = onGoals) }
        item { SettingsRow(title = "Coding Session", subtitle = "Track a focused coding block", onClick = onCodingSession) }
        item { SettingsRow(title = "Coding apps", subtitle = "Choose which apps count as coding", onClick = onCodingApps) }
        item { SettingsRow(title = "Replay My Day", subtitle = "Watch a day's activity play back", onClick = onReplay) }

        item { SectionHeader("Organization", topPadding = 28.dp) }
        item {
            SettingsRow(
                title = "Manage categories",
                subtitle = "Assign apps to categories",
                onClick = onManageCategories
            )
        }

        item { SectionHeader("Privacy", topPadding = 28.dp) }
        item {
            Text(
                "TimeTrace reads only app-usage metadata (which app, when, for how " +
                    "long) through Android's Usage Access permission. It never reads " +
                    "screen contents, messages, or keystrokes.\n\n" +
                    "Everything is stored locally on this device - there is no account, " +
                    "no cloud sync, and no analytics or advertising SDK.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item { SectionHeader("Data", topPadding = 28.dp) }
        item {
            SettingsRow(
                title = "Clear local data",
                subtitle = "Removes category assignments, goals, and coding sessions",
                onClick = { showClearDataConfirm = true }
            )
        }
    }

    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Clear local data?") },
            text = {
                Text(
                    "This removes your category assignments, goals, and coding session " +
                        "history from this device. Usage data itself is controlled by " +
                        "Android, not TimeTrace, and is unaffected."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearLocalData()
                    showClearDataConfirm = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, topPadding: Dp = 0.dp) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = topPadding, bottom = 4.dp)
    )
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
