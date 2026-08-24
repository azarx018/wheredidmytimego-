package com.timetrace.app.ui.screens.appdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.domain.model.SessionStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AppDetailUiState(
    val isLoading: Boolean = true,
    val appName: String = "",
    val stats: SessionStats? = null
)

class AppDetailViewModel(
    private val repository: UsageRepository,
    val packageName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    fun load(date: LocalDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val sessions = repository.getSessionsForAppOnDay(packageName, date)
            val appName = repository.resolveAppLabel(packageName) ?: packageName
            val total = sessions.sumOf { it.durationMillis }

            val stats = SessionStats(
                sessions = sessions,
                totalDurationMillis = total,
                averageSessionMillis = if (sessions.isNotEmpty()) total / sessions.size else 0L,
                longestSessionMillis = sessions.maxOfOrNull { it.durationMillis } ?: 0L
            )

            _uiState.value = AppDetailUiState(isLoading = false, appName = appName, stats = stats)
        }
    }
}
