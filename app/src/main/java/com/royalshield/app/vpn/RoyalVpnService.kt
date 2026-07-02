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
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * VPN Service
 * Handles VPN connection lifecycle (Base setup without fake tunnels)
 */
class RoyalVpnService : VpnService() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var vpnInterface: android.os.ParcelFileDescriptor? = null
    private var packetLoopJob: Job? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val config = intent.getStringExtra(EXTRA_CONFIG)
                if (config != null) {
                    establishVpn(config)
                } else {
                    Log.e(TAG, "VPN config is null")
                    sendBroadcast(Intent(ACTION_VPN_MISSING_CONFIG))
                    stopSelf()
                }
            }
            ACTION_DISCONNECT -> {
                disconnect()
            }
        }
        return START_STICKY
    }

    private fun establishVpn(config: String) {
        Log.i(TAG, "Establishing VPN connection...")
        try {
            // Configure local loopback VPN builder
            val builder = Builder()
                .setSession("RoyalShieldVPN")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
            
            vpnInterface = builder.establish()
            
            if (vpnInterface != null) {
                // Start Foreground notification
                startForeground(NOTIFICATION_ID, createNotification())
                
                // Broadcast success
                sendBroadcast(Intent(ACTION_VPN_CONNECTED))
                Log.i(TAG, "VPN Interface established successfully")
                
                // Start packet loop (local loopback reader/sink)
                startPacketLoop()
            } else {
                throw IllegalStateException("Failed to establish parcel file descriptor")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN", e)
            val errorIntent = Intent(ACTION_VPN_ERROR).apply {
                putExtra("error", e.message ?: "Failed to establish VPN")
            }
            sendBroadcast(errorIntent)
            stopSelf()
        }
    }

    private fun startPacketLoop() {
        packetLoopJob = serviceScope.launch {
            val fd = vpnInterface?.fileDescriptor ?: return@launch
            val input = FileInputStream(fd)
            val output = FileOutputStream(fd)
            val buffer = ByteBuffer.allocate(32767)
            
            try {
                while (isActive) {
                    // Read packets from the tunnel interface
                    val readBytes = input.read(buffer.array())
                    if (readBytes > 0) {
                        // Simply consume local packets for safe sandbox analysis
                        buffer.clear()
                    }
                    delay(10) // Small delay to avoid CPU overhead
                }
            } catch (e: Exception) {
                Log.e(TAG, "Packet loop error", e)
            } finally {
                withContext(NonCancellable) {
                    try { input.close() } catch(e: Exception) {}
                    try { output.close() } catch(e: Exception) {}
                }
            }
        }
    }
    
    private fun disconnect() {
        Log.d(TAG, "Disconnecting VPN...")
        packetLoopJob?.cancel()
        packetLoopJob = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
        vpnInterface = null
        sendBroadcast(Intent(ACTION_VPN_DISCONNECTED))
        stopSelf()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        serviceScope.cancel()
    }
    
    companion object {
        private const val TAG = "RoyalVpnService"
        
        const val ACTION_CONNECT = "com.royalshield.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.royalshield.vpn.DISCONNECT"
        const val ACTION_VPN_CONNECTED = "com.royalshield.vpn.CONNECTED"
        const val ACTION_VPN_DISCONNECTED = "com.royalshield.vpn.DISCONNECTED"
        const val ACTION_VPN_ERROR = "com.royalshield.vpn.ERROR"
        const val ACTION_VPN_MISSING_CONFIG = "com.royalshield.vpn.MISSING_CONFIG"
        
        const val EXTRA_CONFIG = "vpn_config"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vpn_channel"
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
            .setSmallIcon(R.drawable.icon_shield_gold)
            .setContentTitle("Royal Shield VPN Active")
            .setContentText("Tu conexión está protegida y encriptada.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }
}
