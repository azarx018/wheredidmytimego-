package com.timetrace.app.ui.navigation

sealed class Destination(val route: String) {
    data object Dashboard : Destination("dashboard")
    data object Apps : Destination("apps")
    data object Timeline : Destination("timeline")
    data object Statistics : Destination("statistics")
    data object Settings : Destination("settings")
    data object Categories : Destination("categories")
    data object Goals : Destination("goals")
    data object Coding : Destination("coding")
    data object CodingApps : Destination("coding_apps")
    data object Replay : Destination("replay")

    data object AppDetail : Destination("app_detail/{packageName}/{dateEpochDay}") {
        fun createRoute(packageName: String, dateEpochDay: Long) =
            "app_detail/$packageName/$dateEpochDay"
    }

    companion object {
        val bottomBarDestinations = listOf(Dashboard, Apps, Timeline, Statistics, Settings)
    }
}
