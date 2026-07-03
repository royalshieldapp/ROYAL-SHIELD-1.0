package com.royalshield.app.managers

import android.content.Context
import android.location.Location
import android.util.Log
import android.widget.Toast

/**
 * Manager to handle destination arrival reminders.
 * Notifies the user when they are within a specific radius of their destination.
 */
class DestinationReminderManager(private val context: Context) {

    data class ReminderDestination(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Float = 500f, // Default 500m alert radius
        var isActive: Boolean = true
    )

    private val destinations = mutableListOf<ReminderDestination>()

    fun addDestination(name: String, lat: Double, lon: Double, radius: Float = 500f) {
        destinations.add(ReminderDestination(name, lat, lon, radius))
        Log.d("MapReminder", "Added reminder for: $name")
    }

    fun checkArrival(currentLocation: Location) {
        val iterator = destinations.iterator()
        while (iterator.hasNext()) {
            val dest = iterator.next()
            if (!dest.isActive) continue

            val destLocation = Location("").apply {
                latitude = dest.latitude
                longitude = dest.longitude
            }

            val distance = currentLocation.distanceTo(destLocation)
            
            if (distance <= dest.radiusMeters) {
                Log.i("MapReminder", "Arriving at destination: ${dest.name}")
                showArrivalNotification(dest.name)
                dest.isActive = false // Trigger once
            }
        }
    }

    private fun showArrivalNotification(destinationName: String) {
        // In a real app, this would be a system notification
        Toast.makeText(
            context,
            "📍 Arriving at $destinationName soon!",
            Toast.LENGTH_LONG
        ).show()
    }
}
