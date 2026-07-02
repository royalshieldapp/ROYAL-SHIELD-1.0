package com.royalshield.app.features.riskprediction.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import com.royalshield.app.features.riskprediction.data.remote.RiskFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Risk Heatmap Overlay Utilities
 * Integrates with existing GoogleMap composable
 */

/**
 * Convert risk zones to weighted points for heatmap
 */
fun riskZonesToWeightedPoints(zones: List<RiskFeature>): List<WeightedLatLng> {
    return zones.mapNotNull { zone ->
        try {
            // Get outer ring of polygon
            val coords = zone.geometry.coordinates.first() // List<List<Double>>
            
            // Find center (average of all points in the ring)
            val avgLat = coords.map { it[1] }.average()
            val avgLng = coords.map { it[0] }.average()
            
            // Weight based on risk score (0-100 -> 0-1)
            val weight = zone.properties.riskScore / 100.0
            
            WeightedLatLng(LatLng(avgLat, avgLng), weight)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Create heatmap gradient (green -> yellow -> red)
 */
fun createRiskGradient(): Gradient {
    val colors = intArrayOf(
        Color(0xFF00E676).toArgb(),  // GREEN - Low risk
        Color(0xFFFFEB3B).toArgb(),  // YELLOW - Medium risk
        Color(0xFFFF9800).toArgb(),  // ORANGE - High risk
        Color(0xFFFF3B30).toArgb()   // RED - Critical risk
    )
    
    val startPoints = floatArrayOf(
        0.0f,   // 0-25: Green
        0.25f,  // 25-50: Yellow
        0.50f,  // 50-75: Orange
        0.75f   // 75-100: Red
    )
    
    return Gradient(colors, startPoints)
}

/**
 * Create heatmap tile provider
 */
suspend fun createHeatmapProvider(
    weightedPoints: List<WeightedLatLng>,
    radius: Int = 50,
    opacity: Double = 0.6
): HeatmapTileProvider = withContext(Dispatchers.Default) {
    HeatmapTileProvider.Builder()
        .weightedData(weightedPoints)
        .radius(radius)
        .opacity(opacity)
        .gradient(createRiskGradient())
        .build()
}

/**
 * Get risk level color
 */
fun getRiskColor(riskScore: Double): Color {
    return when {
        riskScore >= 75.0 -> Color(0xFFFF3B30)  // CRITICAL - Red
        riskScore >= 50.0 -> Color(0xFFFF9800)  // HIGH - Orange
        riskScore >= 25.0 -> Color(0xFFFFEB3B)  // MEDIUM - Yellow
        else -> Color(0xFF00E676)                // LOW - Green
    }
}

/**
 * Get risk level text
 */
fun getRiskLevelText(riskScore: Double): String {
    return when {
        riskScore >= 75.0 -> "CRITICAL"
        riskScore >= 50.0 -> "HIGH"
        riskScore >= 25.0 -> "MEDIUM"
        else -> "LOW"
    }
}

/**
 * Calculate bounding box from camera position
 */
fun calculateBoundingBox(
    center: LatLng,
    zoomLevel: Float
): LatLngBounds {
    // Approximate degrees per pixel at different zoom levels
    val metersPerPixel = 156543.03392 * Math.cos(center.latitude * Math.PI / 180) / Math.pow(2.0, zoomLevel.toDouble())
    
    // Assume screen is ~1000 pixels
    val latDelta = (metersPerPixel * 1000) / 111000  // meters to degrees
    val lngDelta = (metersPerPixel * 1000) / (111000 * Math.cos(center.latitude * Math.PI / 180))
    
    return LatLngBounds(
        LatLng(center.latitude - latDelta, center.longitude - lngDelta),  // SW
        LatLng(center.latitude + latDelta, center.longitude + lngDelta)   // NE
    )
}
