package com.royalshield.app.services

import android.content.Context
import android.util.Log

/**
 * Helper to manage the automation service lifecycle
 */
object AutomationServiceHelper {

    /**
     * Starts the service if there are enabled laws
     */
    fun startIfNeeded(context: Context) {
        val lawManager = AutomationLawManager(context)
        val activeLaws = lawManager.getActiveLaws()
        
        if (activeLaws.isNotEmpty()) {
            AutomationService.start(context)
        }
    }

    /**
     * Restarts the service to apply changes to the laws
     */
    fun restart(context: Context) {
        // Stop current service
        AutomationService.stop(context)
        
        // Start again if necessary
        startIfNeeded(context)
    }
}
