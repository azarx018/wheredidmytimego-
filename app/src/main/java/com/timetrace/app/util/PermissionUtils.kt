package com.timetrace.app.util

import android.content.Context
import android.content.Intent
import android.provider.Settings

object PermissionUtils {
    /** Opens the system "Usage Access" settings screen so the user can grant it manually. */
    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
