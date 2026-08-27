package com.timetrace.app.data.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.timetrace.app.R

object NotificationHelper {
    private const val CHANNEL_ID = "timetrace_summary"
    private const val DAILY_SUMMARY_NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Your daily screen-time summary and goal progress"
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Neutral, factual phrasing per brief section 12 - a report, not a scolding.
     *
     * @SuppressLint is needed because lint's permission analysis can't trace
     * through [hasPermission] to know the check already happened - it only
     * recognizes inline checkSelfPermission calls at the call site. The
     * try/catch below is the real safety net lint is asking for: if the
     * permission is revoked in the moment between our check and the actual
     * call (rare, but possible), this fails silently instead of crashing.
     */
    @SuppressLint("MissingPermission")
    fun showDailySummary(context: Context, totalDurationText: String) {
        if (!hasPermission(context)) return

        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("You used your phone for $totalDurationText today.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(DAILY_SUMMARY_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission revoked between our check and this call - skip silently.
        }
    }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
