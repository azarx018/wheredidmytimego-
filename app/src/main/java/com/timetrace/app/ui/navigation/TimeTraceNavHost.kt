package com.timetrace.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.ui.screens.appdetail.AppDetailScreen
import com.timetrace.app.ui.screens.apps.AppsScreen
import com.timetrace.app.ui.screens.dashboard.DashboardScreen
import com.timetrace.app.ui.screens.dashboard.DashboardViewModel
import com.timetrace.app.ui.screens.onboarding.OnboardingScreen
import com.timetrace.app.ui.screens.settings.SettingsScreen
import com.timetrace.app.ui.screens.statistics.StatisticsScreen
import com.timetrace.app.ui.screens.timeline.TimelineScreen
import com.timetrace.app.util.PermissionUtils

@Composable
fun TimeTraceNavHost(usageRepository: UsageRepository) {
    val context = LocalContext.current

    // Re-checked whenever this composable recomposes after returning from
    // Settings (see onboarding button below) - cheap, no polling involved.
    var hasUsageAccess by remember {
        mutableStateOf(usageRepository.usageAccessState() == UsageAccessState.GRANTED)
    }

    if (!hasUsageAccess) {
        OnboardingScreen(
            onGrantAccessClick = {
                PermissionUtils.openUsageAccessSettings(context)
                // Optimistically re-check; DashboardScreen re-verifies on its
                // own refresh() too, so a stale true/false here is harmless.
                hasUsageAccess = usageRepository.usageAccessState() == UsageAccessState.GRANTED
            }
        )
        return
    }

    val navController = rememberNavController()

    Scaffold(
        bottomBar = { TimeTraceBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(Destination.Dashboard.route) {
                val viewModel: DashboardViewModel = viewModel(
                    factory = SimpleViewModelFactory { DashboardViewModel(usageRepository) }
                )
                DashboardScreen(viewModel)
            }
            composable(Destination.Apps.route) {
                val viewModel: com.timetrace.app.ui.screens.apps.AppsViewModel = viewModel(
                    factory = SimpleViewModelFactory {
                        com.timetrace.app.ui.screens.apps.AppsViewModel(usageRepository)
                    }
                )
                AppsScreen(
                    viewModel = viewModel,
                    onAppClick = { packageName, date ->
                        navController.navigate(
                            Destination.AppDetail.createRoute(packageName, date.toEpochDay())
                        )
                    }
                )
            }
            composable(Destination.Timeline.route) {
                val viewModel: com.timetrace.app.ui.screens.timeline.TimelineViewModel = viewModel(
                    factory = SimpleViewModelFactory {
                        com.timetrace.app.ui.screens.timeline.TimelineViewModel(usageRepository)
                    }
                )
                TimelineScreen(
                    viewModel = viewModel,
                    onEntryClick = { packageName, date ->
                        navController.navigate(
                            Destination.AppDetail.createRoute(packageName, date.toEpochDay())
                        )
                    }
                )
            }
            composable(Destination.Statistics.route) { StatisticsScreen() }
            composable(Destination.Settings.route) { SettingsScreen() }
            composable(Destination.AppDetail.route) { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("packageName").orEmpty()
                val dateEpochDay = backStackEntry.arguments?.getString("dateEpochDay")
                    ?.toLongOrNull()
                    ?: java.time.LocalDate.now().toEpochDay()
                val viewModel: com.timetrace.app.ui.screens.appdetail.AppDetailViewModel = viewModel(
                    key = "$packageName-$dateEpochDay",
                    factory = SimpleViewModelFactory {
                        com.timetrace.app.ui.screens.appdetail.AppDetailViewModel(usageRepository, packageName)
                    }
                )
                AppDetailScreen(viewModel, java.time.LocalDate.ofEpochDay(dateEpochDay))
            }
        }
    }
}

@Composable
private fun TimeTraceBottomBar(navController: androidx.navigation.NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        Destination.bottomBarDestinations.forEach { destination ->
            val (icon, label) = when (destination) {
                Destination.Dashboard -> Icons.Default.Home to "Home"
                Destination.Apps -> Icons.Default.List to "Apps"
                Destination.Timeline -> Icons.Default.Schedule to "Timeline"
                Destination.Statistics -> Icons.Default.BarChart to "Stats"
                Destination.Settings -> Icons.Default.Settings to "Settings"
                else -> Icons.Default.Home to ""
            }
            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { androidx.compose.material3.Text(label) }
            )
        }
    }
}
