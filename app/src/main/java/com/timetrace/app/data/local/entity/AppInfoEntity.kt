package com.timetrace.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfoEntity(
    @PrimaryKey val packageName: String,
    val categoryId: Long? = null,
    val isExcluded: Boolean = false,
    val isCodingApp: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
