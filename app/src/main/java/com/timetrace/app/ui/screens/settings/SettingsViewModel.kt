package com.timetrace.app.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrace.app.data.local.SettingsDataStore
import com.timetrace.app.data.local.ThemeMode
import com.timetrace.app.data.repository.UsageRepository
import com.timetrace.app.domain.model.UsageAccessState
import com.timetrace.app.util.safeLaunch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class ExportState { IDLE, EXPORTING, SUCCESS, FAILED }

data class SettingsUiState(
    val usageAccessState: UsageAccessState = UsageAccessState.NOT_GRANTED,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val exportState: ExportState = ExportState.IDLE
)

class SettingsViewModel(
    private val repository: UsageRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // The DataStore Flow itself is expected to be well-behaved (it never
        // throws for a missing key, only for I/O corruption, which is rare
        // enough that a raw launch here - rather than safeLaunch - is fine;
        // this collector should just live for the ViewModel's lifetime.
        viewModelScope.launch {
            combine(
                settingsDataStore.themeMode,
                settingsDataStore.notificationsEnabled
            ) { theme, notificationsEnabled -> theme to notificationsEnabled }
                .collect { (theme, notificationsEnabled) ->
                    _uiState.value = _uiState.value.copy(
                        themeMode = theme,
                        notificationsEnabled = notificationsEnabled
                    )
                }
        }
    }

    fun refreshUsageAccess() {
        _uiState.value = _uiState.value.copy(usageAccessState = repository.usageAccessState())
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        safeLaunch { settingsDataStore.setThemeMode(mode) }
    }

    fun onNotificationsToggled(enabled: Boolean) {
        safeLaunch { settingsDataStore.setNotificationsEnabled(enabled) }
    }

    fun clearLocalData() {
        safeLaunch { repository.clearAllLocalData() }
    }

    /** [uri] comes from a CreateDocument picker result in the screen - the user already
     * chose where to save, this just writes the export JSON there. */
    fun exportData(uri: Uri) {
        _uiState.value = _uiState.value.copy(exportState = ExportState.EXPORTING)
        safeLaunch(onError = {
            _uiState.value = _uiState.value.copy(exportState = ExportState.FAILED)
        }) {
            val ok = repository.exportToUri(uri)
            _uiState.value = _uiState.value.copy(
                exportState = if (ok) ExportState.SUCCESS else ExportState.FAILED
            )
        }
    }

    fun clearExportStatus() {
        _uiState.value = _uiState.value.copy(exportState = ExportState.IDLE)
    }
}
