package com.royalshield.app.features.intel.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.features.intel.data.IntelRepository
import com.royalshield.app.features.intel.models.IntelAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class IntelAlertDetailUiState {
    object Loading : IntelAlertDetailUiState()
    data class Content(val alert: IntelAlert) : IntelAlertDetailUiState()
    data class Error(val message: String) : IntelAlertDetailUiState()
}

class IntelAlertDetailViewModel(
    private val alertId: String,
    private val repository: IntelRepository = IntelRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<IntelAlertDetailUiState>(IntelAlertDetailUiState.Loading)
    val uiState: StateFlow<IntelAlertDetailUiState> = _uiState.asStateFlow()

    init {
        loadAlert()
    }

    private fun loadAlert() {
        viewModelScope.launch {
            try {
                val alert = repository.getAlert(alertId)
                if (alert != null) {
                    _uiState.value = IntelAlertDetailUiState.Content(alert)
                } else {
                    _uiState.value = IntelAlertDetailUiState.Error("Alert not found")
                }
            } catch (e: Exception) {
                _uiState.value = IntelAlertDetailUiState.Error(e.message ?: "Failed to load alert")
            }
        }
    }
}
