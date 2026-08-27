package com.timetrace.app.ui.screens.categories

import androidx.lifecycle.ViewModel
import com.timetrace.app.data.local.entity.AppCategoryEntity
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.domain.model.AppUsageSummary
import com.timetrace.app.util.safeLaunch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val apps: List<AppUsageSummary> = emptyList(),
    val categories: List<AppCategoryEntity> = emptyList(),
    val error: Boolean = false
)

class CategoriesViewModel(private val repository: UsageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = false)

        safeLaunch(onError = {
            _uiState.value = _uiState.value.copy(isLoading = false, error = true)
        }) {
            // Today's app list is used as a practical "apps I actually have" source -
            // there's no separate installed-apps query, keeping this screen consistent
            // with what the rest of the app already shows.
            val apps = repository.getAppUsageList(LocalDate.now())
            val categories = repository.getAllCategories()
            _uiState.value = CategoriesUiState(isLoading = false, apps = apps, categories = categories)
        }
    }

    fun onCategoryAssigned(packageName: String, categoryId: Long?) {
        safeLaunch(onError = {
            _uiState.value = _uiState.value.copy(error = true)
        }) {
            repository.setAppCategory(packageName, categoryId)
            refresh()
        }
    }
}
