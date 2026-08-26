package com.timetrace.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.timetrace.app.util.formatDuration

/**
 * One row in an app usage list: name, duration, and a slim progress bar
 * showing this app's share of the total. Deliberately simple - no custom
 * canvas drawing - to keep render cost and code size low. The bar animates
 * into place (Toggl-style smoothness) instead of snapping to its value.
 */
@Composable
fun AppUsageRow(
    appName: String,
    durationMillis: Long,
    fractionOfTotal: Float,
    modifier: Modifier = Modifier
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fractionOfTotal.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "usage_row_progress"
    )

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text(text = appName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = durationMillis.formatDuration(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
