package com.timetrace.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timetrace.app.domain.model.CategoryUsageSummary
import com.timetrace.app.util.formatDuration

/**
 * The dashboard's signature visual, styled after Digital Wellbeing's ring
 * chart: a donut split into arcs by category, with the total centered
 * inside. Sweeps in from empty on first composition rather than snapping to
 * full - a small bit of life that a plain number-and-list dashboard lacks.
 */
@Composable
fun UsageRingChart(
    totalDurationMillis: Long,
    categories: List<CategoryUsageSummary>,
    modifier: Modifier = Modifier,
    diameter: Dp = 168.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 900),
        label = "ring_sweep"
    )

    val strokeWidthDp = 16.dp
    val fallbackColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val strokePx = strokeWidthDp.toPx()
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(strokePx / 2, strokePx / 2)

            // Track background
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (totalDurationMillis > 0 && categories.isNotEmpty()) {
                var startAngle = -90f
                categories.forEach { category ->
                    val fraction = category.totalDurationMillis.toFloat() / totalDurationMillis.toFloat()
                    val sweep = 360f * fraction * animatedProgress
                    val color = runCatching { Color(android.graphics.Color.parseColor(category.colorHex)) }
                        .getOrDefault(fallbackColor)
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    startAngle += 360f * fraction
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                totalDurationMillis.formatDuration(),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                "today",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
