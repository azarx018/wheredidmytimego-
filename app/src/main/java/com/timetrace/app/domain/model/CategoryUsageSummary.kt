package com.timetrace.app.domain.model

/** Total usage for one category on a given day (brief section 9). */
data class CategoryUsageSummary(
    val categoryId: Long?,
    val categoryName: String,
    val colorHex: String,
    val totalDurationMillis: Long
)
