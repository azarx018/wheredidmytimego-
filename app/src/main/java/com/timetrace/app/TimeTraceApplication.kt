package com.timetrace.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.timetrace.app.data.local.AppDatabase
import com.timetrace.app.data.local.SettingsDataStore
import com.timetrace.app.data.notification.DailySummaryWorker
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.data.usage.UsageStatsProvider
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

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

    lateinit var settingsDataStore: SettingsDataStore
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)
        val usageStatsProvider = UsageStatsProvider(this)
        usageRepository = UsageRepository(
            context = this,
            usageStatsProvider = usageStatsProvider,
            appCategoryDao = database.appCategoryDao(),
            appInfoDao = database.appInfoDao(),
            goalDao = database.goalDao(),
            codingSessionDao = database.codingSessionDao()
        )

        scheduleDailySummaryWork()
    }

    /**
     * Always scheduled, unconditionally - DailySummaryWorker itself checks the
     * notifications-enabled flag and Usage Access before posting anything, so
     * toggling the Settings switch doesn't need to reach into WorkManager to
     * cancel/re-enqueue. enqueueUniquePeriodicWork + KEEP means this is a
     * no-op on every app start after the first.
     */
    private fun scheduleDailySummaryWork() {
        val now = LocalDateTime.now()
        var nextRun = now.withHour(20).withMinute(0).withSecond(0).withNano(0)
        if (now.isAfter(nextRun)) nextRun = nextRun.plusDays(1)
        val initialDelay = Duration.between(now, nextRun).toMillis()

        val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_summary",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        fun from(context: android.content.Context): TimeTraceApplication =
            context.applicationContext as TimeTraceApplication
    }
}
