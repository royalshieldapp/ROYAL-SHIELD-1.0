package com.royalshield.app.managers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyAdvisorManagerTest {

    @Test
    fun riskLevel_enum_hasExpectedHierarchy() {
        assertEquals(RiskLevel.HIGH, RiskLevel.valueOf("HIGH"))
        assertEquals(RiskLevel.MEDIUM, RiskLevel.valueOf("MEDIUM"))
        assertEquals(RiskLevel.LOW, RiskLevel.valueOf("LOW"))
        
        // Sorting check: High risk should come before Medium and Low
        val list = listOf(RiskLevel.LOW, RiskLevel.HIGH, RiskLevel.MEDIUM)
        val sorted = list.sorted()
        assertEquals(listOf(RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.LOW), sorted)
    }

    @Test
    fun privacyApp_model_holdsExpectedValues() {
        val app = PrivacyApp(
            packageName = "com.suspicious.app",
            appName = "Suspicious Utility",
            icon = null,
            permissions = listOf("CAMERA", "RECORD_AUDIO", "ACCESS_FINE_LOCATION"),
            riskLevel = RiskLevel.HIGH
        )

        assertEquals("com.suspicious.app", app.packageName)
        assertEquals("Suspicious Utility", app.appName)
        assertEquals(3, app.permissions.size)
        assertTrue(app.permissions.contains("CAMERA"))
        assertEquals(RiskLevel.HIGH, app.riskLevel)
    }

    @Test
    fun privacyApp_sortingByRiskLevel_placesHighRiskFirst() {
        val appHigh = PrivacyApp("com.app.high", "High App", null, listOf("CAMERA"), RiskLevel.HIGH)
        val appMedium = PrivacyApp("com.app.med", "Med App", null, listOf("ACCESS_FINE_LOCATION", "READ_CONTACTS", "POST_NOTIFICATIONS"), RiskLevel.MEDIUM)
        val appLow = PrivacyApp("com.app.low", "Low App", null, listOf("POST_NOTIFICATIONS"), RiskLevel.LOW)

        val unsorted = listOf(appLow, appHigh, appMedium)
        val sorted = unsorted.sortedBy { it.riskLevel }

        assertEquals("com.app.high", sorted[0].packageName)
        assertEquals("com.app.med", sorted[1].packageName)
        assertEquals("com.app.low", sorted[2].packageName)
    }
}

