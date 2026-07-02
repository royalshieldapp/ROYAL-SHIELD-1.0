package com.royalshield.app.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.royalshield.app.data.db.AppDatabase
import com.royalshield.app.data.db.ActionType
import com.royalshield.app.data.db.AutomationRule
import com.royalshield.app.data.db.TriggerType
import com.royalshield.app.vpn.VpnManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AutomationEngine {
    private const val TAG = "AutomationEngine"

    fun processEvent(context: Context, triggerType: TriggerType, params: String = "") {
        val database = AppDatabase.getDatabase(context)
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val rules = database.automationDao().getAllRules().first()
                val activeRules = rules.filter { it.isEnabled && it.triggerType == triggerType }

                activeRules.forEach { rule ->
                    Log.d(TAG, "Executing rule: ${rule.name}")
                    executeAction(context, rule)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing automation event", e)
            }
        }
    }

    private fun executeAction(context: Context, rule: AutomationRule) {
        Log.i(TAG, "Action ${rule.actionType} triggered by rule ${rule.name}")
        
        when (rule.actionType) {
            ActionType.TOGGLE_VPN -> {
                handleVpnToggle(context, rule)
            }
            else -> {
                // Other actions (Cameras, etc.) - implement as needed
                Log.d(TAG, "Action ${rule.actionType} not yet implemented")
            }
        }
    }
    
    private fun handleVpnToggle(context: Context, rule: AutomationRule) {
        val vpnManager = VpnManager.getInstance(context)
        
        // Check if on public WiFi
        if (isOnPublicWiFi(context)) {
            if (!vpnManager.isConnected()) {
                Log.i(TAG, "Public WiFi detected, auto-connecting VPN...")
                
                // Demo config - In production, fetch from backend API
                val demoConfig = """
                    [Interface]
                    Address = 10.8.0.2/32
                    DNS = 1.1.1.1
                    
                    [Peer]
                    PublicKey = YOUR_SERVER_PUBLIC_KEY_HERE
                    Endpoint = YOUR_SERVER_IP:51820
                    AllowedIPs = 0.0.0.0/0
                    PersistentKeepalive = 25
                """.trimIndent()
                
                vpnManager.connect(demoConfig)
            }
        } else {
            // On trusted network, can disconnect if desired
            Log.i(TAG, "On trusted network, VPN auto-connect not needed")
        }
    }
    
    private fun isOnPublicWiFi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        // Check if on WiFi
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return false
        }
        
        // In a production app, you'd check:
        // 1. SSID against a whitelist of trusted networks
        // 2. Network encryption type (WPA2/WPA3 vs open)
        // 3. Captive portal detection
        
        // For now, assume all WiFi is potentially public
        // User can configure trusted networks in settings
        return true
    }
}
