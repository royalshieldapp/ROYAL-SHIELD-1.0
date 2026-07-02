package com.royalshield.app.services

import android.content.Intent
import android.provider.Settings
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.royalshield.app.managers.AnalyticsManager

/**
 * Service to detect AI Voice Scams using Call Screening API.
 * This service is triggered on every incoming call.
 */
class VoiceScamDetectionService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle.schemeSpecificPart
        Log.d("VoiceScamService", "Screening call from: $phoneNumber")

        // 1. Analyze risk (Simulation)
        val riskScore = calculateInitialRisk(phoneNumber)
        
        // 2. Track event
        AnalyticsManager.logEvent("call_screened")
        if (riskScore > 70) {
            AnalyticsManager.logEvent("high_risk_call_detected")
            
            // Check for overlay permission before starting
            if (Settings.canDrawOverlays(this)) {
                val intent = Intent(this, ScamOverlayService::class.java).apply {
                    putExtra("phone_number", phoneNumber)
                    putExtra("risk_score", riskScore)
                }
                startService(intent)
            }
            
            Log.w("VoiceScamService", "CRITICAL RISK: Potential AI Voice Scam detected for $phoneNumber")
        }

        // 3. Decide action
        val response = CallResponse.Builder()
            .setDisallowCall(false) // Let user decide after seeing overlay
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)
    }

    private fun calculateInitialRisk(phoneNumber: String): Int {
        // Enhanced mock logic
        return when {
            phoneNumber.startsWith("+") && phoneNumber.length > 13 -> 95 // Unusual international length
            phoneNumber.contains("400") || phoneNumber.contains("800") -> 65 // Common toll-free formats
            phoneNumber.length < 7 -> 90 // Spoofed short numbers
            else -> (10..40).random() // Random noise for baseline
        }
    }
}
