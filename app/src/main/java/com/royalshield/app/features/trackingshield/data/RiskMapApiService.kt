package com.royalshield.app.features.trackingshield.data

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Response

data class RiskMapResponse(
    val type: String,
    val features: List<RiskFeature>,
    val metadata: RiskMetadata
)

data class RiskFeature(
    val type: String,
    val geometry: Geometry,
    val properties: RiskProperties
)

data class Geometry(
    val type: String,
    val coordinates: List<List<List<Double>>>
)

data class RiskProperties(
    val h3_cell: String,
    val risk_score: Double,
    val risk_level: String,
    val event_count: Int
)

data class RiskMetadata(
    val zone_count: Int,
    val generated_at: String
)

interface RiskMapApiService {
    @GET("/api/v1/risk-map")
    suspend fun getRiskMap(
        @Query("bbox_min_lat") minLat: Double,
        @Query("bbox_min_lng") minLng: Double,
        @Query("bbox_max_lat") maxLat: Double,
        @Query("bbox_max_lng") maxLng: Double,
        @Query("resolution") resolution: Int = 9
    ): Response<RiskMapResponse>
    
    @GET("/api/v1/hotspots")
    suspend fun getHotspots(
        @Query("min_severity") minSeverity: String = "HIGH"
    ): Response<Any> // We will define Hotspot response later
}
