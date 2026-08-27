package com.timetrace.app.domain.model

/** A single contiguous block of foreground usage for one app. */
data class UsageSession(
    val packageName: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long
) {
    val durationMillis: Long get() = (endTimeMillis - startTimeMillis).coerceAtLeast(0)
}

/** Aggregated usage for one app over a given day. */
data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val totalDurationMillis: Long,
    val sessionCount: Int,
    val lastUsedMillis: Long,
    val categoryId: Long? = null
)

/** Total usage for a whole day, plus the top contributing apps. */
data class DailyUsageOverview(
    val dateEpochDay: Long,
    val totalDurationMillis: Long,
    val appCount: Int,
    val topApps: List<AppUsageSummary>,
    val mostUsedApp: AppUsageSummary?
)

enum class UsageAccessState {
    GRANTED,
    NOT_GRANTED
}
