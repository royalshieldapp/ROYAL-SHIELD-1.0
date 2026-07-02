package com.royalshield.app.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.net.wifi.WifiManager
import android.os.*
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.royalshield.app.R
import java.util.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority


/**
 * Background service that executes Automation Laws
 * Similar to Fing's automation system
 */
class AutomationService : Service() {
    
    private lateinit var lawManager: AutomationLawManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    
    // Receivers
    private var batteryReceiver: BroadcastReceiver? = null
    private var wifiReceiver: BroadcastReceiver? = null
    
    // Timers
    private var timeCheckTimer: Timer? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "automation_laws_channel"
        
        fun start(context: Context) {
            val intent = Intent(context, AutomationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, AutomationService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        lawManager = AutomationLawManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        setupMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        cleanupMonitoring()
    }
    
    /**
     * Sets up event monitoring
     */
    private fun setupMonitoring() {
        setupBatteryMonitoring()
        setupWifiMonitoring()
        setupLocationMonitoring()
        setupTimeMonitoring()
    }
    
    /**
     * Battery monitoring
     */
    private fun setupBatteryMonitoring() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val batteryPct = (level / scale.toFloat() * 100).toInt()
                    
                    checkLawsForTrigger(TriggerData.BatteryData(batteryPct))
                }
            }
        }
        
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }
    
    /**
     * WiFi monitoring
     */
    private fun setupWifiMonitoring() {
        wifiReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val wifiInfo = wifiManager?.connectionInfo
                
                wifiInfo?.let {
                    // Connected devices could be detected here
                    // For now, we only detect network changes
                    val bssid = it.bssid ?: return
                    checkLawsForTrigger(TriggerData.NetworkData(bssid))
                }
            }
        }
        
        val filter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(wifiReceiver, filter)
    }
    
    /**
     * Location monitoring
     */
    private fun setupLocationMonitoring() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60000)
                .setMinUpdateIntervalMillis(30000)
                .build()
            
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        currentLocation = location
                        checkLawsForTrigger(
                            TriggerData.LocationData(location.latitude, location.longitude)
                        )
                    }
                }
            }
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Location permissions missing
        }
    }
    
    /**
     * Time monitoring
     */
    private fun setupTimeMonitoring() {
        timeCheckTimer = Timer()
        timeCheckTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                
                checkLawsForTrigger(TriggerData.TimeData(hour, minute))
            }
        }, 0, 60000) // Check every minute
    }
    
    /**
     * Checks and executes laws based on a trigger
     */
    private fun checkLawsForTrigger(triggerData: TriggerData) {
        val activeLaws = lawManager.getActiveLaws()
        
        activeLaws.forEach { law ->
            if (lawManager.shouldExecuteLaw(law, triggerData)) {
                executeLawAction(law.action, law.name)
            }
        }
    }
    
    /**
     * Executes a law's action
     */
    private fun executeLawAction(action: LawAction, lawName: String) {
        when (action) {
            is LawAction.EnableNightMode -> {
                sendNotification("🌙 Law Executed", "$lawName: Night mode activated")
                // Integration with the app's night mode logic would go here
            }
            is LawAction.DisableNightMode -> {
                sendNotification("☀️ Law Executed", "$lawName: Night mode deactivated")
            }
            is LawAction.EnableAlarm -> {
                sendNotification("🛡️ Law Executed", "$lawName: Alarm activated")
            }
            is LawAction.DisableAlarm -> {
                sendNotification("🔓 Law Executed", "$lawName: Alarm deactivated")
            }
            is LawAction.ShareLocation -> {
                sendNotification("📍 Law Executed", "$lawName: Location shared")
                currentLocation?.let {
                    // Integration with location sharing logic would go here
                }
            }
            is LawAction.TriggerSOS -> {
                sendNotification("🚨 Law Executed", "$lawName: SOS activated", true)
                // Integration with the SOS button would go here
            }
            is LawAction.EnableCameras -> {
                sendNotification("📹 Law Executed", "$lawName: Cameras activated")
            }
            is LawAction.DisableCameras -> {
                sendNotification("📹 Law Executed", "$lawName: Cameras deactivated")
            }
            is LawAction.SendNotification -> {
                sendNotification(action.title, action.message)
            }
        }
    }
    
    /**
     * Sends a notification to the user
     */
    private fun sendNotification(title: String, message: String, isUrgent: Boolean = false) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.icon_shield_gold)
            .setPriority(if (isUrgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    /**
     * Cleans up receivers and timers
     */
    private fun cleanupMonitoring() {
        batteryReceiver?.let { unregisterReceiver(it) }
        wifiReceiver?.let { unregisterReceiver(it) }
        timeCheckTimer?.cancel()
    }
    
    /**
     * Creates the notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Automation Laws",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Royal Shield automation notifications"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Creates the foreground service notification
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚡ Automation Laws Active")
            .setContentText("Monitoring events and executing automatic rules")
            .setSmallIcon(R.drawable.icon_shield_gold)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
