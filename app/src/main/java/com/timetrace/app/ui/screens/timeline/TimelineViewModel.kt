package com.timetrace.app.ui.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.domain.model.UsageSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TimelineUiState(
    val isLoading: Boolean = true,
    val usageAccessState: UsageAccessState = UsageAccessState.NOT_GRANTED,
    val selectedDate: LocalDate = LocalDate.now(),
    val entries: List<UsageSession> = emptyList(),
    val appNames: Map<String, String> = emptyMap()
)

class TimelineViewModel(private val repository: UsageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    fun refresh(date: LocalDate = _uiState.value.selectedDate) {
        val access = repository.usageAccessState()
        if (access != UsageAccessState.GRANTED) {
            _uiState.value = _uiState.value.copy(isLoading = false, usageAccessState = access)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                usageAccessState = access,
                selectedDate = date
            )
            val entries = repository.getTimelineForDay(date)
            val names = repository.getAppUsageList(date).associate { it.packageName to it.appName }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                entries = entries,
                appNames = names
            )
        }
    }

    fun onDateSelected(date: LocalDate) = refresh(date)
}
