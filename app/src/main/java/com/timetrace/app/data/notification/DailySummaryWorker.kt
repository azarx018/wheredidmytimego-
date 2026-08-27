package com.timetrace.app.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.timetrace.app.TimeTraceApplication
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.util.formatDuration
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Posts the "You used your phone for Xh Ym today" notification once daily.
 * Silently no-ops (returns success without posting) if notifications are
 * disabled in Settings or Usage Access isn't granted - this worker is always
 * scheduled at app start (see TimeTraceApplication), and checks both flags
 * itself rather than the app conditionally scheduling/unscheduling it.
 */
class DailySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = TimeTraceApplication.from(applicationContext)

        val notificationsEnabled = app.settingsDataStore.notificationsEnabled.first()
        if (!notificationsEnabled) return Result.success()

        if (app.usageRepository.usageAccessState() != UsageAccessState.GRANTED) return Result.success()

        return try {
            val overview = app.usageRepository.getDailyOverview(LocalDate.now())
            NotificationHelper.showDailySummary(applicationContext, overview.totalDurationMillis.formatDuration())
            Result.success()
        } catch (e: Exception) {
            // A missed daily summary isn't worth retrying aggressively and
            // spamming the system with retries - just skip today's.
            Result.success()
        }
    }
}
