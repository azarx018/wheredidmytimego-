package com.timetrace.app.domain.model

/** Aggregated session statistics for one app on one day (brief section 6). */
data class SessionStats(
    val sessions: List<UsageSession>,
    val totalDurationMillis: Long,
    val averageSessionMillis: Long,
    val longestSessionMillis: Long
)
