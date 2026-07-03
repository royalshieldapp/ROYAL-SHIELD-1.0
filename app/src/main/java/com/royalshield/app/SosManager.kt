package com.royalshield.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.royalshield.app.managers.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SosManager {

    /**
     * Triggers a comprehensive SOS alert:
     * 1. Vibrates device for feedback.
     * 2. Gathers location.
     * 3. Sends alerts via TwilioManager (prioritizing Cloud Backend -> Twilio API -> Device SMS).
     * 4. Logs event locally and in Firebase.
     */
    fun triggerSos(context: Context) {
        // 1. Initial haptic feedback
        vibrate(context, 100)
        
        // 2. Gather All Contacts
        val allContacts = mutableListOf<String>()
        
        // Add single phone if present
        PreferencesManager.getEmergencyPhone()?.let { if (it.isNotBlank()) allContacts.add(it) }
        
        // Add contacts from the list
        PreferencesManager.getEmergencyContacts().forEach { contact ->
            if (!allContacts.contains(contact.phone)) {
                allContacts.add(contact.phone)
            }
        }

        if (allContacts.isEmpty()) {
            Toast.makeText(context, "No Emergency Contacts Configured!", Toast.LENGTH_LONG).show()
            return
        }

        // 3. Get Location & Send
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                executeSosDelivery(context, allContacts, location)
            }.addOnFailureListener {
                executeSosDelivery(context, allContacts, null)
            }
        } else {
            executeSosDelivery(context, allContacts, null)
        }
    }

    // Keep legacy name for backward compatibility if needed, but point to new logic
    fun triggerSilentSos(context: Context) = triggerSos(context)

    private fun executeSosDelivery(context: Context, phoneNumbers: List<String>, location: Location?) {
        val lat = location?.latitude
        val lng = location?.longitude
        
        val mapsUrl = if (lat != null && lng != null) {
            "https://maps.google.com/?q=$lat,$lng"
        } else {
            "Unknown Location"
        }
        
        val message = "🚨 SOS ALERT! I am in danger! Need help immediately. Location: $mapsUrl"

        // 1. Log Local History
        PreferencesManager.addSOSEvent(
            hasLocation = location != null,
            latitude = lat,
            longitude = lng
        )

        // 2. Notify Cloud (Firebase)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = com.royalshield.app.data.SosRepository()
                repository.sendSosAlert(latitude = lat, longitude = lng)
            } catch (e: Exception) {
                Log.e("SosManager", "Firebase SOS update failed", e)
            }
        }

        // 3. Send Multi-Channel Alerts via TwilioManager
        val twilio = com.royalshield.app.managers.TwilioManager(context)
        phoneNumbers.forEach { phone ->
            twilio.sendEmergencySms(phone, message, lat, lng) { success ->
                if (success) {
                    Log.d("SosManager", "SOS sent successfully to $phone")
                } else {
                    Log.e("SosManager", "Total failure sending SOS to $phone")
                }
            }
        }

        // 4. Final confirmation vibration
        vibrate(context, 500)
    }

    private fun vibrate(context: Context, duration: Long) {
        if (!PreferencesManager.isVibrationEnabled()) return
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(duration)
        }
    }
}
