package com.timetrace.app.ui.screens.coding

import androidx.lifecycle.ViewModel
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.domain.model.AppUsageSummary
import com.timetrace.app.util.safeLaunch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class CodingAppsUiState(
    val isLoading: Boolean = true,
    val apps: List<AppUsageSummary> = emptyList(),
    val codingPackages: Set<String> = emptySet(),
    val error: Boolean = false
)

class CodingAppsViewModel(private val repository: UsageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CodingAppsUiState())
    val uiState: StateFlow<CodingAppsUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = false)

        safeLaunch(onError = {
            _uiState.value = _uiState.value.copy(isLoading = false, error = true)
        }) {
            val apps = repository.getAppUsageList(LocalDate.now())
            val codingPackages = repository.getCodingAppPackageNames()
            _uiState.value = CodingAppsUiState(isLoading = false, apps = apps, codingPackages = codingPackages)
        }
    }

    fun toggle(packageName: String, isCoding: Boolean) {
        safeLaunch(onError = { _uiState.value = _uiState.value.copy(error = true) }) {
            repository.setCodingApp(packageName, isCoding)
            refresh()
        }
    }
}
