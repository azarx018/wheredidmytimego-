package com.timetrace.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.timetrace.app.data.local.dao.AppCategoryDao
import com.timetrace.app.data.local.dao.AppInfoDao
import com.timetrace.app.data.local.dao.CodingSessionDao
import com.timetrace.app.data.local.dao.GoalDao
import com.timetrace.app.data.local.entity.AppCategoryEntity
import com.timetrace.app.data.local.entity.AppInfoEntity
import com.timetrace.app.data.local.entity.CodingSessionEntity
import com.timetrace.app.data.local.entity.DefaultCategories
import com.timetrace.app.data.local.entity.GoalEntity
import com.timetrace.app.data.local.entity.GoalTargetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter
    fun fromGoalTargetType(value: GoalTargetType): String = value.name

    @TypeConverter
    fun toGoalTargetType(value: String): GoalTargetType = GoalTargetType.valueOf(value)
}

/**
 * Deliberately small schema: only user configuration (categories, per-app
 * settings, goals, coding sessions) is persisted. Raw usage history stays in
 * Android's own UsageStatsManager and is read on demand - see
 * [com.timetrace.app.data.usage.UsageStatsProvider].
 */
@Database(
    entities = [
        AppCategoryEntity::class,
        AppInfoEntity::class,
        GoalEntity::class,
        CodingSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appCategoryDao(): AppCategoryDao
    abstract fun appInfoDao(): AppInfoDao
    abstract fun goalDao(): GoalDao
    abstract fun codingSessionDao(): CodingSessionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): AppDatabase {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "timetrace.db"
            ).build()

            // Seed default categories once, off the main thread.
            CoroutineScope(Dispatchers.IO).launch {
                if (db.appCategoryDao().count() == 0) {
                    db.appCategoryDao().insertAll(DefaultCategories.seed)
                }
            }
            return db
        }
    }
}
