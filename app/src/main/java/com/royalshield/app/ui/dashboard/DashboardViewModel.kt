package com.royalshield.app.ui.dashboard

import androidx.lifecycle.ViewModel
import com.royalshield.app.ui.dashboard.state.DashboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

fun releaseSafeDashboardState(): DashboardState = DashboardState()

class DashboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(releaseSafeDashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun onThreatSelected(threatId: String?) {
        _state.update { it.copy(selectedThreatId = threatId) }
    }

    fun onTimeFilterSelected(filter: String) {
        _state.update { it.copy(selectedRecentsFilter = filter) }
    }
}
