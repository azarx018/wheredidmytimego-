package com.timetrace.app.ui.screens.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.ui.components.AppListItem
import com.timetrace.app.ui.components.DaySelector
import java.time.LocalDate

@Composable
fun AppsScreen(
    viewModel: AppsViewModel,
    onAppClick: (packageName: String, date: LocalDate) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        DaySelector(
            selectedDate = uiState.selectedDate,
            onDateSelected = viewModel::onDateSelected
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            SegmentedButton(
                selected = uiState.sortOrder == AppSortOrder.USAGE_TIME,
                onClick = { viewModel.onSortOrderChanged(AppSortOrder.USAGE_TIME) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Usage time") }
            SegmentedButton(
                selected = uiState.sortOrder == AppSortOrder.NAME,
                onClick = { viewModel.onSortOrderChanged(AppSortOrder.NAME) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Name") }
        }

        when {
            uiState.isLoading -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            uiState.usageAccessState != UsageAccessState.GRANTED -> CenteredMessage(
                "Grant Usage Access from Settings to see your apps here."
            )

            uiState.apps.isEmpty() -> CenteredMessage("No usage data for this day.")

            else -> LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(uiState.apps, key = { it.packageName }) { app ->
                    AppListItem(
                        packageName = app.packageName,
                        appName = app.appName,
                        durationMillis = app.totalDurationMillis,
                        fractionOfTotal = if (uiState.totalDurationMillis > 0) {
                            app.totalDurationMillis.toFloat() / uiState.totalDurationMillis.toFloat()
                        } else 0f,
                        onClick = { onAppClick(app.packageName, uiState.selectedDate) }
                    )
                }
            }
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
