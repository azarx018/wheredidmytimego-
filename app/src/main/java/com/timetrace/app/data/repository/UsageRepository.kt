package com.timetrace.app.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.timetrace.app.data.local.dao.AppCategoryDao
import com.timetrace.app.data.local.dao.AppInfoDao
import com.timetrace.app.data.local.dao.CodingSessionDao
import com.timetrace.app.data.local.dao.GoalDao
import com.timetrace.app.data.local.entity.AppInfoEntity
import com.timetrace.app.data.local.entity.CodingSessionEntity
import com.timetrace.app.data.local.entity.GoalEntity
import com.timetrace.app.data.local.entity.GoalTargetType
import com.timetrace.app.data.usage.UsageStatsProvider
import com.timetrace.app.domain.model.AppUsageSummary
import com.timetrace.app.domain.model.CategoryUsageSummary
import com.timetrace.app.domain.model.CodingSessionBreakdown
import com.timetrace.app.domain.model.DailyUsageOverview
import com.timetrace.app.domain.model.DayTotal
import com.timetrace.app.domain.model.GoalProgress
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.domain.model.UsageSession
import com.timetrace.app.domain.model.WeeklyUsageOverview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class UsageRepository(
    private val context: Context,
    private val usageStatsProvider: UsageStatsProvider,
    private val appCategoryDao: AppCategoryDao,
    private val appInfoDao: AppInfoDao,
    private val goalDao: GoalDao,
    private val codingSessionDao: CodingSessionDao
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
     * genuinely separate sessions stay distinct. Filtered through [resolveAppLabel] the
     * same way Apps/Dashboard are, so background system components that emit usage
     * events (keyboard, launcher, SystemUI, ...) don't flood the timeline with entries
     * the user never actually "used".
     */
    suspend fun getTimelineForDay(date: LocalDate, mergeGapMillis: Long = 60_000L): List<UsageSession> =
        withContext(Dispatchers.Default) {
            val sorted = getSessionsForDay(date)
                .filter { it.packageName != context.packageName }
                .filter { resolveAppLabel(it.packageName) != null }
                .sortedBy { it.startTimeMillis }
            val merged = mutableListOf<UsageSession>()
            for (session in sorted) {
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

    /**
     * Aggregated stats over the 7 days ending on [endDate] inclusive (brief section 8:
     * Weekly). Reuses getAppUsageList per day rather than a separate raw-session pass -
     * this screen is opened rarely enough that 7 sequential day summaries are cheap.
     */
    suspend fun getWeeklyOverview(endDate: LocalDate = LocalDate.now()): WeeklyUsageOverview =
        withContext(Dispatchers.Default) {
            val dates = (0..6).map { endDate.minusDays((6 - it).toLong()) } // oldest first
            val dayTotals = mutableListOf<DayTotal>()
            val appAccum = mutableMapOf<String, AppUsageSummary>()

            for (date in dates) {
                val apps = getAppUsageList(date)
                dayTotals += DayTotal(date, apps.sumOf { it.totalDurationMillis })
                for (app in apps) {
                    val existing = appAccum[app.packageName]
                    appAccum[app.packageName] = if (existing == null) {
                        app
                    } else {
                        existing.copy(totalDurationMillis = existing.totalDurationMillis + app.totalDurationMillis)
                    }
                }
            }

            val total = dayTotals.sumOf { it.totalDurationMillis }
            WeeklyUsageOverview(
                days = dayTotals,
                totalDurationMillis = total,
                averageDailyMillis = if (dayTotals.isNotEmpty()) total / dayTotals.size else 0L,
                mostUsedApp = appAccum.values.maxByOrNull { it.totalDurationMillis },
                highestUsageDay = dayTotals.maxByOrNull { it.totalDurationMillis }?.date
            )
        }

    suspend fun getAllCategories() = appCategoryDao.getAll()

    suspend fun setAppCategory(packageName: String, categoryId: Long?) {
        val existing = appInfoDao.get(packageName)
        appInfoDao.upsert(
            (existing ?: AppInfoEntity(packageName = packageName)).copy(categoryId = categoryId)
        )
    }

    /** Per-category totals for a day (brief section 9), unassigned apps fold into "Other". */
    suspend fun getCategoryBreakdown(date: LocalDate): List<CategoryUsageSummary> =
        withContext(Dispatchers.Default) {
            val apps = getAppUsageList(date)
            val categories = getAllCategories()
            val categoryById = categories.associateBy { it.id }
            val otherCategory = categories.find { it.name == "Other" }

            apps.groupBy { it.categoryId ?: otherCategory?.id }
                .map { (categoryId, appsInCategory) ->
                    val category = categoryId?.let { categoryById[it] }
                    CategoryUsageSummary(
                        categoryId = categoryId,
                        categoryName = category?.name ?: "Other",
                        colorHex = category?.colorHex ?: "#9A9AA6",
                        totalDurationMillis = appsInCategory.sumOf { it.totalDurationMillis }
                    )
                }
                .sortedByDescending { it.totalDurationMillis }
        }

    // --- Goals (brief section 11) ---

    suspend fun getGoalsWithProgress(date: LocalDate = LocalDate.now()): List<GoalProgress> =
        withContext(Dispatchers.Default) {
            val goals = goalDao.getAll()
            if (goals.isEmpty()) return@withContext emptyList()

            val apps = getAppUsageList(date)
            val categories = getCategoryBreakdown(date)
            val categoryEntities = getAllCategories()

            goals.map { goal ->
                when (goal.targetType) {
                    GoalTargetType.APP -> {
                        val app = apps.find { it.packageName == goal.targetId }
                        GoalProgress(
                            goal = goal,
                            label = app?.appName ?: resolveAppLabel(goal.targetId) ?: goal.targetId,
                            currentDurationMillis = app?.totalDurationMillis ?: 0L
                        )
                    }
                    GoalTargetType.CATEGORY -> {
                        val categoryId = goal.targetId.toLongOrNull()
                        val categoryUsage = categories.find { it.categoryId == categoryId }
                        val categoryName = categoryEntities.find { it.id == categoryId }?.name ?: "Category"
                        GoalProgress(
                            goal = goal,
                            label = categoryName,
                            currentDurationMillis = categoryUsage?.totalDurationMillis ?: 0L
                        )
                    }
                }
            }
        }

    suspend fun createGoal(targetType: GoalTargetType, targetId: String, targetDurationMillis: Long) {
        goalDao.insert(
            GoalEntity(targetType = targetType, targetId = targetId, targetDurationMillis = targetDurationMillis)
        )
    }

    suspend fun deleteGoal(id: Long) {
        goalDao.delete(id)
    }

    // --- Coding Session (brief section 10) ---

    suspend fun setCodingApp(packageName: String, isCodingApp: Boolean) {
        val existing = appInfoDao.get(packageName)
        appInfoDao.upsert(
            (existing ?: AppInfoEntity(packageName = packageName)).copy(isCodingApp = isCodingApp)
        )
    }

    suspend fun getCodingAppPackageNames(): Set<String> = appInfoDao.getCodingAppPackages().toSet()

    suspend fun getActiveCodingSession(): CodingSessionEntity? = codingSessionDao.getActiveSession()

    suspend fun startCodingSession(): CodingSessionEntity {
        val session = CodingSessionEntity(startTimeMillis = System.currentTimeMillis())
        val id = codingSessionDao.insert(session)
        return session.copy(id = id)
    }

    suspend fun stopCodingSession(session: CodingSessionEntity) {
        codingSessionDao.update(session.copy(endTimeMillis = System.currentTimeMillis()))
    }

    /**
     * Per-app usage during an in-progress or completed coding session window, split by
     * which apps the user has flagged as coding apps. Deliberately does NOT claim every
     * app used is "productive" - see brief section 10.
     */
    suspend fun getCodingSessionBreakdown(startMillis: Long, endMillis: Long): CodingSessionBreakdown =
        withContext(Dispatchers.Default) {
            val sessions = usageStatsProvider.getSessions(startMillis, endMillis)
                .filter { it.packageName != context.packageName }
            val codingPackages = getCodingAppPackageNames()

            val perApp = sessions
                .groupBy { it.packageName }
                .mapNotNull { (pkg, pkgSessions) ->
                    val label = resolveAppLabel(pkg) ?: return@mapNotNull null
                    AppUsageSummary(
                        packageName = pkg,
                        appName = label,
                        totalDurationMillis = pkgSessions.sumOf { it.durationMillis },
                        sessionCount = pkgSessions.size,
                        lastUsedMillis = pkgSessions.maxOf { it.endTimeMillis }
                    )
                }
                .sortedByDescending { it.totalDurationMillis }

            val codingMillis = perApp.filter { it.packageName in codingPackages }.sumOf { it.totalDurationMillis }
            val totalMillis = perApp.sumOf { it.totalDurationMillis }

            CodingSessionBreakdown(
                apps = perApp,
                codingPackages = codingPackages,
                codingDurationMillis = codingMillis,
                otherDurationMillis = totalMillis - codingMillis
            )
        }

    /**
     * Resets TimeTrace's own configuration (category assignments, goals, coding session
     * history). Deliberately does NOT touch app_category itself - a user's custom
     * categories are taxonomy, not derived data, and deleting them would just force an
     * immediate re-seed of the defaults anyway. Usage data is Android's, not ours to clear.
     */
    suspend fun clearAllLocalData() {
        appInfoDao.deleteAll()
        goalDao.deleteAll()
        codingSessionDao.deleteAll()
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
     * e.g. it was uninstalled, or a transient PackageManager/system exception occurs.
     * See brief section 20 ("Do not crash because one application cannot be resolved").
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
    } catch (e: Exception) {
        // Deliberately broad: PackageManager can throw NameNotFoundException
        // (uninstalled), SecurityException (package-visibility restrictions
        // on Android 11+), or transient RuntimeExceptions from the system
        // service dying/restarting. Any of these should just filter this one
        // app out, never crash the screen that's asking about it.
        null
    }
}
