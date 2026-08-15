package com.royalshield.app.features.riskprediction.ui

import com.royalshield.app.features.riskprediction.data.remote.Hotspot
import com.royalshield.app.features.riskprediction.data.remote.HotspotCenter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskMapTruthfulnessTest {
    @Test
    fun providerFailureClearsUnverifiedHotspots() {
        val stale = Hotspot(
            hotspotId = 1,
            center = HotspotCenter(25.76, -80.19),
            h3Cell = "old",
            radiusMeters = 100.0,
            eventCount = 4,
            eventTypes = emptyMap(),
            severities = emptyMap(),
            riskScore = 0.8,
            riskLevel = "HIGH",
        )

        val state = unavailableRiskMapState(RiskMapUiState(hotspots = listOf(stale)), "STATUS_UNREACHABLE")

        assertTrue(state.hotspots.isEmpty())
        assertTrue(state.riskZones.isEmpty())
        assertEquals("STATUS_UNREACHABLE", state.error)
        assertEquals(0L, state.lastUpdate)
    }
}
