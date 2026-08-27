package com.timetrace.app.ui.screens.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timetrace.app.R
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.ui.components.AppUsageRow
import com.timetrace.app.ui.components.StaggeredAppear
import com.timetrace.app.ui.components.UsageRingChart
import com.timetrace.app.util.PermissionUtils
import com.timetrace.app.util.formatDuration

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onCodingSessionClick: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Refresh on entry only - not polled. If the user just granted usage
    // access and comes back to this screen, this re-checks state.
    LaunchedEffect(Unit) { viewModel.refresh() }

    when {
        uiState.isLoading -> LoadingState()
        uiState.error -> com.timetrace.app.ui.components.ErrorState(onRetry = viewModel::refresh)
        uiState.usageAccessState != UsageAccessState.GRANTED -> UsageAccessRequiredState(
            onGrantClick = { PermissionUtils.openUsageAccessSettings(context) }
        )
        uiState.today == null || uiState.today?.totalDurationMillis == 0L -> NoUsageState()
        else -> DashboardContent(uiState, onCodingSessionClick)
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
private fun DashboardContent(uiState: DashboardUiState, onCodingSessionClick: () -> Unit) {
    val today = uiState.today ?: return
    val totalMillis = today.totalDurationMillis

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (uiState.activeCodingSession != null) {
            item { CodingSessionBanner(onClick = onCodingSessionClick) }
        }

        item {
            // Digital-Wellbeing-style hero: a category-split ring with the
            // day's total centered inside, rather than a bare number.
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                UsageRingChart(totalDurationMillis = totalMillis, categories = uiState.categories)
            }
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
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )
        }
        item {
            Text(
                text = "Top Apps",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        itemsIndexed(today.topApps) { index, app ->
            StaggeredAppear(index = index) {
                AppUsageRow(
                    appName = app.appName,
                    durationMillis = app.totalDurationMillis,
                    fractionOfTotal = if (totalMillis > 0) {
                        app.totalDurationMillis.toFloat() / totalMillis.toFloat()
                    } else 0f
                )
            }
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

/** A Toggl-style "still running" reminder so a coding session isn't forgotten off-screen. */
@Composable
private fun CodingSessionBanner(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha))
        )
        Text(
            "Coding session running \u2014 tap to view",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
