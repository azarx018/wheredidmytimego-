package com.timetrace.app.ui.screens.replay

import androidx.lifecycle.ViewModel
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.domain.model.UsageSession
import com.timetrace.app.util.safeLaunch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class ReplayUiState(
    val isLoading: Boolean = true,
    val usageAccessState: UsageAccessState = UsageAccessState.NOT_GRANTED,
    val selectedDate: LocalDate = LocalDate.now(),
    val entries: List<UsageSession> = emptyList(),
    val appNames: Map<String, String> = emptyMap(),
    val visibleCount: Int = 0,
    val isPlaying: Boolean = false,
    val error: Boolean = false
)

class ReplayViewModel(private val repository: UsageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ReplayUiState())
    val uiState: StateFlow<ReplayUiState> = _uiState.asStateFlow()

    fun load(date: LocalDate = _uiState.value.selectedDate) {
        val access = repository.usageAccessState()
        if (access != UsageAccessState.GRANTED) {
            _uiState.value = _uiState.value.copy(isLoading = false, usageAccessState = access)
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            usageAccessState = access,
            selectedDate = date,
            visibleCount = 0,
            isPlaying = false,
            error = false
        )

        safeLaunch(onError = {
            _uiState.value = _uiState.value.copy(isLoading = false, error = true)
        }) {
            val entries = repository.getTimelineForDay(date)
            val names = repository.getAppUsageList(date).associate { it.packageName to it.appName }
            _uiState.value = _uiState.value.copy(isLoading = false, entries = entries, appNames = names)
        }
    }

    fun onDateSelected(date: LocalDate) = load(date)

    fun setPlaying(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }

    fun revealNext(): Boolean {
        val state = _uiState.value
        if (state.visibleCount >= state.entries.size) return false
        _uiState.value = state.copy(visibleCount = state.visibleCount + 1)
        return state.visibleCount + 1 < state.entries.size
    }

    fun reset() {
        _uiState.value = _uiState.value.copy(visibleCount = 0, isPlaying = false)
    }

    fun revealAll() {
        _uiState.value = _uiState.value.copy(visibleCount = _uiState.value.entries.size, isPlaying = false)
    }
}
