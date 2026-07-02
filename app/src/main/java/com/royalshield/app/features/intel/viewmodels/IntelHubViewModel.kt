package com.royalshield.app.features.intel.viewmodels

import com.royalshield.app.managers.BreachCheckManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.features.intel.data.IntelRepository
import com.royalshield.app.features.intel.data.SettingsRepository
import com.royalshield.app.features.intel.models.AppSettings
import com.royalshield.app.features.intel.models.IOC
import com.royalshield.app.features.intel.models.IntelAlert
import com.royalshield.app.features.intel.models.Monitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed class IntelHubUiState {
    object Loading : IntelHubUiState()
    data class Content(
        val monitors: List<Monitor>,
        val iocs: List<IOC>,
        val alerts: List<IntelAlert>,
        val settings: AppSettings
    ) : IntelHubUiState()
    data class Error(val message: String) : IntelHubUiState()
}

class IntelHubViewModel(
    private val intelRepository: IntelRepository = IntelRepository(),
    private val settingsRepository: SettingsRepository = SettingsRepository(),
    private val breachCheckManager: BreachCheckManager = BreachCheckManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow<IntelHubUiState>(IntelHubUiState.Loading)
    val uiState: StateFlow<IntelHubUiState> = _uiState.asStateFlow()

    init {
        loadIntelData()
    }

    private fun loadIntelData() {
        viewModelScope.launch {
            combine(
                intelRepository.getMonitors(),
                intelRepository.getIocs(),
                intelRepository.getAlerts(),
                settingsRepository.getAppSettings()
            ) { monitors, iocs, alerts, settings ->
                IntelHubUiState.Content(monitors, iocs, alerts, settings)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun addMonitor(value: String, type: String) {
        viewModelScope.launch {
            intelRepository.addMonitor(Monitor(value = value, type = type))
            
            // Run dark web breach check immediately
            val foundAlerts = breachCheckManager.runBreachCheck(value)
            foundAlerts.forEach { alert ->
                intelRepository.createAlert(alert)
            }
        }
    }

    fun addIoc(value: String, type: com.royalshield.app.features.intel.models.IocType) {
        viewModelScope.launch {
            intelRepository.addIoc(IOC(value = value, type = type))
        }
    }

    fun updateComplianceMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateComplianceMode(enabled)
        }
    }
}
