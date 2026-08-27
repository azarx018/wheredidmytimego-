package com.timetrace.app.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.timetrace.app.data.local.entity.AppCategoryEntity
import com.timetrace.app.ui.components.AppIcon
import com.timetrace.app.ui.components.StaggeredAppear

@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel) {
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
                "Assign categories",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Tap an app to change its category. Categories power the breakdown in Statistics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (uiState.apps.isEmpty()) {
            item {
                Text(
                    "No apps used today yet - come back once you have some usage to categorize.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        itemsIndexed(uiState.apps, key = { _, app -> app.packageName }) { index, app ->
            StaggeredAppear(index = index) {
                CategoryAssignRow(
                    appName = app.appName,
                    packageName = app.packageName,
                    currentCategory = uiState.categories.find { it.id == app.categoryId },
                    categories = uiState.categories,
                    onCategorySelected = { categoryId -> viewModel.onCategoryAssigned(app.packageName, categoryId) }
                )
            }
        }
    }
}

/** Daylio-style row: a soft tint of the assigned category's color behind the
 * whole card, rather than a plain list row - the category feels like a
 * property of the app, not an afterthought dropdown. */
@Composable
private fun CategoryAssignRow(
    appName: String,
    packageName: String,
    currentCategory: AppCategoryEntity?,
    categories: List<AppCategoryEntity>,
    onCategorySelected: (Long) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val accentColor = currentCategory?.colorHex?.let {
        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
    } ?: MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = packageName, size = 32.dp)
            Text(
                appName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        Box {
            TextButton(onClick = { menuExpanded = true }) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    currentCategory?.name ?: "Uncategorized",
                    color = accentColor,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            onCategorySelected(category.id)
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}
