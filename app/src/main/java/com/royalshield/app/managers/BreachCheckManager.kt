package com.royalshield.app.managers

import com.royalshield.app.features.intel.models.AlertCategory
import com.royalshield.app.features.intel.models.IntelAlert
import kotlinx.coroutines.delay

/**
 * Manager to simulate checking for email/domain breaches on the Dark Web.
 */
class BreachCheckManager {
    
    /**
     * Simulates a breach check for a given value (email or domain).
     * Returns a list of alerts if breaches are "found".
     */
    suspend fun runBreachCheck(value: String): List<IntelAlert> {
        // Simulate a network delay for a premium experience
        delay(2500)
        
        val alerts = mutableListOf<IntelAlert>()
        
        // Realistic simulation based on common test strings
        val lowerValue = value.lowercase()
        
        if (lowerValue.contains("leak") || lowerValue.contains("pwned") || lowerValue.contains("scam")) {
            alerts.add(
                IntelAlert(
                    title = "CRITICAL: DATA LEAK DETECTED",
                    category = AlertCategory.CREDENTIAL_LEAK,
                    severity = "CRITICAL",
                    description = "The address '$value' was found in a 2024 database leak. Plaintext passwords and personal metadata may be exposed.",
                    recommendedActions = listOf(
                        "Change your primary password immediately.",
                        "Activate Multi-Factor Authentication (MFA).",
                        "Audit your recent bank statements."
                    )
                )
            )
        } else if (lowerValue.contains("gmail.com") || lowerValue.contains("outlook.com")) {
            // Randomly simulate a medium alert for common domains
            if ((1..10).random() > 7) {
                alerts.add(
                    IntelAlert(
                        title = "POTENTIAL EXPOSURE",
                        category = AlertCategory.OTHER,
                        severity = "MEDIUM",
                        description = "Your provider has reported minor security incidents. Minor metadata exposure possible.",
                        recommendedActions = listOf("Monitor your login activity.")
                    )
                )
            }
        }
        
        return alerts
    }
}
