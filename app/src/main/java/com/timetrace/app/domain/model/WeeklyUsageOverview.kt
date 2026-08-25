package com.timetrace.app.domain.model

import java.time.LocalDate

/** Total usage for a single day within a weekly view. */
data class DayTotal(val date: LocalDate, val totalDurationMillis: Long)

/** Aggregated stats across a 7-day window (brief section 8: Weekly). */
data class WeeklyUsageOverview(
    val days: List<DayTotal>,
    val totalDurationMillis: Long,
    val averageDailyMillis: Long,
    val mostUsedApp: AppUsageSummary?,
    val highestUsageDay: LocalDate?
)
