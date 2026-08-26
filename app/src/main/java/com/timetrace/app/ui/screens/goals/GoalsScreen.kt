package com.timetrace.app.ui.screens.goals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timetrace.app.data.local.entity.AppCategoryEntity
import com.timetrace.app.data.local.entity.GoalTargetType
import com.timetrace.app.domain.model.AppUsageSummary
import com.timetrace.app.domain.model.GoalProgress
import com.timetrace.app.util.formatDuration

@Composable
fun GoalsScreen(viewModel: GoalsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add goal")
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.error -> com.timetrace.app.ui.components.ErrorState(
                onRetry = { viewModel.refresh() },
                modifier = Modifier.padding(padding)
            )

            uiState.goals.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No goals yet. Tap + to set a daily target for an app or category.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.goals, key = { it.goal.id }) { progress ->
                    GoalRow(progress, onDelete = { viewModel.deleteGoal(progress.goal.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            availableApps = uiState.availableApps,
            availableCategories = uiState.availableCategories,
            onDismiss = { showAddDialog = false },
            onCreateAppGoal = { pkg, duration ->
                viewModel.addAppGoal(pkg, duration)
                showAddDialog = false
            },
            onCreateCategoryGoal = { catId, duration ->
                viewModel.addCategoryGoal(catId, duration)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun GoalRow(progress: GoalProgress, onDelete: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(progress.label, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove goal",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Neutral, non-judgmental phrasing per brief section 11 - "X of Y today",
        // never "you wasted" or similar.
        Text(
            "${progress.currentDurationMillis.formatDuration()} of ${progress.goal.targetDurationMillis.formatDuration()} today",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        val fraction = if (progress.goal.targetDurationMillis > 0) {
            progress.currentDurationMillis.toFloat() / progress.goal.targetDurationMillis.toFloat()
        } else 0f
        val animatedFraction by animateFloatAsState(
            targetValue = fraction.coerceIn(0f, 1f),
            animationSpec = tween(500),
            label = "goal_progress"
        )
        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun AddGoalDialog(
    availableApps: List<AppUsageSummary>,
    availableCategories: List<AppCategoryEntity>,
    onDismiss: () -> Unit,
    onCreateAppGoal: (String, Long) -> Unit,
    onCreateCategoryGoal: (Long, Long) -> Unit
) {
    var targetType by remember { mutableStateOf(GoalTargetType.APP) }
    var selectedApp by remember { mutableStateOf(availableApps.firstOrNull()) }
    var selectedCategory by remember { mutableStateOf(availableCategories.firstOrNull()) }
    var durationMinutes by remember { mutableStateOf(60f) }
    var appMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New goal") },
        text = {
            Column {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    SegmentedButton(
                        selected = targetType == GoalTargetType.APP,
                        onClick = { targetType = GoalTargetType.APP },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("App") }
                    SegmentedButton(
                        selected = targetType == GoalTargetType.CATEGORY,
                        onClick = { targetType = GoalTargetType.CATEGORY },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Category") }
                }

                if (targetType == GoalTargetType.APP) {
                    Box {
                        TextButton(onClick = { appMenuExpanded = true }) {
                            Text(selectedApp?.appName ?: "Choose app")
                        }
                        DropdownMenu(expanded = appMenuExpanded, onDismissRequest = { appMenuExpanded = false }) {
                            if (availableApps.isEmpty()) {
                                DropdownMenuItem(text = { Text("No apps used today yet") }, onClick = {})
                            }
                            availableApps.forEach { app ->
                                DropdownMenuItem(
                                    text = { Text(app.appName) },
                                    onClick = { selectedApp = app; appMenuExpanded = false }
                                )
                            }
                        }
                    }
                } else {
                    Box {
                        TextButton(onClick = { categoryMenuExpanded = true }) {
                            Text(selectedCategory?.name ?: "Choose category")
                        }
                        DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                            availableCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = { selectedCategory = category; categoryMenuExpanded = false }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                val minutes = durationMinutes.toInt()
                Text(
                    "Target: ${minutes / 60}h ${minutes % 60}m",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    valueRange = 15f..360f,
                    steps = 22
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val millis = durationMinutes.toLong() * 60_000L
                if (targetType == GoalTargetType.APP) {
                    selectedApp?.let { onCreateAppGoal(it.packageName, millis) }
                } else {
                    selectedCategory?.let { onCreateCategoryGoal(it.id, millis) }
                }
            }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
