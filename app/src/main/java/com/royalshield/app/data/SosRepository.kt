package com.royalshield.app.data

import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.royalshield.app.managers.PreferencesManager
import kotlinx.coroutines.tasks.await
import java.util.Date

class SosRepository {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    data class SosAlert(
        val userId: String,
        val userEmail: String?,
        val timestamp: Date,
        val location: GeoPointData?,
        val batteryLevel: Int,
        val status: String = "ACTIVE",
        val evidencePhotoBase64: String? = null
    )

    data class GeoPointData(val latitude: Double, val longitude: Double)

    suspend fun sendSosAlert(
        latitude: Double?, 
        longitude: Double?, 
        batteryLevel: Int = -1,
        photoBase64: String? = null
    ): Boolean {
        return try {
            val user = auth.currentUser
            val userId = user?.uid ?: "anonymous_${PreferencesManager.getEmergencyPhone()}"
            
            val alert = SosAlert(
                userId = userId,
                userEmail = user?.email,
                timestamp = Date(),
                location = if (latitude != null && longitude != null) GeoPointData(latitude, longitude) else null,
                batteryLevel = batteryLevel,
                evidencePhotoBase64 = photoBase64
            )

            db.collection("sos_alerts")
                .add(alert)
                .await()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
