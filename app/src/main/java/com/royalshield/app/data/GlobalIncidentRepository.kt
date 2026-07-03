package com.royalshield.app.data

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

data class Incident(
    val id: Int, 
    val title: String, 
    val type: String, 
    val lat: Double, 
    val lon: Double,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class MapReminder(
    val id: Int, 
    val name: String, 
    val address: String, 
    val isActive: Boolean,
    val lat: Double,
    val lon: Double
)

// Simplified singleton without Dagger for now if Dagger is not set up, 
// using 'object' or manual dependency injection to ensure it works immediately.
object GlobalIncidentRepository {

    suspend fun getIncidents(): List<Incident> {
        delay(1000) // Simulate network latency
        return listOf(
            Incident(1, "Theft reported", "Theft", 4.6097, -74.0817, "Robo a mano armada reportado cerca de la plaza central."),
            Incident(2, "Manifestation", "Protest", 4.6200, -74.0900, "Protesta pacifica bloqueando la via principal."),
            Incident(3, "Suspicious Activity", "Suspicious", 4.6150, -74.0850, "Personas merodeando vehiculos estacionados.")
        )
    }

    suspend fun getReminders(): List<MapReminder> {
        delay(500)
        return listOf(
            MapReminder(1, "Office", "Downtown Ave 45", true, 4.6097, -74.0817),
            MapReminder(2, "Home", "Sunset Blvd 12", false, 4.6200, -74.0900),
            MapReminder(3, "Gym", "Fit Street 99", true, 4.6150, -74.0850)
        )
    }
}
