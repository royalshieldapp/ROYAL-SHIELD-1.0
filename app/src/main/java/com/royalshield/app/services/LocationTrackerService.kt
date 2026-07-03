package com.royalshield.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.royalshield.app.R
import com.royalshield.app.features.trackingshield.data.FamilyRepository
import com.royalshield.app.managers.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocationTrackerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: FamilyRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    companion object {
        private const val CHANNEL_ID = "RoyalShieldLocationChannel"
        private const val NOTIFICATION_ID = 999
        private const val UPDATE_INTERVAL = 30000L // 30 seconds
    }

    override fun onCreate() {
        super.onCreate()
        repository = FamilyRepository(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
        if (PreferencesManager.getDeviceRole() != "CHILD") {
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(10000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    serviceScope.launch {
                        // Get ParentUID from Preferences stored during pairing
                        val parentUid = PreferencesManager.getParentUid()
                        if (!parentUid.isNullOrEmpty()) {
                            repository.updateChildLocation(
                                parentUid,
                                location.latitude,
                                location.longitude,
                                location.speed * 3.6f, // m/s to km/h
                                getBatteryLevel()
                            )
                        }
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        // Observe remote play sound trigger
        serviceScope.launch {
            val parentUid = PreferencesManager.getParentUid()
            if (!parentUid.isNullOrEmpty()) {
                var lastPlayTime: Long = 0
                repository.observeAlertTrigger(parentUid).collect { timestamp ->
                    if (timestamp != null && timestamp > System.currentTimeMillis() - 10000 && timestamp != lastPlayTime) {
                        lastPlayTime = timestamp
                        playAlertSound()
                    }
                }
            }
        }
    }
    
    private fun playAlertSound() {
        try {
            val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, notificationUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun getBatteryLevel(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Child Tracking Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Royal Shield Protection")
            .setContentText("Monitoring location for your safety")
            .setSmallIcon(R.mipmap.ic_launcher) // Ensure this icon exists or use generic
            .setOngoing(true)
            .build()
    }
}
