package com.timetrace.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_category")
data class AppCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val isDefault: Boolean = false
)

object DefaultCategories {
    // Seeded once on first launch; ids are stable so AppInfoEntity.categoryId
    // can reference them without a lookup-by-name.
    val seed = listOf(
        AppCategoryEntity(1, "Coding", "#7C4DFF", true),
        AppCategoryEntity(2, "Social", "#2ED9C3", true),
        AppCategoryEntity(3, "Entertainment", "#FFC24B", true),
        AppCategoryEntity(4, "Browser", "#4E9AFF", true),
        AppCategoryEntity(5, "Communication", "#4CD97B", true),
        AppCategoryEntity(6, "Productivity", "#FF9F4E", true),
        AppCategoryEntity(7, "Games", "#FF5C7A", true),
        AppCategoryEntity(8, "Other", "#9A9AA6", true)
    )
}
