package com.timetrace.app.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.timetrace.app.data.local.dao.AppCategoryDao
import com.timetrace.app.data.local.dao.AppInfoDao
import com.timetrace.app.data.usage.UsageStatsProvider
import com.timetrace.app.domain.model.AppUsageSummary
import com.timetrace.app.domain.model.DailyUsageOverview
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.domain.model.UsageSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class UsageRepository(
    private val context: Context,
    private val usageStatsProvider: UsageStatsProvider,
    private val appCategoryDao: AppCategoryDao,
    private val appInfoDao: AppInfoDao
) {
    private val packageManager: PackageManager get() = context.packageManager

    fun usageAccessState(): UsageAccessState = usageStatsProvider.usageAccessState()

    /** Sessions for a single calendar day, in the device's default time zone. */
    suspend fun getSessionsForDay(date: LocalDate): List<UsageSession> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return usageStatsProvider.getSessions(start, end)
    }

    suspend fun getDailyOverview(date: LocalDate): DailyUsageOverview =
        withContext(Dispatchers.Default) {
            val sessions = getSessionsForDay(date)
            val summaries = summarize(sessions)
            DailyUsageOverview(
                dateEpochDay = date.toEpochDay(),
                totalDurationMillis = summaries.sumOf { it.totalDurationMillis },
                appCount = summaries.size,
                topApps = summaries.sortedByDescending { it.totalDurationMillis }.take(5),
                mostUsedApp = summaries.maxByOrNull { it.totalDurationMillis }
            )
        }

    suspend fun getAppUsageList(date: LocalDate): List<AppUsageSummary> =
        withContext(Dispatchers.Default) {
            summarize(getSessionsForDay(date)).sortedByDescending { it.totalDurationMillis }
        }

    /** Sessions for one app on one day, sorted chronologically. Used by the App Detail screen. */
    suspend fun getSessionsForAppOnDay(packageName: String, date: LocalDate): List<UsageSession> =
        withContext(Dispatchers.Default) {
            getSessionsForDay(date)
                .filter { it.packageName == packageName }
                .sortedBy { it.startTimeMillis }
        }

    /**
     * Chronological timeline for a day (brief section 7): consecutive sessions of the
     * same app within [mergeGapMillis] of each other are merged into one entry so quick
     * app-switches and brief interruptions don't fragment the timeline into noise, while
     * genuinely separate sessions stay distinct.
     */
    suspend fun getTimelineForDay(date: LocalDate, mergeGapMillis: Long = 60_000L): List<UsageSession> =
        withContext(Dispatchers.Default) {
            val sorted = getSessionsForDay(date).sortedBy { it.startTimeMillis }
            val merged = mutableListOf<UsageSession>()
            for (session in sorted) {
                if (session.packageName == context.packageName) continue
                val last = merged.lastOrNull()
                if (last != null &&
                    last.packageName == session.packageName &&
                    session.startTimeMillis - last.endTimeMillis <= mergeGapMillis
                ) {
                    merged[merged.lastIndex] = last.copy(
                        endTimeMillis = maxOf(last.endTimeMillis, session.endTimeMillis)
                    )
                } else {
                    merged += session
                }
            }
            merged
        }

    private suspend fun summarize(sessions: List<UsageSession>): List<AppUsageSummary> =
        withContext(Dispatchers.Default) {
            sessions
                .groupBy { it.packageName }
                .mapNotNull { (pkg, pkgSessions) ->
                    // Skip our own app and packages the system won't resolve a label for.
                    if (pkg == context.packageName) return@mapNotNull null
                    val label = resolveAppLabel(pkg) ?: return@mapNotNull null
                    val appInfo = appInfoDao.get(pkg)
                    if (appInfo?.isExcluded == true) return@mapNotNull null

                    AppUsageSummary(
                        packageName = pkg,
                        appName = label,
                        totalDurationMillis = pkgSessions.sumOf { it.durationMillis },
                        sessionCount = pkgSessions.size,
                        lastUsedMillis = pkgSessions.maxOf { it.endTimeMillis },
                        categoryId = appInfo?.categoryId
                    )
                }
        }

    /** Returns null (and thus filters the app out) if it can no longer be resolved -
     * e.g. it was uninstalled after the usage event was recorded. See brief section 20.
     * Non-private: App Detail resolves a single label directly without a full day scan. */
    fun resolveAppLabel(packageName: String): String? = try {
        val info: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
        // Filter out non-launchable system components; they clutter usage lists
        // without being meaningful to the user.
        if (packageManager.getLaunchIntentForPackage(packageName) == null &&
            (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        ) {
            null
        } else {
            packageManager.getApplicationLabel(info).toString()
        }
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
