package com.timetrace.app.ui.screens.statistics

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timetrace.app.domain.model.CategoryUsageSummary
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.ui.components.AppListItem
import com.timetrace.app.ui.components.WeeklyBarChart
import com.timetrace.app.util.formatDuration
import java.time.format.DateTimeFormatter

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            SegmentedButton(
                selected = uiState.period == StatsPeriod.DAILY,
                onClick = { viewModel.onPeriodChanged(StatsPeriod.DAILY) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Daily") }
            SegmentedButton(
                selected = uiState.period == StatsPeriod.WEEKLY,
                onClick = { viewModel.onPeriodChanged(StatsPeriod.WEEKLY) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Weekly") }
        }

        when {
            uiState.isLoading -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            uiState.error -> com.timetrace.app.ui.components.ErrorState(onRetry = { viewModel.refresh() })

            uiState.usageAccessState != UsageAccessState.GRANTED -> CenteredMessage(
                "Grant Usage Access from Settings to see statistics."
            )

            uiState.period == StatsPeriod.DAILY -> DailyStats(uiState)
            else -> WeeklyStats(uiState)
        }
    }
}

@Composable
private fun DailyStats(uiState: StatisticsUiState) {
    val daily = uiState.daily ?: return

    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            Text(
                "Total usage",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                daily.totalDurationMillis.formatDuration(),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBlock("Sessions", uiState.dailySessionCount.toString())
                StatBlock("Avg session", uiState.dailyAverageSessionMillis.formatDuration())
                StatBlock("Apps used", daily.appCount.toString())
            }
        }

        if (daily.topApps.isNotEmpty()) {
            item {
                Text(
                    "Top apps",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(daily.topApps, key = { it.packageName }) { app ->
                AppListItem(
                    packageName = app.packageName,
                    appName = app.appName,
                    durationMillis = app.totalDurationMillis,
                    fractionOfTotal = if (daily.totalDurationMillis > 0) {
                        app.totalDurationMillis.toFloat() / daily.totalDurationMillis.toFloat()
                    } else 0f,
                    onClick = {}
                )
            }
        }

        if (uiState.categories.isNotEmpty()) {
            item {
                Text(
                    "By category",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
                )
            }
            itemsIndexed(uiState.categories, key = { _, item -> item.categoryName }) { index, category ->
                com.timetrace.app.ui.components.StaggeredAppear(index = index) {
                    CategoryRow(category)
                }
            }
        }
    }
}

@Composable
private fun WeeklyStats(uiState: StatisticsUiState) {
    val weekly = uiState.weekly ?: return

    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            Text(
                "This week",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                weekly.totalDurationMillis.formatDuration(),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
        item {
            WeeklyBarChart(days = weekly.days, modifier = Modifier.padding(bottom = 24.dp))
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBlock("Daily average", weekly.averageDailyMillis.formatDuration())
                StatBlock("Most used", weekly.mostUsedApp?.appName ?: "\u2014")
                StatBlock(
                    "Busiest day",
                    weekly.highestUsageDay?.format(DateTimeFormatter.ofPattern("EEE")) ?: "\u2014"
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(category: CategoryUsageSummary) {
    val accentColor = runCatching { Color(android.graphics.Color.parseColor(category.colorHex)) }
        .getOrDefault(MaterialTheme.colorScheme.onSurfaceVariant)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Text(
                category.categoryName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        Text(
            category.totalDurationMillis.formatDuration(),
            style = MaterialTheme.typography.titleMedium,
            color = accentColor
        )
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
