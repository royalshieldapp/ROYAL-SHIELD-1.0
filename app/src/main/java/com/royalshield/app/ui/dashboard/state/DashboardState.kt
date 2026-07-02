package com.royalshield.app.ui.dashboard.state

import com.royalshield.app.ui.dashboard.models.ActionItem
import com.royalshield.app.ui.dashboard.models.Connector
import com.royalshield.app.ui.dashboard.models.KpiMetrics
import com.royalshield.app.ui.dashboard.models.ThreatEvent

data class DashboardState(
    val metrics: KpiMetrics = KpiMetrics(0, 0, 0, 0, 0, 0),
    val threatEvents: List<ThreatEvent> = emptyList(),
    val actionItems: List<ActionItem> = emptyList(),
    val connectors: List<Connector> = emptyList(),
    val timelineData: List<Float> = emptyList(), // Normalized heights 0..1
    val selectedRecentsFilter: String = "24h",   // 1Day, 1Week, 1Month
    val selectedThreatId: String? = null,
    val isLoading: Boolean = false
)
