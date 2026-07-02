package com.royalshield.app.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * VPN Manager
 * Centralized VPN state management and lifecycle control
 */
class VpnManager(private val context: Context) {
    
    private val _connectionState = MutableStateFlow<VpnState>(VpnState.Disconnected)
    val connectionState: StateFlow<VpnState> = _connectionState.asStateFlow()
    
    private val _bytesTransferred = MutableStateFlow(VpnStats(0L, 0L))
    val bytesTransferred: StateFlow<VpnStats> = _bytesTransferred.asStateFlow()
    
    private val vpnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                RoyalVpnService.ACTION_VPN_CONNECTED -> {
                    _connectionState.value = VpnState.Connected
                    Log.i(TAG, "VPN state: Connected")
                }
                RoyalVpnService.ACTION_VPN_DISCONNECTED -> {
                    _connectionState.value = VpnState.Disconnected
                    Log.i(TAG, "VPN state: Disconnected")
                }
                RoyalVpnService.ACTION_VPN_MISSING_CONFIG -> {
                    _connectionState.value = VpnState.MissingConfig
                    Log.i(TAG, "VPN state: Missing Config")
                }
                RoyalVpnService.ACTION_VPN_ERROR -> {
                    val error = intent.getStringExtra("error") ?: "Unknown error"
                    _connectionState.value = VpnState.Error(error)
                    Log.e(TAG, "VPN error: $error")
                }
            }
        }
    }
    
    init {
        registerReceiver()
    }
    
    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(RoyalVpnService.ACTION_VPN_CONNECTED)
            addAction(RoyalVpnService.ACTION_VPN_DISCONNECTED)
            addAction(RoyalVpnService.ACTION_VPN_MISSING_CONFIG)
            addAction(RoyalVpnService.ACTION_VPN_ERROR)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(vpnReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(vpnReceiver, filter)
        }
    }
    
    /**
     * Connect to VPN
     * Returns Intent if VPN permission is required, null otherwise
     */
    fun connect(config: String?): Intent? {
        if (config.isNullOrBlank()) {
            _connectionState.value = VpnState.MissingConfig
            return null
        }

        val prepareIntent = VpnService.prepare(context)
        
        return if (prepareIntent != null) {
            // User needs to grant VPN permission
            _connectionState.value = VpnState.PermissionRequired(prepareIntent)
            prepareIntent
        } else {
            // Permission already granted, connect
            startVpnService(RoyalVpnService.ACTION_CONNECT, config)
            _connectionState.value = VpnState.Connecting
            null
        }
    }
    
    /**
     * Disconnect VPN
     */
    fun disconnect() {
        startVpnService(RoyalVpnService.ACTION_DISCONNECT)
        _connectionState.value = VpnState.Disconnecting
    }
    
    /**
     * Check if VPN is connected
     */
    fun isConnected(): Boolean {
        return _connectionState.value is VpnState.Connected
    }

    /**
     * Report Premium Required state
     */
    fun setPremiumRequired() {
        _connectionState.value = VpnState.PremiumRequired
    }
    
    private fun startVpnService(action: String, config: String? = null) {
        val intent = Intent(context, RoyalVpnService::class.java).apply {
            this.action = action
            config?.let { putExtra(RoyalVpnService.EXTRA_CONFIG, it) }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    fun cleanup() {
        try {
            context.unregisterReceiver(vpnReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receiver", e)
        }
    }
    
    companion object {
        private const val TAG = "VpnManager"
        
        @Volatile
        private var instance: VpnManager? = null
        
        fun getInstance(context: Context): VpnManager {
            return instance ?: synchronized(this) {
                instance ?: VpnManager(context.applicationContext).also { instance = it  }
            }
        }
    }
}

/**
 * VPN Connection States
 */
sealed class VpnState {
    object Disconnected : VpnState()
    object Connecting : VpnState()
    object Connected : VpnState()
    object Disconnecting : VpnState()
    object MissingConfig : VpnState()
    object PremiumRequired : VpnState()
    data class PermissionRequired(val intent: Intent) : VpnState()
    data class Error(val message: String) : VpnState()
}

/**
 * VPN Statistics
 */
data class VpnStats(
    val bytesReceived: Long,
    val bytesSent: Long
) {
    fun totalBytes() = bytesReceived + bytesSent
    
    fun toReadableString(): String {
        return "↓ ${formatBytes(bytesReceived)} ↑ ${formatBytes(bytesSent)}"
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
