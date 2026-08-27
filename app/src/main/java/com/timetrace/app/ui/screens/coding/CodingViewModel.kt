package com.timetrace.app.ui.screens.coding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrace.app.data.local.entity.CodingSessionEntity
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.domain.model.CodingSessionBreakdown
import com.timetrace.app.util.safeLaunch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CodingUiState(
    val isLoading: Boolean = true,
    val activeSession: CodingSessionEntity? = null,
    val elapsedMillis: Long = 0L,
    val breakdown: CodingSessionBreakdown? = null,
    val error: Boolean = false
)

class CodingViewModel(private val repository: UsageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CodingUiState())
    val uiState: StateFlow<CodingUiState> = _uiState.asStateFlow()
    private var tickerJob: Job? = null

    fun onScreenEntered() {
        safeLaunch(onError = {
            _uiState.value = _uiState.value.copy(isLoading = false, error = true)
        }) {
            val active = repository.getActiveCodingSession()
            _uiState.value = _uiState.value.copy(isLoading = false, activeSession = active, error = false)
            if (active != null) startTicking(active)
        }
    }

    fun startSession() {
        safeLaunch(onError = { _uiState.value = _uiState.value.copy(error = true) }) {
            val session = repository.startCodingSession()
            _uiState.value = _uiState.value.copy(activeSession = session, error = false)
            startTicking(session)
        }
    }

    fun stopSession() {
        val session = _uiState.value.activeSession ?: return
        tickerJob?.cancel()
        safeLaunch(onError = { _uiState.value = _uiState.value.copy(error = true) }) {
            repository.stopCodingSession(session)
            _uiState.value = _uiState.value.copy(activeSession = null, elapsedMillis = 0L, breakdown = null)
        }
    }

    private fun startTicking(session: CodingSessionEntity) {
        tickerJob?.cancel()
        // Refreshes on a slow interval while this screen is visible only - not
        // a background poll (see brief section 21). Cancelled in stopSession()
        // and onCleared(); a single failed refresh doesn't kill the loop, it
        // just tries again next tick.
        tickerJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val now = System.currentTimeMillis()
                    val breakdown = repository.getCodingSessionBreakdown(session.startTimeMillis, now)
                    _uiState.value = _uiState.value.copy(
                        elapsedMillis = now - session.startTimeMillis,
                        breakdown = breakdown
                    )
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    // Skip this tick, keep the timer/loop alive for the next one.
                }
                delay(5_000L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}
