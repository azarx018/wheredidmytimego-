package com.timetrace.app.ui.screens.coding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.timetrace.app.ui.components.AppIcon
import com.timetrace.app.ui.components.StaggeredAppear

@Composable
fun CodingAppsScreen(viewModel: CodingAppsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    if (uiState.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) { CircularProgressIndicator() }
        return
    }

    if (uiState.error) {
        com.timetrace.app.ui.components.ErrorState(onRetry = { viewModel.refresh() })
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                "Coding apps",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Mark the apps that count as coding for the Coding Session breakdown. " +
                    "Nothing here changes how your overall usage is tracked.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        itemsIndexed(uiState.apps, key = { _, app -> app.packageName }) { index, app ->
            StaggeredAppear(index = index) {
                val isCoding = app.packageName in uiState.codingPackages
                // Toggl-style: the row itself tints to the "on" color when
                // active, not just the switch, so scanning a long list for
                // what's marked as coding is a glance, not a squint.
                val backgroundColor by animateColorAsState(
                    targetValue = if (isCoding) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                    animationSpec = tween(250),
                    label = "coding_app_row_bg"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(backgroundColor)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(packageName = app.packageName, size = 32.dp)
                        Text(
                            app.appName,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                    Switch(
                        checked = isCoding,
                        onCheckedChange = { viewModel.toggle(app.packageName, it) }
                    )
                }
            }
        }
    }
}
