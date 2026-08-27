package com.timetrace.app.domain.model

/**
 * Per-app usage during a coding session window, split into apps the user has
 * marked as coding/productivity apps vs. everything else (brief section 10 -
 * "Let the user define which apps count as coding/productivity apps").
 */
data class CodingSessionBreakdown(
    val apps: List<AppUsageSummary>,
    val codingPackages: Set<String>,
    val codingDurationMillis: Long,
    val otherDurationMillis: Long
)
