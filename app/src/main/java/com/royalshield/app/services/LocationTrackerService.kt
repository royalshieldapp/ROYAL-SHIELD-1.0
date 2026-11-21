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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationTrackerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: FamilyRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

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
        if (PreferencesManager.getDeviceRole() != "CHILD") {
            stopSelf()
            return START_NOT_STICKY
        }

        val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasLocationPermission) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } catch (e: SecurityException) {
            android.util.Log.e("LocationTrackerService", "Unable to start location foreground service", e)
            stopSelf()
            return START_NOT_STICKY
        }
        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
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
            locationCallback?.let { callback ->
                fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            }
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
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        serviceScope.cancel()
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
