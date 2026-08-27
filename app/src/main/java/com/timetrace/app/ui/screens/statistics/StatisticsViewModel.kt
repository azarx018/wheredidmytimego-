package com.timetrace.app.ui.screens.statistics

import androidx.lifecycle.ViewModel
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.domain.model.CategoryUsageSummary
import com.timetrace.app.domain.model.DailyUsageOverview
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.domain.model.WeeklyUsageOverview
import com.timetrace.app.util.safeLaunch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

enum class StatsPeriod { DAILY, WEEKLY }

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val usageAccessState: UsageAccessState = UsageAccessState.NOT_GRANTED,
    val period: StatsPeriod = StatsPeriod.DAILY,
    val daily: DailyUsageOverview? = null,
    val dailySessionCount: Int = 0,
    val dailyAverageSessionMillis: Long = 0L,
    val categories: List<CategoryUsageSummary> = emptyList(),
    val weekly: WeeklyUsageOverview? = null,
    val error: Boolean = false
)

class StatisticsViewModel(private val repository: UsageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    fun refresh() {
        val access = repository.usageAccessState()
        if (access != UsageAccessState.GRANTED) {
            _uiState.value = _uiState.value.copy(isLoading = false, usageAccessState = access)
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, usageAccessState = access, error = false)

        safeLaunch(onError = {
            _uiState.value = _uiState.value.copy(isLoading = false, error = true)
        }) {
            val today = LocalDate.now()
            val daily = repository.getDailyOverview(today)
            val apps = repository.getAppUsageList(today)
            val sessionCount = apps.sumOf { it.sessionCount }
            val avgSession = if (sessionCount > 0) apps.sumOf { it.totalDurationMillis } / sessionCount else 0L
            val categories = repository.getCategoryBreakdown(today)
            val weekly = repository.getWeeklyOverview(today)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                daily = daily,
                dailySessionCount = sessionCount,
                dailyAverageSessionMillis = avgSession,
                categories = categories,
                weekly = weekly
            )
        }
    }

    fun onPeriodChanged(period: StatsPeriod) {
        _uiState.value = _uiState.value.copy(period = period)
    }
}
