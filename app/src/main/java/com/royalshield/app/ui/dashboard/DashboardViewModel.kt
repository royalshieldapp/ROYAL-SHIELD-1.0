package com.royalshield.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.ui.dashboard.models.*
import com.royalshield.app.ui.dashboard.state.DashboardState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class DashboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            // Simulate network delay
            delay(500)

            val mockMetrics = KpiMetrics(
                investigationsTotal = 55,
                lowSeverity = 5,
                mediumSeverity = 10,
                highSeverity = 40,
                exposedEntities = 152,
                entitiesDelta = 4
            )

            val mockConnectors = listOf(
                Connector("Microsoft Defender", true),
                Connector("Azure AD", true),
                Connector("AWS CloudTrail", false)
            )

            // Generate random threat events for Radar
            val mockThreats = List(20) {
                val severity = when (Random.nextInt(0, 10)) {
                    in 0..4 -> Severity.LOW
                    in 5..7 -> Severity.MEDIUM
                    else -> Severity.HIGH
                }
                ThreatEvent(
                    id = "evt_$it",
                    title = "Suspicious Activity $it",
                    severity = severity,
                    angleDeg = Random.nextFloat() * 360f,
                    radius = Random.nextFloat() * 0.8f + 0.1f, // 0.1 to 0.9
                    timestamp = System.currentTimeMillis() - Random.nextLong(0L, 86400000L)
                )
            }

            // Generate Action Items
            val mockActions = List(15) {
                ActionItem(
                    id = "act_$it",
                    title = "Investigate Endpoint User-$it",
                    description = "Abnormal login pattern detected on device.",
                    severity = if (it % 3 == 0) Severity.HIGH else Severity.MEDIUM,
                    status = "Open"
                )
            }

            // Generate timeline bars (normalized 0..1)
            val mockTimeline = List(24) { Random.nextFloat() }

            _state.update {
                it.copy(
                    isLoading = false,
                    metrics = mockMetrics,
                    connectors = mockConnectors,
                    threatEvents = mockThreats,
                    actionItems = mockActions,
                    timelineData = mockTimeline
                )
            }
        }
    }

    fun onThreatSelected(threatId: String?) {
        _state.update { it.copy(selectedThreatId = threatId) }
    }

    fun onTimeFilterSelected(filter: String) {
        _state.update { it.copy(selectedRecentsFilter = filter) }
        // In a real app, we would reload data here
    }
}
