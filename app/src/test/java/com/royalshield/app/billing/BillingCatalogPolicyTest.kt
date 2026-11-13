package com.royalshield.app.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingCatalogPolicyTest {

    @Test
    fun disconnected_client_waits_instead_of_presenting_local_prices() {
        val result = BillingCatalogPolicy.evaluate(
            isBillingReady = false,
            returnedProductIds = emptySet(),
        )

        assertEquals(BillingCatalogState.WaitingForPlay, result)
    }

    @Test
    fun empty_play_catalog_is_reported_as_unavailable() {
        val result = BillingCatalogPolicy.evaluate(
            isBillingReady = true,
            returnedProductIds = emptySet(),
        )

        assertTrue(result is BillingCatalogState.Unavailable)
        assertTrue((result as BillingCatalogState.Unavailable).message.contains("Google Play"))
    }

    @Test
    fun catalog_is_ready_only_when_every_required_plan_is_returned_by_play() {
        val result = BillingCatalogPolicy.evaluate(
            isBillingReady = true,
            returnedProductIds = setOf(
                "lifetime_starter",
                "lifetime_gold",
                "lifetime_ultimate",
            ),
        )

        assertEquals(BillingCatalogState.Ready, result)
    }

    @Test
    fun partial_catalog_identifies_missing_play_products() {
        val result = BillingCatalogPolicy.evaluate(
            isBillingReady = true,
            returnedProductIds = setOf("lifetime_starter"),
        )

        assertTrue(result is BillingCatalogState.Unavailable)
        val message = (result as BillingCatalogState.Unavailable).message
        assertTrue(message.contains("lifetime_gold"))
        assertTrue(message.contains("lifetime_ultimate"))
    }
}
