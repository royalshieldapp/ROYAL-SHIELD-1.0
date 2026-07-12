package com.royalshield.app.features.riskprediction.data.remote

import retrofit2.Response
import retrofit2.http.*

/**
 * Royal Shield Risk Prediction API
 * Connects to backend REST API (localhost or Render)
 */
interface RiskPredictionApi {
    
    /**
     * Get risk heatmap for a bounding box
     * GET /api/v1/risk-map
     */
    @GET("/api/v1/risk-map")
    suspend fun getRiskMap(
        @Query("bbox_min_lat") minLat: Double,
        @Query("bbox_min_lng") minLng: Double,
        @Query("bbox_max_lat") maxLat: Double,
        @Query("bbox_max_lng") maxLng: Double,
        @Query("resolution") resolution: Int = 9,
        @Query("date_filter") dateFilter: String? = null
    ): Response<RiskMapResponse>
    
    /**
     * Get zone details for specific H3 cell
     * GET /api/v1/risk-zones/{h3_cell}
     */
    @GET("/api/v1/risk-zones/{h3_cell}")
    suspend fun getZoneDetails(
        @Path("h3_cell") h3Cell: String
    ): Response<ZoneDetailsResponse>
    
    /**
     * Get risk history for a zone
     * GET /api/v1/risk-history
     */
    @GET("/api/v1/risk-history")
    suspend fun getRiskHistory(
        @Query("h3_cell") h3Cell: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): Response<Map<String, Any>>
    
    /**
     * Get current hotspots
     * GET /api/v1/hotspots
     */
    @GET("/api/v1/hotspots")
    suspend fun getHotspots(
        @Query("bbox_min_lat") minLat: Double? = null,
        @Query("bbox_min_lng") minLng: Double? = null,
        @Query("bbox_max_lat") maxLat: Double? = null,
        @Query("bbox_max_lng") maxLng: Double? = null,
        @Query("hotspot_type") hotspotType: String? = null,
        @Query("severity") severity: String? = null,
        @Query("time_window_days") timeWindowDays: Int = 30
    ): Response<HotspotsResponse>
    
    /**
     * Predict future hotspots
     * GET /api/v1/hotspots/predict
     */
    @GET("/api/v1/hotspots/predict")
    suspend fun predictHotspots(
        @Query("bbox_min_lat") minLat: Double? = null,
        @Query("bbox_min_lng") minLng: Double? = null,
        @Query("bbox_max_lat") maxLat: Double? = null,
        @Query("bbox_max_lng") maxLng: Double? = null,
        @Query("days_ahead") daysAhead: Int = 7
    ): Response<Map<String, Any>>
    
    /**
     * Find hotspots near a location
     * GET /api/v1/hotspots/nearby
     */
    @GET("/api/v1/hotspots/nearby")
    suspend fun getNearbyHotspots(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius_meters") radiusMeters: Double = 1000.0
    ): Response<Map<String, Any>>
    
    /**
     * Predict risk for locations
     * POST /api/v1/predict/risk
     */
    @POST("/api/v1/predict/risk")
    suspend fun predictRisk(
        @Body request: PredictRiskRequest
    ): Response<PredictRiskResponse>
    
    /**
     * Get explainable prediction
     * GET /api/v1/predict/explain
     */
    @GET("/api/v1/predict/explain")
    suspend fun explainPrediction(
        @Query("h3_cell") h3Cell: String,
        @Query("prediction_date") predictionDate: String? = null
    ): Response<PredictionExplanation>
    
    /**
     * Get trend forecast
     * GET /api/v1/predict/trends
     */
    @GET("/api/v1/predict/trends")
    suspend fun getTrendForecast(
        @Query("h3_cell") h3Cell: String,
        @Query("days") days: Int = 30
    ): Response<Map<String, Any>>

    @GET("/api/v1/map-layers")
    suspend fun getMapLayers(
        @Query("bbox_min_lat") minLat: Double,
        @Query("bbox_min_lng") minLng: Double,
        @Query("bbox_max_lat") maxLat: Double,
        @Query("bbox_max_lng") maxLng: Double,
        @Query("layers") layers: String = "cameras,police,speed,protest,theft,cyber"
    ): Response<MapLayersResponse>
}
