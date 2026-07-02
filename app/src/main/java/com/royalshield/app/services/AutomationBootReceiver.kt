package com.royalshield.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * BroadcastReceiver que inicia el servicio de Automation Laws
 * cuando el dispositivo se enciende o la app se actualiza
 */
class AutomationBootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Verificar si hay Laws activas antes de iniciar el servicio
                val lawManager = AutomationLawManager(context)
                val activeLaws = lawManager.getActiveLaws()
                
                if (activeLaws.isNotEmpty()) {
                    // Iniciar el servicio de automatización
                    AutomationService.start(context)
                }
            }
        }
    }
}

// Helper movido a su propio archivo AutomationServiceHelper.kt
