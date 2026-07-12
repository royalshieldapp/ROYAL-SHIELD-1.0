package com.royalshield.app.features.riskprediction.data.repository

import android.util.Log
import com.royalshield.app.features.riskprediction.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for Risk Prediction API
 * Handles all backend communication with error handling and caching
 */
class RiskPredictionRepository {
    
    private val api = RiskApiClient.api
    private val TAG = "RiskPredictionRepository"
    
    /**
     * Get risk heatmap for bounding box
     */
    suspend fun getRiskMap(
        minLat: Double,
        minLng: Double,
        maxLat: Double,
        maxLng: Double,
        resolution: Int = 9
    ): Result<RiskMapResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching risk map: ($minLat,$minLng) to ($maxLat,$maxLng)")
            
            val response = api.getRiskMap(minLat, minLng, maxLat, maxLng, resolution)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Risk map fetched: ${response.body()!!.features.size} zones")
                Result.success(response.body()!!)
            } else {
                val error = "API error: ${response.code()} ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get zone details
     */
    suspend fun getZoneDetails(h3Cell: String): Result<ZoneDetailsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getZoneDetails(h3Cell)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch zone details"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching zone details: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get current hotspots
     */
    suspend fun getHotspots(
        minLat: Double? = null,
        minLng: Double? = null,
        maxLat: Double? = null,
        maxLng: Double? = null,
        severity: String? = null,
        timeWindowDays: Int = 30
    ): Result<HotspotsResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching hotspots...")
            
            val response = api.getHotspots(
                minLat, minLng, maxLat, maxLng,
                hotspotType = null,
                severity = severity,
                timeWindowDays = timeWindowDays
            )
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Hotspots fetched: ${response.body()!!.hotspots.size} clusters")
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch hotspots"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching hotspots: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Predict risk for location
     */
    suspend fun predictRisk(
        lat: Double,
        lng: Double
    ): Result<RiskPrediction> = withContext(Dispatchers.IO) {
        try {
            val request = PredictRiskRequest(
                locations = listOf(LocationInput(lat, lng))
            )
            
            val response = api.predictRisk(request)
            
            if (response.isSuccessful && response.body() != null) {
                val predictions = response.body()!!.predictions
                if (predictions.isNotEmpty()) {
                    Result.success(predictions[0])
                } else {
                    Result.failure(Exception("No prediction returned"))
                }
            } else {
                Result.failure(Exception("Failed to predict risk"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error predicting risk: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get prediction explanation
     */
    suspend fun explainPrediction(h3Cell: String): Result<PredictionExplanation> = withContext(Dispatchers.IO) {
        try {
            val response = api.explainPrediction(h3Cell)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get explanation"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting explanation: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getMapLayers(
        minLat: Double,
        minLng: Double,
        maxLat: Double,
        maxLng: Double,
        layers: String = "cameras,police,speed,protest,theft,cyber"
    ): Result<MapLayersResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMapLayers(minLat, minLng, maxLat, maxLng, layers)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch map layers"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching map layers: ${e.message}", e)
            Result.failure(e)
        }
    }
}
