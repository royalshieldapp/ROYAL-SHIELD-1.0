package com.royalshield.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.royalshield.app.R
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

/**
 * Orchestrates the real WireGuard userspace backend.
 * No local packet sink or fake tunnel is used here.
 */
class RoyalVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val backend by lazy { GoBackend(applicationContext) }
    private val tunnel = object : Tunnel {
        override fun getName(): String = "RoyalShieldVPN"

        override fun onStateChange(newState: Tunnel.State) {
            Log.i(TAG, "WireGuard tunnel state: $newState")
        }
    }
    private var statsJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val config = intent.getStringExtra(EXTRA_CONFIG)
                if (config.isNullOrBlank()) {
                    Log.e(TAG, "VPN config is missing")
                    sendBroadcast(Intent(ACTION_VPN_MISSING_CONFIG))
                    stopSelf()
                } else {
                    establishVpn(config)
                }
            }
            ACTION_DISCONNECT -> disconnect()
        }
        return START_STICKY
    }

    private fun establishVpn(configText: String) {
        serviceScope.launch {
            try {
                Log.i(TAG, "Starting WireGuard VPN connection...")
                val config = Config.parse(ByteArrayInputStream(configText.toByteArray(Charsets.UTF_8)))

                withContext(Dispatchers.Main) {
                    startForeground(NOTIFICATION_ID, createNotification())
                }

                val state = backend.setState(tunnel, Tunnel.State.UP, config)
                if (state == Tunnel.State.UP) {
                    sendBroadcast(Intent(ACTION_VPN_CONNECTED))
                    startStatsLoop()
                    Log.i(TAG, "WireGuard VPN started successfully")
                } else {
                    throw IllegalStateException("WireGuard backend returned state $state")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting WireGuard VPN", e)
                sendBroadcast(Intent(ACTION_VPN_ERROR).apply {
                    putExtra("error", e.message ?: "Failed to establish WireGuard VPN")
                })
                stopSelf()
            }
        }
    }

    private fun startStatsLoop() {
        statsJob?.cancel()
        statsJob = serviceScope.launch {
            while (isActive) {
                try {
                    val stats = backend.getStatistics(tunnel)
                    sendBroadcast(Intent(ACTION_VPN_STATS).apply {
                        putExtra(EXTRA_BYTES_RECEIVED, stats.totalRx())
                        putExtra(EXTRA_BYTES_SENT, stats.totalTx())
                    })
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to read VPN stats", e)
                }
                delay(1000)
            }
        }
    }

    private fun disconnect() {
        serviceScope.launch {
            Log.d(TAG, "Disconnecting WireGuard VPN...")
            statsJob?.cancel()
            statsJob = null
            try {
                backend.setState(tunnel, Tunnel.State.DOWN, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping WireGuard VPN", e)
            } finally {
                withContext(NonCancellable) {
                    sendBroadcast(Intent(ACTION_VPN_DISCONNECTED))
                    stopSelf()
                }
            }
        }
    }

    override fun onDestroy() {
        statsJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Royal VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status of the encrypted connection"
                enableLights(true)
                lightColor = Color.YELLOW
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.img_icon_shield_gold)
            .setContentTitle("Royal Shield VPN Active")
            .setContentText("Your WireGuard tunnel is active.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "RoyalVpnService"

        const val ACTION_CONNECT = "com.royalshield.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.royalshield.vpn.DISCONNECT"
        const val ACTION_VPN_CONNECTED = "com.royalshield.vpn.CONNECTED"
        const val ACTION_VPN_DISCONNECTED = "com.royalshield.vpn.DISCONNECTED"
        const val ACTION_VPN_ERROR = "com.royalshield.vpn.ERROR"
        const val ACTION_VPN_MISSING_CONFIG = "com.royalshield.vpn.MISSING_CONFIG"
        const val ACTION_VPN_STATS = "com.royalshield.vpn.STATS"

        const val EXTRA_CONFIG = "vpn_config"
        const val EXTRA_BYTES_RECEIVED = "bytes_received"
        const val EXTRA_BYTES_SENT = "bytes_sent"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vpn_channel"
    }
}
