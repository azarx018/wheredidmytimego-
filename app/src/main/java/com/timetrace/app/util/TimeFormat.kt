package com.timetrace.app.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/** Formats a duration as "1h 05m" / "42m" / "0m", matching the mockups. */
fun Long.formatDuration(): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(this)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes.toString().padStart(2, '0')}m" else "${minutes}m"
}

private val timeFormatter24h = DateTimeFormatter.ofPattern("HH:mm")

fun Long.formatClockTime(zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(this).atZone(zone).format(timeFormatter24h)

/** Formats a duration as "01:17:42" for the running coding-session timer. */
fun Long.formatElapsed(): String {
    val totalSeconds = this / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
