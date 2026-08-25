package com.timetrace.app.ui.screens.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.domain.model.AppUsageSummary
import com.timetrace.app.domain.model.UsageAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AppSortOrder { USAGE_TIME, NAME }

data class AppsUiState(
    val isLoading: Boolean = true,
    val usageAccessState: UsageAccessState = UsageAccessState.NOT_GRANTED,
    val selectedDate: LocalDate = LocalDate.now(),
    val sortOrder: AppSortOrder = AppSortOrder.USAGE_TIME,
    val apps: List<AppUsageSummary> = emptyList(),
    val totalDurationMillis: Long = 0L
)

class AppsViewModel(private val repository: UsageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

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
            val apps = repository.getAppUsageList(date)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                apps = sortApps(apps, _uiState.value.sortOrder),
                totalDurationMillis = apps.sumOf { it.totalDurationMillis }
            )
        }
    }

    fun onDateSelected(date: LocalDate) = refresh(date)

    fun onSortOrderChanged(order: AppSortOrder) {
        _uiState.value = _uiState.value.copy(
            sortOrder = order,
            apps = sortApps(_uiState.value.apps, order)
        )
    }

    private fun sortApps(apps: List<AppUsageSummary>, order: AppSortOrder): List<AppUsageSummary> =
        when (order) {
            AppSortOrder.USAGE_TIME -> apps.sortedByDescending { it.totalDurationMillis }
            AppSortOrder.NAME -> apps.sortedBy { it.appName.lowercase() }
        }
}
