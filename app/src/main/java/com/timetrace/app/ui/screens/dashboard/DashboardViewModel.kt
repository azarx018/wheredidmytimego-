package com.timetrace.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import com.timetrace.app.data.local.entity.CodingSessionEntity
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.domain.model.CategoryUsageSummary
import com.timetrace.app.domain.model.DailyUsageOverview
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.util.safeLaunch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class DashboardUiState(
    val isLoading: Boolean = true,
    val usageAccessState: UsageAccessState = UsageAccessState.NOT_GRANTED,
    val today: DailyUsageOverview? = null,
    val yesterdayTotalMillis: Long = 0L,
    val categories: List<CategoryUsageSummary> = emptyList(),
    val activeCodingSession: CodingSessionEntity? = null,
    val error: Boolean = false
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

        _uiState.value = _uiState.value.copy(isLoading = true, usageAccessState = access, error = false)

        safeLaunch(onError = {
            _uiState.value = _uiState.value.copy(isLoading = false, error = true)
        }) {
            val today = LocalDate.now()
            val todayOverview = repository.getDailyOverview(today)
            val yesterdayOverview = repository.getDailyOverview(today.minusDays(1))
            val categories = repository.getCategoryBreakdown(today)
            // Surfaced as a small Toggl-style "still running" banner so a
            // coding session started earlier isn't forgotten about off-screen.
            val activeCodingSession = repository.getActiveCodingSession()

            _uiState.value = DashboardUiState(
                isLoading = false,
                usageAccessState = access,
                today = todayOverview,
                yesterdayTotalMillis = yesterdayOverview.totalDurationMillis,
                categories = categories,
                activeCodingSession = activeCodingSession
            )
        }
    }
}
