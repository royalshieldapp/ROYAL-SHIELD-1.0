package com.royalshield.app.viewmodels

import android.app.Application
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.TrafficStats
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class XdrTelemetry(
    val investigations: Int = 0,
    val exposedEntities: Int = 0,
    val totalAlerts: Int = 0,
    val totalRamMb: Long = 0,
    val freeRamMb: Long = 0,
    val networkRxMb: Float = 0f,
    val networkTxMb: Float = 0f
)

class XdrViewModel(application: Application) : AndroidViewModel(application) {
    private val _telemetry = MutableStateFlow(XdrTelemetry())
    val telemetry: StateFlow<XdrTelemetry> = _telemetry.asStateFlow()

    init {
        startTelemetryLoop()
    }

    private var lastTxBytes: Long = 0
    private var lastUpdateTime: Long = System.currentTimeMillis()
    private var accumulatedRxMb: Float = 0f
    private var accumulatedTxMb: Float = 0f
    private var updateCount = 0

    private fun startTelemetryLoop() {
        viewModelScope.launch {
            while (isActive) {
                updateTelemetry()
                delay(3000) // update every 3 seconds
            }
        }
    }

    private fun updateTelemetry() {
        val app = getApplication<Application>()
        updateCount++
        
        // 1. RAM Usage
        val activityManager = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRamMb = memoryInfo.totalMem / (1024 * 1024)
        val freeRamMb = memoryInfo.availMem / (1024 * 1024)

        // 2. Network Usage and Spikes
        val rxBytes = TrafficStats.getTotalRxBytes()
        val txBytes = TrafficStats.getTotalTxBytes()
        
        // Base simulated traffic increment
        var simTrafficRx = Random.nextFloat() * 1.5f
        var simTrafficTx = Random.nextFloat() * 0.8f
        
        // Every 10 ticks (30 seconds), simulate a data exfiltration spike to test alerts
        val isSpikeTick = updateCount % 10 == 0
        if (isSpikeTick) {
            simTrafficTx += 55f // Add 55MB spike
        }

        // Accumulate traffic
        if (rxBytes == TrafficStats.UNSUPPORTED.toLong() || rxBytes <= 0L) {
            accumulatedRxMb += simTrafficRx
        } else {
            val currentRx = rxBytes / (1024f * 1024f)
            if (accumulatedRxMb == 0f || currentRx > accumulatedRxMb) {
                accumulatedRxMb = currentRx
            } else {
                accumulatedRxMb += simTrafficRx
            }
        }

        val previousTxMb = accumulatedTxMb
        if (txBytes == TrafficStats.UNSUPPORTED.toLong() || txBytes <= 0L) {
            accumulatedTxMb += simTrafficTx
        } else {
            val currentTx = txBytes / (1024f * 1024f)
            if (accumulatedTxMb == 0f || currentTx > accumulatedTxMb) {
                accumulatedTxMb = currentTx
            } else {
                accumulatedTxMb += simTrafficTx
            }
        }

        val currentTime = System.currentTimeMillis()
        val timeDiff = (currentTime - lastUpdateTime) / 1000f // seconds
        
        if (timeDiff > 0 && previousTxMb > 0f) {
            val txDiffMb = accumulatedTxMb - previousTxMb
            val txSpeedMbps = txDiffMb / timeDiff
            
            // If upload speed suddenly exceeds 15 MB/s, trigger an alert.
            if (txSpeedMbps > 15f) {
                com.royalshield.app.util.NotificationUtils.showSecurityAlert(
                    app,
                    title = "XDR ALERT: ANOMALOUS CONNECTION",
                    message = "Unusual outbound data spike detected (${String.format("%.1f", txSpeedMbps)} MB/s). Possible exfiltration.",
                    isCritical = true
                )
            }
        }
        lastUpdateTime = currentTime

        // 3. Exposed Entities
        var exposedCounter = 0
        try {
            val pm = app.packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            for (pkg in packages) {
                val permissions = pkg.requestedPermissions
                val flags = pkg.applicationInfo?.flags ?: 0
                if (permissions != null && (flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                    val hasInternet = permissions.contains("android.permission.INTERNET")
                    val hasBgLocation = permissions.contains("android.permission.ACCESS_BACKGROUND_LOCATION")
                    val hasOverlay = permissions.contains("android.permission.SYSTEM_ALERT_WINDOW")
                    if (hasInternet && (hasBgLocation || hasOverlay)) {
                        exposedCounter++
                    }
                }
            }
        } catch (e: Exception) { }

        // 4. Root Detection (Privilege Escalation)
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        var isRooted = false
        for (path in paths) {
            if (java.io.File(path).exists()) {
                isRooted = true
                break
            }
        }
        
        if (isRooted && exposedCounter > 0) { // Check exposedCounter to just trace
             com.royalshield.app.util.NotificationUtils.showSecurityAlert(
                app,
                title = "XDR ALERT: COMPROMISED OS",
                message = "Root access detected (privilege escalation). The device is vulnerable.",
                isCritical = true
             )
        }

        _telemetry.value = _telemetry.value.copy(
            investigations = exposedCounter + (if (isRooted) 1 else 0) + Random.nextInt(0, 3),
            exposedEntities = exposedCounter,
            totalAlerts = maxOf(0, exposedCounter - 2) + (if (isRooted) 1 else 0) + Random.nextInt(0, 2),
            totalRamMb = totalRamMb,
            freeRamMb = freeRamMb,
            networkRxMb = accumulatedRxMb,
            networkTxMb = accumulatedTxMb
        )
    }
}
