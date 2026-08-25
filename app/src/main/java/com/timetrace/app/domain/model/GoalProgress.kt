package com.timetrace.app.domain.model

import com.timetrace.app.data.local.entity.GoalEntity

/** A goal paired with today's actual usage against it (brief section 11). */
data class GoalProgress(
    val goal: GoalEntity,
    val label: String,
    val currentDurationMillis: Long
)
