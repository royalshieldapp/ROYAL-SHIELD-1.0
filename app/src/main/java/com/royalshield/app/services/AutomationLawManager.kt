package com.royalshield.app.services

import android.content.Context
import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize

/**
 * Manager to handle Automation Laws
 * Similar to Fing's "Laws" system
 */
class AutomationLawManager(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("automation_laws", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_LAWS = "laws"
    }
    
    /**
     * Gets all saved laws
     */
    fun getAllLaws(): List<AutomationLaw> {
        val json = prefs.getString(KEY_LAWS, null) ?: return emptyList()
        val type = object : TypeToken<List<AutomationLaw>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Saves or updates a law
     */
    fun saveLaw(law: AutomationLaw) {
        val laws = getAllLaws().toMutableList()
        val existingIndex = laws.indexOfFirst { it.id == law.id }
        
        if (existingIndex >= 0) {
            laws[existingIndex] = law
        } else {
            laws.add(law)
        }
        
        val json = gson.toJson(laws)
        prefs.edit().putString(KEY_LAWS, json).apply()
    }
    
    /**
     * Deletes a law by ID
     */
    fun deleteLaw(lawId: String) {
        val laws = getAllLaws().toMutableList()
        laws.removeAll { it.id == lawId }
        
        val json = gson.toJson(laws)
        prefs.edit().putString(KEY_LAWS, json).apply()
    }
    
    /**
     * Gets all active laws
     */
    fun getActiveLaws(): List<AutomationLaw> {
        return getAllLaws().filter { it.enabled }
    }
    
    /**
     * Checks if a law should be executed based on its trigger
     */
    fun shouldExecuteLaw(law: AutomationLaw, triggerData: TriggerData): Boolean {
        if (!law.enabled) return false
        
        return when (law.trigger) {
            is LawTrigger.TimeSchedule -> {
                triggerData is TriggerData.TimeData &&
                triggerData.hour == law.trigger.startHour &&
                triggerData.minute == law.trigger.startMinute
            }
            is LawTrigger.BatteryLow -> {
                triggerData is TriggerData.BatteryData &&
                triggerData.batteryLevel <= law.trigger.threshold
            }
            is LawTrigger.LocationEnter -> {
                triggerData is TriggerData.LocationData &&
                isWithinRadius(
                    triggerData.latitude,
                    triggerData.longitude,
                    law.trigger.latitude,
                    law.trigger.longitude,
                    law.trigger.radius
                )
            }
            is LawTrigger.LocationExit -> {
                triggerData is TriggerData.LocationData &&
                !isWithinRadius(
                    triggerData.latitude,
                    triggerData.longitude,
                    law.trigger.latitude,
                    law.trigger.longitude,
                    law.trigger.radius
                )
            }
            is LawTrigger.DeviceConnected -> {
                triggerData is TriggerData.NetworkData &&
                triggerData.deviceMac == law.trigger.deviceMac
            }
            is LawTrigger.InternetSpeedDrop -> {
                triggerData is TriggerData.SpeedData &&
                triggerData.speedMbps < law.trigger.thresholdMbps
            }
        }
    }
    
    /**
     * Calculates if a location is within the specified radius
     */
    private fun isWithinRadius(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
        radiusMeters: Int
    ): Boolean {
        val earthRadius = 6371000.0 // Earth radius in meters
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distance = earthRadius * c
        
        return distance <= radiusMeters
    }
}

/**
 * Data model for an Automation Law
 */
@Parcelize
data class AutomationLaw(
    val id: String,
    val name: String,
    val trigger: LawTrigger,
    val action: LawAction,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Trigger types (events that activate a law)
 */
sealed class LawTrigger : Parcelable {
    
    @Parcelize
    data class TimeSchedule(
        val startHour: Int,
        val startMinute: Int
    ) : LawTrigger()
    
    @Parcelize
    data class BatteryLow(
        val threshold: Int // Percentage
    ) : LawTrigger()
    
    @Parcelize
    data class LocationEnter(
        val latitude: Double,
        val longitude: Double,
        val radius: Int // Meters
    ) : LawTrigger()
    
    @Parcelize
    data class LocationExit(
        val latitude: Double,
        val longitude: Double,
        val radius: Int // Meters
    ) : LawTrigger()
    
    @Parcelize
    data class DeviceConnected(
        val deviceMac: String
    ) : LawTrigger()
    
    @Parcelize
    data class InternetSpeedDrop(
        val thresholdMbps: Double
    ) : LawTrigger()
    
    fun getDescription(): String {
        return when (this) {
            is TimeSchedule -> "Schedule: ${startHour}:${startMinute.toString().padStart(2, '0')}"
            is BatteryLow -> "Battery < $threshold%"
            is LocationEnter -> "Enter location (${radius}m)"
            is LocationExit -> "Exit location (${radius}m)"
            is DeviceConnected -> "Device connected"
            is InternetSpeedDrop -> "Speed < $thresholdMbps Mbps"
        }
    }
}

/**
 * Types of actions a law can execute
 */
sealed class LawAction : Parcelable {
    
    @Parcelize
    object EnableNightMode : LawAction()
    
    @Parcelize
    object DisableNightMode : LawAction()
    
    @Parcelize
    object EnableAlarm : LawAction()
    
    @Parcelize
    object DisableAlarm : LawAction()
    
    @Parcelize
    object ShareLocation : LawAction()
    
    @Parcelize
    object TriggerSOS : LawAction()
    
    @Parcelize
    object EnableCameras : LawAction()
    
    @Parcelize
    object DisableCameras : LawAction()
    
    @Parcelize
    data class SendNotification(
        val title: String,
        val message: String
    ) : LawAction()
    
    fun getDescription(): String {
        return when (this) {
            is EnableNightMode -> "Enable night mode"
            is DisableNightMode -> "Disable night mode"
            is EnableAlarm -> "Enable alarm"
            is DisableAlarm -> "Disable alarm"
            is ShareLocation -> "Share location"
            is TriggerSOS -> "Enable SOS"
            is EnableCameras -> "Enable cameras"
            is DisableCameras -> "Disable cameras"
            is SendNotification -> "Notification: $title"
        }
    }
}

/**
 * Trigger data to check if a law should execute
 */
sealed class TriggerData {
    data class TimeData(val hour: Int, val minute: Int) : TriggerData()
    data class BatteryData(val batteryLevel: Int) : TriggerData()
    data class LocationData(val latitude: Double, val longitude: Double) : TriggerData()
    data class NetworkData(val deviceMac: String) : TriggerData()
    data class SpeedData(val speedMbps: Double) : TriggerData()
}
