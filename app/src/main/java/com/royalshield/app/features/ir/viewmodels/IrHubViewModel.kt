package com.royalshield.app.features.ir.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.features.ir.data.IrRepository
import com.royalshield.app.features.ir.models.Asset
import com.royalshield.app.features.ir.models.Incident
import com.royalshield.app.features.ir.models.IncidentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class IrHubUiState {
    object Loading : IrHubUiState()
    data class Content(val incidents: List<Incident>, val assets: List<Asset> = emptyList()) : IrHubUiState()
    data class Error(val message: String) : IrHubUiState()
}

class IrHubViewModel(private val repository: IrRepository = IrRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow<IrHubUiState>(IrHubUiState.Loading)
    val uiState: StateFlow<IrHubUiState> = _uiState.asStateFlow()

    init {
        loadIncidents()
    }

    private fun loadIncidents() {
        viewModelScope.launch {
            repository.getIncidents().collect { incidents ->
                _uiState.value = IrHubUiState.Content(incidents)
            }
        }
    }

    fun startIncident(title: String, type: IncidentType, affectedAssets: String, notes: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val incident = Incident(
                    title = title,
                    type = type,
                    affectedAssets = affectedAssets,
                    notes = notes
                )
                val id = repository.createIncident(incident)
                onCreated(id)
            } catch (e: Exception) {
                _uiState.value = IrHubUiState.Error(e.message ?: "Failed to create incident")
            }
        }
    }
}
