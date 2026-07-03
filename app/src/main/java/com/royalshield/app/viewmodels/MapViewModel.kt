package com.royalshield.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.AlienVaultRepository
import com.royalshield.app.models.ThreatAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Threat Map features.
 */
class MapViewModel : ViewModel() {
    private val repository = AlienVaultRepository()

    private val _threats = MutableStateFlow<List<ThreatAlert>>(emptyList())
    val threats: StateFlow<List<ThreatAlert>> = _threats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refreshThreats()
    }

    fun refreshThreats(lat: Double? = null, lng: Double? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _threats.value = repository.getRecentThreats(lat, lng)
            } catch (e: Exception) {
                // Keep existing or handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}
