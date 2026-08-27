package com.timetrace.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.timetrace.app.domain.model.DayTotal
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * Minimal weekly bar chart drawn with Canvas - deliberately no charting
 * library, per the "keep the APK as light as possible" requirement.
 */
@Composable
fun WeeklyBarChart(days: List<DayTotal>, modifier: Modifier = Modifier) {
    val maxMillis = (days.maxOfOrNull { it.totalDurationMillis } ?: 0L).coerceAtLeast(1L)
    val today = LocalDate.now()
    val barColor = MaterialTheme.colorScheme.primary

    // Locale.getDefault() isn't observable by Compose - it won't recompose if
    // the device locale changes while this screen is visible. Reading it via
    // LocalConfiguration makes that change trigger recomposition correctly.
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: java.util.Locale.getDefault()

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val barCount = days.size.coerceAtLeast(1)
            val gap = 12.dp.toPx()
            val barWidth = (size.width - gap * (barCount - 1)) / barCount

            days.forEachIndexed { index, day ->
                val fraction = day.totalDurationMillis.toFloat() / maxMillis.toFloat()
                val barHeight = size.height * fraction.coerceIn(0.02f, 1f)
                val left = index * (barWidth + gap)
                drawRoundRect(
                    color = if (day.date == today) barColor else barColor.copy(alpha = 0.5f),
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            days.forEach { day ->
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(1),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
