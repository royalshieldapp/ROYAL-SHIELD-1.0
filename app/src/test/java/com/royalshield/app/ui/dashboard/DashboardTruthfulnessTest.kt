package com.royalshield.app.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardTruthfulnessTest {
    @Test
    fun releaseDashboardHasNoGeneratedThreatsOrActions() {
        val state = releaseSafeDashboardState()

        assertTrue(state.threatEvents.isEmpty())
        assertTrue(state.actionItems.isEmpty())
        assertTrue(state.connectors.isEmpty())
        assertTrue(state.timelineData.isEmpty())
        assertEquals(0, state.metrics.investigationsTotal)
    }
}
