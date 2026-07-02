package com.royalshield.app.features.riskprediction.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Risk Map API Response Models
 * Matches backend API responses from royal_shield_backend
 */


data class RiskMapResponse(
    val type: String,
    val features: List<RiskFeature>,
    val metadata: RiskMapMetadata
)


data class RiskFeature(
    val type: String,
    val geometry: Geometry,
    val properties: RiskProperties
)


data class Geometry(
    val type: String,
    val coordinates: List<List<List<Double>>>  // [[[lng, lat], ...]]
)


data class RiskProperties(
    @Json(name = "h3_cell") val h3Cell: String,
    @Json(name = "risk_score") val riskScore: Double,
    @Json(name = "risk_level") val riskLevel: String,
    @Json(name = "event_count") val eventCount: Int,
    @Json(name = "crime_count") val crimeCount: Int,
    @Json(name = "fire_count") val fireCount: Int,
    @Json(name = "recent_7d") val recent7d: Int,
    @Json(name = "recent_30d") val recent30d: Int
)


data class RiskMapMetadata(
    val bbox: BoundingBox,
    val resolution: Int,
    @Json(name = "zone_count") val zoneCount: Int,
    @Json(name = "generated_at") val generatedAt: String
)


data class BoundingBox(
    @Json(name = "min_lat") val minLat: Double,
    @Json(name = "min_lng") val minLng: Double,
    @Json(name = "max_lat") val maxLat: Double,
    @Json(name = "max_lng") val maxLng: Double
)

// Zone Details

data class ZoneDetailsResponse(
    @Json(name = "h3_cell") val h3Cell: String,
    val center: List<Double>,  // [lat, lng]
    @Json(name = "risk_score") val riskScore: Double,
    @Json(name = "risk_level") val riskLevel: String,
    val statistics: ZoneStatistics,
    @Json(name = "recent_events") val recentEvents: List<RecentEvent>,
    val trends: Trends
)


data class ZoneStatistics(
    @Json(name = "total_events") val totalEvents: Int,
    @Json(name = "crime_events") val crimeEvents: Int,
    @Json(name = "fire_events") val fireEvents: Int,
    @Json(name = "osint_events") val osintEvents: Int?,
    @Json(name = "recent_7d") val recent7d: Int,
    @Json(name = "recent_30d") val recent30d: Int,
    @Json(name = "severity_critical") val severityCritical: Int,
    @Json(name = "severity_high") val severityHigh: Int,
    @Json(name = "severity_medium") val severityMedium: Int,
    @Json(name = "severity_low") val severityLow: Int
)


data class RecentEvent(
    val type: String,
    val severity: String,
    @Json(name = "occurred_at") val occurredAt: String,
    val description: String
)


data class Trends(
    @Json(name = "7d_change") val change7d: String,
    @Json(name = "30d_change") val change30d: String,
    val direction: String
)

// Hotspots

data class HotspotsResponse(
    val hotspots: List<Hotspot>,
    @Json(name = "total_count") val totalCount: Int,
    val metadata: HotspotMetadata
)


data class Hotspot(
    @Json(name = "hotspot_id") val hotspotId: Int,
    val center: HotspotCenter,
    @Json(name = "h3_cell") val h3Cell: String,
    @Json(name = "radius_meters") val radiusMeters: Double,
    @Json(name = "event_count") val eventCount: Int,
    @Json(name = "event_types") val eventTypes: Map<String, Int>,
    val severities: Map<String, Int>,
    @Json(name = "risk_score") val riskScore: Double,
    @Json(name = "risk_level") val riskLevel: String
)


data class HotspotCenter(
    val lat: Double,
    val lng: Double
)


data class HotspotMetadata(
    @Json(name = "clusters_found") val clustersFound: Int,
    @Json(name = "total_events") val totalEvents: Int,
    @Json(name = "recent_events") val recentEvents: Int,
    @Json(name = "noise_points") val noisePoints: Int,
    @Json(name = "time_window_days") val timeWindowDays: Int,
    @Json(name = "generated_at") val generatedAt: String
)

// Prediction

data class PredictRiskRequest(
    val locations: List<LocationInput>,
    @Json(name = "prediction_date") val predictionDate: String? = null
)


data class LocationInput(
    val lat: Double,
    val lng: Double
)


data class PredictRiskResponse(
    val predictions: List<RiskPrediction>,
    @Json(name = "total_requested") val totalRequested: Int,
    @Json(name = "total_predicted") val totalPredicted: Int,
    @Json(name = "prediction_date") val predictionDate: String,
    @Json(name = "generated_at") val generatedAt: String
)


data class RiskPrediction(
    val location: Map<String, Double>,  // {"lat": ..., "lng": ...}
    @Json(name = "h3_cell") val h3Cell: String,
    @Json(name = "predicted_risk_score") val predictedRiskScore: Double,
    @Json(name = "predicted_risk_level") val predictedRiskLevel: String,
    val confidence: Double,
    @Json(name = "prediction_date") val predictionDate: String
)

// Explanation

data class PredictionExplanation(
    @Json(name = "h3_cell") val h3Cell: String,
    @Json(name = "risk_score") val riskScore: Double,
    @Json(name = "risk_level") val riskLevel: String,
    @Json(name = "top_factors") val topFactors: List<RiskFactor>,
    @Json(name = "natural_language") val naturalLanguage: String,
    val confidence: Double
)


data class RiskFactor(
    val feature: String,
    val value: Double,
    val contribution: Double,
    val direction: String
)
