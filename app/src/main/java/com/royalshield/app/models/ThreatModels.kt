package com.royalshield.app.models

import androidx.compose.ui.graphics.vector.ImageVector

enum class ThreatType {
    THEFT, MANIFESTATION, SUSPICIOUS
}

data class ThreatAlert(
    val id: String,
    val type: ThreatType,
    val label: String,
    val lat: Double,
    val lon: Double,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TrafficAlertType {
    TRAFFIC_CAMERA, POLICE, SPEED_TRAP, ACCIDENT, TRAFFIC_JAM, ROAD_HAZARD
}

data class TrafficAlert(
    val id: String,
    val type: TrafficAlertType,
    val label: String,
    val lat: Double,
    val lon: Double,
    val description: String,
    val icon: ImageVector
)
