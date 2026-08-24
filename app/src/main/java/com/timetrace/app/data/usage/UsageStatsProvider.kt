package com.timetrace.app.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.domain.model.UsageSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thin wrapper around [UsageStatsManager].
 *
 * Design choice (see brief section 16 & 21): we never persist raw usage
 * events. We ask Android for the event log within a time window and
 * reconstruct sessions in memory each time a screen needs them. This keeps
 * the database small and avoids the app going stale relative to the source
 * of truth. Callers are expected to do this on screen-entry / explicit
 * refresh only, never on a timer.
 */
class UsageStatsProvider(context: Context) {

    private val appContext = context.applicationContext
    private val usageStatsManager =
        appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val appOpsManager =
        appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    fun usageAccessState(): UsageAccessState {
        val mode = appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            appContext.packageName
        )
        return if (mode == AppOpsManager.MODE_ALLOWED) {
            UsageAccessState.GRANTED
        } else {
            UsageAccessState.NOT_GRANTED
        }
    }

    /**
     * Reconstructs foreground sessions between [startMillis] and [endMillis]
     * by walking the raw event stream. Runs on [Dispatchers.Default] since
     * parsing the event log is CPU-bound, not I/O-bound.
     */
    suspend fun getSessions(startMillis: Long, endMillis: Long): List<UsageSession> =
        withContext(Dispatchers.Default) {
            if (usageAccessState() != UsageAccessState.GRANTED) return@withContext emptyList()

            val events = usageStatsManager.queryEvents(startMillis, endMillis)
            val openSessionStart = HashMap<String, Long>()
            val sessions = mutableListOf<UsageSession>()
            val event = UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue

                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND,
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        openSessionStart.putIfAbsent(pkg, event.timeStamp)
                    }

                    UsageEvents.Event.MOVE_TO_BACKGROUND,
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        val start = openSessionStart.remove(pkg)
                        if (start != null && event.timeStamp > start) {
                            sessions += UsageSession(pkg, start, event.timeStamp)
                        }
                    }
                }
            }

            // Any app still "open" at the end of the window (e.g. currently in
            // foreground) counts up to endMillis rather than being dropped.
            for ((pkg, start) in openSessionStart) {
                if (endMillis > start) {
                    sessions += UsageSession(pkg, start, endMillis)
                }
            }

            sessions
                .filter { it.durationMillis > 0 }
                .sortedBy { it.startTimeMillis }
        }
}
