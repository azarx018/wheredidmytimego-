package com.timetrace.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.domain.model.DailyUsageOverview
import com.timetrace.app.domain.model.UsageAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val isLoading: Boolean = true,
    val usageAccessState: UsageAccessState = UsageAccessState.NOT_GRANTED,
    val today: DailyUsageOverview? = null,
    val yesterdayTotalMillis: Long = 0L
)

class DashboardViewModel(private val repository: UsageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /** Call from the screen's LaunchedEffect / onResume - never from a timer. */
    fun refresh() {
        val access = repository.usageAccessState()
        if (access != UsageAccessState.GRANTED) {
            _uiState.value = DashboardUiState(isLoading = false, usageAccessState = access)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, usageAccessState = access)

            val today = LocalDate.now()
            val todayOverview = repository.getDailyOverview(today)
            val yesterdayOverview = repository.getDailyOverview(today.minusDays(1))

            _uiState.value = DashboardUiState(
                isLoading = false,
                usageAccessState = access,
                today = todayOverview,
                yesterdayTotalMillis = yesterdayOverview.totalDurationMillis
            )
        }
    }
}
