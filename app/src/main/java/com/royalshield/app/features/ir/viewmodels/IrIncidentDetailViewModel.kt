package com.royalshield.app.features.ir.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.features.ir.data.IrRepository
import com.royalshield.app.features.ir.models.ChecklistItem
import com.royalshield.app.features.ir.models.Incident
import com.royalshield.app.features.ir.models.TimelineEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed class IrDetailUiState {
    object Loading : IrDetailUiState()
    data class Content(
        val incident: Incident,
        val timeline: List<TimelineEvent>,
        val checklist: List<ChecklistItem>
    ) : IrDetailUiState()
    data class Error(val message: String) : IrDetailUiState()
}

class IrIncidentDetailViewModel(
    private val incidentId: String,
    private val repository: IrRepository = IrRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<IrDetailUiState>(IrDetailUiState.Loading)
    val uiState: StateFlow<IrDetailUiState> = _uiState.asStateFlow()

    init {
        loadIncidentDetails()
    }

    private fun loadIncidentDetails() {
        viewModelScope.launch {
            try {
                val incident = repository.getIncident(incidentId)
                if (incident == null) {
                    _uiState.value = IrDetailUiState.Error("Incident not found")
                    return@launch
                }

                combine(
                    repository.getTimeline(incidentId),
                    repository.getChecklist(incidentId)
                ) { timeline, checklist ->
                    IrDetailUiState.Content(incident, timeline, checklist)
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = IrDetailUiState.Error(e.message ?: "Failed to load incident")
            }
        }
    }

    fun addTimelineEvent(actionTaken: String, who: String) {
        viewModelScope.launch {
            repository.addTimelineEvent(incidentId, TimelineEvent(actionTaken = actionTaken, who = who))
        }
    }

    fun toggleChecklistItem(itemId: String, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleChecklistItem(incidentId, itemId, completed)
        }
    }
}
