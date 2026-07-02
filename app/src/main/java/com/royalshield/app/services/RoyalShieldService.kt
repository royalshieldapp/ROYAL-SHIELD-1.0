package com.royalshield.app.services

import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.royalshield.app.GpsLocationManager
import com.royalshield.app.SoundDetector
import com.royalshield.app.managers.SensorSOSManager
import com.royalshield.app.managers.DestinationReminderManager
import com.royalshield.app.managers.TwilioManager
import com.royalshield.app.EmergencyCameraManager
import com.royalshield.app.managers.PreferencesManager
import com.royalshield.app.managers.AutomationEvaluator
import com.royalshield.app.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.os.BatteryManager
import android.content.Context
import androidx.lifecycle.LifecycleService

/**
 * Main service that runs in the background to manage security functions.
 */
class RoyalShieldService : LifecycleService() {

    private lateinit var soundDetector: SoundDetector
    private lateinit var gpsLocationManager: GpsLocationManager
    private lateinit var sensorSOSManager: SensorSOSManager
    private lateinit var destinationReminderManager: DestinationReminderManager
    private lateinit var twilioManager: TwilioManager
    private lateinit var emergencyCameraManager: EmergencyCameraManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var automationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("RoyalShieldService", "Background service created.")

        // Initialize Managers
        twilioManager = TwilioManager(this)
        destinationReminderManager = DestinationReminderManager(this)
        emergencyCameraManager = EmergencyCameraManager(this)
        emergencyCameraManager.initCamera(this)
        
        // Add a mock destination for testing arrival reminder
        // In a real app, these would come from user settings/history
        destinationReminderManager.addDestination("Office", 40.7128, -74.0060)

        // Initialize sound detector
        soundDetector = SoundDetector(
            context = this,
            amplitudeThreshold = 15000,
            onNoiseDetected = {
                Log.d("RoyalShieldService", "Sound alert detected!")
            }
        )

        // Initialize location manager with a callback for arrival reminders
        gpsLocationManager = GpsLocationManager(this) { location ->
            destinationReminderManager.checkArrival(location)
        }

        // Initialize Sensor-based SOS (Accelerometer)
        sensorSOSManager = SensorSOSManager(this) {
            triggerAutomatedSOS()
        }

        // Start Automation Engine (Phase 2.B)
        startAutomationMonitor()
    }

    private fun startAutomationMonitor() {
        val db = AppDatabase.getDatabase(this)
        automationJob = serviceScope.launch {
            db.automationDao().getAllRules().collect { rules ->
                // Check rules whenever they change or periodically
                // For simplicity, we'll evaluate battery every time rules load
                val battery = getBatteryLevel()
                rules.forEach { rule ->
                    AutomationEvaluator.evaluate(this@RoyalShieldService, rule, mapOf("battery" to battery))
                }
            }
        }
    }

    private fun getBatteryLevel(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("RoyalShieldService", "Background service started (Sensors & Reminders Active).")

        // Create Notification
        createNotificationChannel()
        val notification = createNotification()

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 14 (SDK 34) requires specific permissions for each foreground service type
                var serviceType = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION

                val hasLocation = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED

                // Add types if permissions are granted
                if (android.os.Build.VERSION.SDK_INT >= 30) { // FOREGROUND_SERVICE_TYPE_CAMERA/MICROPHONE added in API 30/34? Actually Camera is 30, Mic is 30.
                     if (hasCamera) serviceType = serviceType or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                     if (hasAudio) serviceType = serviceType or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                
                if (hasLocation) {
                    startForeground(1, notification, serviceType)
                } else {
                     // Fallback if no location, but maybe try to start if we have camera? 
                     // For now, adhere to existing logic which prioritizes location.
                     if (hasCamera || hasAudio) {
                         startForeground(1, notification, serviceType)
                     } else {
                         Log.w("RoyalShieldService", "Missing permissions for Foreground Service.")
                         if (android.os.Build.VERSION.SDK_INT >= 34) {
                             stopSelf()
                             return START_NOT_STICKY
                         } else {
                             startForeground(1, notification)
                         }
                     }
                }
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            Log.e("RoyalShieldService", "Error starting foreground service: ${e.message}")
            stopSelf() 
            return START_NOT_STICKY
        }

        // Safely start sensors. If permissions are missing, they just won't start.
        try {
            soundDetector.start()
        } catch (e: Exception) {
            Log.e("RoyalShieldService", "Error starting SoundDetector: ${e.message}")
        }

        try {
            gpsLocationManager.startLocationUpdates()
        } catch (e: Exception) {
            Log.e("RoyalShieldService", "Error starting Location Manager: ${e.message}")
        }

        try {
            sensorSOSManager.start()
        } catch (e: Exception) {
            Log.e("RoyalShieldService", "Error starting Sensors: ${e.message}")
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val serviceChannel = android.app.NotificationChannel(
                "ROYAL_SHIELD_CHANNEL",
                "Royal Shield Protection Service",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val notificationBuilder = androidx.core.app.NotificationCompat.Builder(this, "ROYAL_SHIELD_CHANNEL")
            .setContentTitle("Royal Shield Active")
            .setContentText("Protecting you in the background")
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Fallback icon, replace with specific app icon if available
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)

        return notificationBuilder.build()
    }

    private fun triggerAutomatedSOS() {
        Log.e("RoyalShieldService", "SENSOR DETECTED IMPACT - TRIGGERING SOS")
        
        // 1. Take Emergency Photo
        try {
            emergencyCameraManager.takePhotoAndSend()
        } catch (e: Exception) {
            Log.e("RoyalShieldService", "Failed to take emergency photo", e)
        }

        // 2. Send SMS
        val phone = PreferencesManager.getEmergencyPhone()
        if (!phone.isNullOrBlank()) {
            twilioManager.sendEmergencySms(phone, "🚨 AUTOMATED SOS: Impact/Crash detected! Please check on me.") { success ->
                if (success) {
                    // Use MainLooper for Toast if called from background thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                         Toast.makeText(this, "SOS Sent via Sensors + Photo Capture!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("RoyalShieldService", "Background service destroyed.")

        soundDetector.stop()
        gpsLocationManager.stopLocationUpdates()
        sensorSOSManager.stop()
        automationJob?.cancel()
        emergencyCameraManager.shutdown()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
