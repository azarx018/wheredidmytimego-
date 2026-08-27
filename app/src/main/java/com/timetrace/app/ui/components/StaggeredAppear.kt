package com.timetrace.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Wraps [content] so it fades and slides in shortly after appearing, staggered
 * by [index]. Used across list screens (Dashboard top apps, Apps list, ...)
 * so content feels like it's arriving rather than just being there - a small
 * touch borrowed from Daylio's lively history entries and Toggl's snappy list
 * transitions, rather than the more static Digital-Wellbeing-style dashboard.
 *
 * Delay is capped so long lists don't make the bottom items wait forever.
 */
@Composable
fun StaggeredAppear(index: Int, content: @Composable () -> Unit) {
    var visible by remember(index) { mutableStateOf(false) }

    LaunchedEffect(index) {
        delay((index.coerceAtMost(10) * 35).toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 5 }
    ) {
        content()
    }
}
