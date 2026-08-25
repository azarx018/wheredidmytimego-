package com.timetrace.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GoalTargetType { APP, CATEGORY }

@Entity(tableName = "goal")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetType: GoalTargetType,
    val targetId: String, // packageName if APP, categoryId.toString() if CATEGORY
    val targetDurationMillis: Long,
    val enabled: Boolean = true
)
