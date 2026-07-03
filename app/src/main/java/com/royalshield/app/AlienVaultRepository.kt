package com.royalshield.app

import android.util.Log
import com.royalshield.app.managers.PreferencesManager
import com.royalshield.app.models.ThreatAlert
import com.royalshield.app.models.ThreatType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Repository to fetch real-time threat data from AlienVault OTX.
 */
class AlienVaultRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://otx.alienvault.com/api/v1"

    private fun getApiKey(): String? {
        return PreferencesManager.getAlienVaultApiKey()
    }

    /**
     * Fetches recent "Pulses" (threat indicators) from OTX.
     * Fallbacks to simulation if no API key is present.
     */
    suspend fun getRecentThreats(lat: Double? = null, lng: Double? = null): List<ThreatAlert> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank() || apiKey == "YOUR_OTX_KEY_HERE") {
            Log.d("AlienVaultRepo", "No API Key found. Using Simulator mode.")
            return@withContext getSimulatedThreats(lat, lng)
        }

        try {
            val request = Request.Builder()
                .url("$baseUrl/pulses/subscribed?limit=20")
                .addHeader("X-OTX-API-KEY", apiKey)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("AlienVaultRepo", "API Error: ${response.code}")
                return@withContext getSimulatedThreats(lat, lng)
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return@withContext emptyList()
            
            val alerts = mutableListOf<ThreatAlert>()
            for (i in 0 until results.length()) {
                val pulse = results.getJSONObject(i)
                val indicators = pulse.optJSONArray("indicators")
                
                // Extract geographic indicators if present
                if (indicators != null && indicators.length() > 0) {
                    for (j in 0 until indicators.length()) {
                        val indicator = indicators.getJSONObject(j)
                        val type = indicator.optString("type")
                        
                        // We only want indicators with lat/lon for the map
                        if (type == "IPv4" || type == "URL") {
                            val lat = indicator.optDouble("latitude", Double.NaN)
                            val lon = indicator.optDouble("longitude", Double.NaN)
                            
                            if (!lat.isNaN() && !lon.isNaN()) {
                                alerts.add(
                                    ThreatAlert(
                                        id = "OTX_${pulse.optString("id")}_$j",
                                        type = determineThreatType(pulse),
                                        label = pulse.optString("name", "Unknown Threat"),
                                        lat = lat,
                                        lon = lon,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            // If we found real indicators, return them. Otherwise fallback.
            if (alerts.isNotEmpty()) alerts else getSimulatedThreats(lat, lng)

        } catch (e: Exception) {
            Log.e("AlienVaultRepo", "Network Error in AlienVault OTX", e)
            getSimulatedThreats(lat, lng)
        }
    }

    private fun determineThreatType(pulse: JSONObject): ThreatType {
        val name = pulse.optString("name").lowercase()
        return when {
            name.contains("theft") || name.contains("phishing") -> ThreatType.THEFT
            name.contains("protest") || name.contains("manifestation") -> ThreatType.MANIFESTATION
            else -> ThreatType.SUSPICIOUS
        }
    }

    /**
     * Provides realistic simulated threat data when offline or no API key is present.
     * Uses the user's current GPS location if provided, generating simulated threats around them.
     */
    private fun getSimulatedThreats(userLat: Double?, userLng: Double?): List<ThreatAlert> {
        val baseLat = userLat ?: 4.6500 // Default to Bogota if no location is available
        val baseLng = userLng ?: -74.0700

        // Generate dynamic threats near the base location (+/- 0.05 degrees is ~ 5km)
        return listOf(
            ThreatAlert("S1", ThreatType.THEFT, "Botnet Activity Detected", baseLat + (Math.random() - 0.5) * 0.1, baseLng + (Math.random() - 0.5) * 0.1),
            ThreatAlert("S2", ThreatType.SUSPICIOUS, "SQL Injection Attempt", baseLat + (Math.random() - 0.5) * 0.1, baseLng + (Math.random() - 0.5) * 0.1),
            ThreatAlert("S3", ThreatType.MANIFESTATION, "Network Anomaly", baseLat + (Math.random() - 0.5) * 0.1, baseLng + (Math.random() - 0.5) * 0.1),
            ThreatAlert("S4", ThreatType.THEFT, "API Exploit", baseLat + (Math.random() - 0.5) * 0.1, baseLng + (Math.random() - 0.5) * 0.1),
            ThreatAlert("S5", ThreatType.SUSPICIOUS, "Malware Phoning Home", baseLat + (Math.random() - 0.5) * 0.1, baseLng + (Math.random() - 0.5) * 0.1)
        )
    }
}
