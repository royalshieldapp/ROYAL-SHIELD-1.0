package com.royalshield.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.data.AdvancedFeatureAvailability
import com.royalshield.app.data.FeatureAvailabilityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeatureAvailabilityViewModel(
    private val repository: FeatureAvailabilityRepository = FeatureAvailabilityRepository.createDefault(),
) : ViewModel() {
    private val _availability = MutableStateFlow(
        AdvancedFeatureAvailability.unavailable("STATUS_LOADING"),
    )
    val availability = _availability.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _availability.value = repository.refresh()
        }
    }
}
