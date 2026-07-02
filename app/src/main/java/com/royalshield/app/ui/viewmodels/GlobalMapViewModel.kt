package com.royalshield.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.data.GlobalIncidentRepository
import com.royalshield.app.data.Incident
import com.royalshield.app.data.MapReminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GlobalMapViewModel : ViewModel() {

    private val _incidents = MutableStateFlow<List<Incident>>(emptyList())
    val incidents: StateFlow<List<Incident>> = _incidents.asStateFlow()

    private val _reminders = MutableStateFlow<List<MapReminder>>(emptyList())
    val reminders: StateFlow<List<MapReminder>> = _reminders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch in parallel in a real app, strict sequential here for simplicity
                _incidents.value = GlobalIncidentRepository.getIncidents()
                _reminders.value = GlobalIncidentRepository.getReminders()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refresh() {
        loadData()
    }
    
    fun addReminder(name: String, address: String, lat: Double, lon: Double) {
        val currentList = _reminders.value.toMutableList()
        val newId = (currentList.maxOfOrNull { it.id } ?: 0) + 1
        currentList.add(MapReminder(newId, name, address, true, lat, lon))
        _reminders.value = currentList
    }
}
