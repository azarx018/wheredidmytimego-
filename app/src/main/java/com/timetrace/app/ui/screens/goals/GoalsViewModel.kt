package com.timetrace.app.ui.screens.goals

import androidx.lifecycle.ViewModel
import com.timetrace.app.data.local.entity.AppCategoryEntity
import com.timetrace.app.data.local.entity.GoalTargetType
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.domain.model.AppUsageSummary
import com.timetrace.app.domain.model.GoalProgress
import com.timetrace.app.util.safeLaunch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class GoalsUiState(
    val isLoading: Boolean = true,
    val goals: List<GoalProgress> = emptyList(),
    val availableApps: List<AppUsageSummary> = emptyList(),
    val availableCategories: List<AppCategoryEntity> = emptyList(),
    val error: Boolean = false
)

class GoalsViewModel(private val repository: UsageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = false)

        safeLaunch(onError = {
            _uiState.value = _uiState.value.copy(isLoading = false, error = true)
        }) {
            val today = LocalDate.now()
            val goals = repository.getGoalsWithProgress(today)
            val apps = repository.getAppUsageList(today)
            val categories = repository.getAllCategories()
            _uiState.value = GoalsUiState(
                isLoading = false,
                goals = goals,
                availableApps = apps,
                availableCategories = categories
            )
        }
    }

    fun addAppGoal(packageName: String, targetDurationMillis: Long) {
        safeLaunch(onError = { _uiState.value = _uiState.value.copy(error = true) }) {
            repository.createGoal(GoalTargetType.APP, packageName, targetDurationMillis)
            refresh()
        }
    }

    fun addCategoryGoal(categoryId: Long, targetDurationMillis: Long) {
        safeLaunch(onError = { _uiState.value = _uiState.value.copy(error = true) }) {
            repository.createGoal(GoalTargetType.CATEGORY, categoryId.toString(), targetDurationMillis)
            refresh()
        }
    }

    fun deleteGoal(id: Long) {
        safeLaunch(onError = { _uiState.value = _uiState.value.copy(error = true) }) {
            repository.deleteGoal(id)
            refresh()
        }
    }
}
