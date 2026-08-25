package com.timetrace.app.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Launches a coroutine in this ViewModel's scope and funnels any exception
 * into [onError] instead of letting it crash the app.
 *
 * A bare `viewModelScope.launch { ... }` does NOT satisfy brief section 20
 * ("do not crash because one application cannot be resolved", "handle
 * database errors", etc.) on its own: an uncaught exception inside it
 * propagates up and kills the process. This is the fix for exactly that -
 * every screen's data loading goes through this instead of a raw launch.
 */
fun ViewModel.safeLaunch(onError: (Throwable) -> Unit = {}, block: suspend () -> Unit) {
    viewModelScope.launch {
        try {
            block()
        } catch (c: CancellationException) {
            throw c // structured concurrency cancellation must propagate, never swallow it
        } catch (t: Throwable) {
            onError(t)
        }
    }
}
