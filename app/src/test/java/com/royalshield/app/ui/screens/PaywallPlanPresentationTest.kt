package com.royalshield.app.ui.screens

import com.royalshield.app.managers.BillingManager
import com.royalshield.app.models.ProductInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaywallPlanPresentationTest {

    private fun product(id: String, price: String) = ProductInfo(
        id = id,
        title = id,
        description = "from Play",
        price = price,
    )

    @Test
    fun maps_complete_catalog_in_reference_order_without_changing_prices() {
        val plans = toPaywallPlanPresentations(
            listOf(
                product(BillingManager.PRODUCT_ULTIMATE, "US$39.49"),
                product(BillingManager.PRODUCT_STARTER, "US$9.99"),
                product(BillingManager.PRODUCT_GOLD, "US$19.99"),
            ),
        )

        assertEquals(listOf("STARTER", "GOLDEN", "ULTIMATE"), plans.map { it.title })
        assertEquals(listOf("US$9.99", "US$19.99", "US$39.49"), plans.map { it.formattedPrice })
        assertTrue(plans.all { it.purchaseLabel == "one-time purchase" })
        assertEquals(listOf(false, true, false), plans.map { it.mostPopular })
    }

    @Test
    fun incomplete_catalog_never_invents_missing_plans() {
        val plans = toPaywallPlanPresentations(
            listOf(product(BillingManager.PRODUCT_STARTER, "CA$12.99")),
        )

        assertEquals(listOf(BillingManager.PRODUCT_STARTER), plans.map { it.productId })
        assertEquals("CA$12.99", plans.single().formattedPrice)
    }

    @Test
    fun continue_requires_real_selected_plan_and_non_purchasing_state() {
        val plans = toPaywallPlanPresentations(
            listOf(product(BillingManager.PRODUCT_GOLD, "€19.99")),
        )

        assertTrue(isPaywallContinueEnabled(BillingManager.PRODUCT_GOLD, false, plans))
        assertFalse(isPaywallContinueEnabled(BillingManager.PRODUCT_GOLD, true, plans))
        assertFalse(isPaywallContinueEnabled("missing", false, plans))
        assertFalse(isPaywallContinueEnabled(null, false, plans))
    }
}
