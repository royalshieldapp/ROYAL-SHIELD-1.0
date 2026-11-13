package com.royalshield.app.features.trackingshield

import com.royalshield.app.features.riskprediction.data.remote.Hotspot
import com.royalshield.app.features.riskprediction.data.remote.HotspotCenter
import com.royalshield.app.features.trackingshield.data.TrackingUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingForecastTruthfulnessTest {
    @Test
    fun unavailableForecastClearsStaleHotspotsAndConnectionState() {
        val stale = Hotspot(1, HotspotCenter(25.76, -80.19), "stale", 100.0, 1, emptyMap(), emptyMap(), 50.0, "HIGH")

        val result = unavailableTrackingForecastState(TrackingUiState(hotspots = listOf(stale)))

        assertTrue(result.hotspots.isEmpty())
        assertFalse(result.isForecastAvailable)
    }
}
