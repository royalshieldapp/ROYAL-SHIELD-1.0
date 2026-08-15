package com.royalshield.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedContentTruthfulnessTest {
    @Test
    fun reportsStartEmptyWithoutPersistedEvidence() {
        assertTrue(defaultSecurityReports().isEmpty())
    }

    @Test
    fun policiesStartEmptyWithoutUserConfiguration() {
        assertTrue(defaultSecurityPolicies().isEmpty())
    }

    @Test
    fun reportFileCandidatesContainOnlyDiscoveredFiles() {
        val discovered = listOf("C:/real/report.pdf", "C:/real/report.pdf")

        assertEquals(listOf("C:/real/report.pdf"), reportFileCandidates(discovered))
        assertTrue(reportFileCandidates(emptyList()).isEmpty())
    }

    @Test
    fun legacySampleReportsAreNotRestoredFromPreferences() {
        val legacy = SecurityReport("1", "Security Scan Report", "Aug 15, 2026", ReportType.SECURITY_SCAN)
        val userReport = SecurityReport("user-7", "My report", "Aug 15, 2026", ReportType.SECURITY_SCAN)

        assertEquals(listOf(userReport), removeLegacySampleReports(listOf(legacy, userReport)))
    }

    @Test
    fun legacySamplePoliciesAreNotRestoredFromPreferences() {
        val legacy = SecurityPolicy(
            id = "2",
            name = "Auto-Enable VPN on Public WiFi",
            description = "legacy",
            scope = PolicyScope.ALL_DEVICES,
            isEnabled = true,
            affectedDevices = 45,
            rules = emptyList(),
        )
        val userPolicy = legacy.copy(id = "user-9", name = "My local draft")

        assertEquals(listOf(userPolicy), removeLegacySamplePolicies(listOf(legacy, userPolicy)))
    }
}
