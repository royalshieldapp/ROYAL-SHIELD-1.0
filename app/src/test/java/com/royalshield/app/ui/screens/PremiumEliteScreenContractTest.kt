package com.royalshield.app.ui.screens

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumEliteScreenContractTest {

    private fun projectFile(relativePath: String): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (!File(current, "settings.gradle.kts").isFile) {
            current = requireNotNull(current.parentFile) { "Project root not found" }
        }
        return File(current, relativePath)
    }

    @Test
    fun view_model_does_not_calculate_a_display_only_discount() {
        val source = projectFile(
            "app/src/main/java/com/royalshield/app/ui/viewmodels/PaywallViewModel.kt",
        ).readText()

        assertFalse(source.contains("applyDiscountToPriceString"))
        assertFalse(source.contains("originalPrice * 0.90"))
    }

    @Test
    fun paywall_uses_reference_hierarchy_without_monthly_or_fallback_prices() {
        val source = projectFile(
            "app/src/main/java/com/royalshield/app/ui/screens/LifetimePlansScreen.kt",
        ).readText()

        assertTrue(source.contains("CHOOSE YOUR PROTECTION"))
        assertTrue(source.contains("Select the plan that fits your security needs."))
        assertTrue(source.contains("Restore purchases"))
        assertTrue(source.contains("Continue with Limited Features"))
        assertFalse(source.contains("/month"))
        assertFalse(Regex("\"\\$\\d+\\.\\d{2}\"").containsMatchIn(source))
    }
}
