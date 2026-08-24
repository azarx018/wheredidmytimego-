package com.timetrace.app

import android.app.Application
import com.timetrace.app.data.local.AppDatabase
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.data.usage.UsageStatsProvider

/**
 * Manual, lightweight dependency container.
 *
 * A DI framework (Hilt/Dagger) was deliberately avoided: for an app this size
 * it adds APK weight and annotation-processing time without a real benefit.
 * Screens obtain dependencies from here via [TimeTraceApplication.from].
 */
class TimeTraceApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var usageRepository: UsageRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        val usageStatsProvider = UsageStatsProvider(this)
        usageRepository = UsageRepository(
            context = this,
            usageStatsProvider = usageStatsProvider,
            appCategoryDao = database.appCategoryDao(),
            appInfoDao = database.appInfoDao()
        )
    }

    companion object {
        fun from(context: android.content.Context): TimeTraceApplication =
            context.applicationContext as TimeTraceApplication
    }
}
