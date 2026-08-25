package com.timetrace.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timetrace.app.R
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.ui.components.AppUsageRow
import com.timetrace.app.util.PermissionUtils
import com.timetrace.app.util.formatDuration
import androidx.compose.ui.platform.LocalContext

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Refresh on entry only - not polled. If the user just granted usage
    // access and comes back to this screen, this re-checks state.
    LaunchedEffect(Unit) { viewModel.refresh() }

    when {
        uiState.isLoading -> LoadingState()
        uiState.usageAccessState != UsageAccessState.GRANTED -> UsageAccessRequiredState(
            onGrantClick = { PermissionUtils.openUsageAccessSettings(context) }
        )
        uiState.today == null || uiState.today?.totalDurationMillis == 0L -> NoUsageState()
        else -> DashboardContent(uiState)
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun UsageAccessRequiredState(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.empty_usage_access_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.empty_usage_access_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Button(onClick = onGrantClick) {
            Text(stringResource(R.string.onboarding_grant_button))
        }
    }
}

@Composable
private fun NoUsageState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.empty_no_usage_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.empty_no_usage_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun DashboardContent(uiState: DashboardUiState) {
    val today = uiState.today ?: return
    val totalMillis = today.totalDurationMillis

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                text = "TODAY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Text(
                text = totalMillis.formatDuration(),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }
        item {
            val delta = totalMillis - uiState.yesterdayTotalMillis
            val comparisonText = when {
                delta == 0L -> "Same as yesterday"
                delta > 0 -> "${delta.formatDuration()} more than yesterday"
                else -> "${(-delta).formatDuration()} less than yesterday"
            }
            Text(
                text = comparisonText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        item {
            Text(
                text = "Top Apps",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(today.topApps, key = { it.packageName }) { app ->
            AppUsageRow(
                appName = app.appName,
                durationMillis = app.totalDurationMillis,
                fractionOfTotal = if (totalMillis > 0) {
                    app.totalDurationMillis.toFloat() / totalMillis.toFloat()
                } else 0f
            )
        }
        item {
            Text(
                text = "${today.appCount} apps used today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
