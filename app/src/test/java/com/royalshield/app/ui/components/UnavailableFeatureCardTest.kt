package com.royalshield.app.ui.components

import com.royalshield.app.data.AvailabilityState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnavailableFeatureCardTest {
    @Test
    fun unavailableFeatureCannotExecuteProtectedCallback() {
        var executed = false

        val accepted = executeWhenReady(
            AvailabilityState.Unavailable("NO_E2E_EVIDENCE"),
        ) { executed = true }

        assertFalse(accepted)
        assertFalse(executed)
    }

    @Test
    fun readyFeatureCanExecuteProtectedCallback() {
        var executed = false
        val ready = AvailabilityState.Ready(
            dataFreshness = "2026-08-15T15:00:00Z",
            sources = listOf("e2e:vpn"),
            modelVersion = null,
            confidence = null,
        )

        val accepted = executeWhenReady(ready) { executed = true }

        assertTrue(accepted)
        assertTrue(executed)
    }

    @Test
    fun unverifiedAdvancedItemsAreHidden() {
        val state = AvailabilityState.Unavailable("STATUS_UNVERIFIED")

        assertTrue(verifiedItems(state, listOf("sample signal")).isEmpty())
    }
}
