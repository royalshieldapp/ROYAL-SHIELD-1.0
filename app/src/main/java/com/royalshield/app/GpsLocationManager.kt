package com.royalshield.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.Executors

/**
 * Manages GPS location for secure tracking.
 * Sends the location to a server and notifies listeners.
 */
class GpsLocationManager(
    private val context: Context,
    private val onLocationChanged: ((Location) -> Unit)? = null
) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val scheduler = Executors.newScheduledThreadPool(1)
    private val client = OkHttpClient()

    /**
     * Starts location tracking.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    Log.d("GpsLocationManager", "New Location: ${it.latitude}, ${it.longitude}")
                    sendLocationToServer(it.latitude, it.longitude)
                    onLocationChanged?.invoke(it)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("GpsLocationManager", "Missing location permission", e)
        }
    }

    /**
     * Stops location tracking.
     */
    fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        scheduler.shutdown()
    }

    /**
     * Sends location to the server.
     */
    private fun sendLocationToServer(latitude: Double, longitude: Double) {
        scheduler.execute {
            val json = "{\"latitude\":$latitude, \"longitude\":$longitude}"
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("https://your-api.com/location") // Placeholder
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) Log.e("GpsLocationManager", "Error: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("GpsLocationManager", "Network failure: ${e.message}")
            }
        }
    }
}
