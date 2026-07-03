package com.royalshield.app.ui.dashboard.models

import androidx.compose.ui.graphics.Color

data class ThreatEvent(
    val id: String,
    val title: String,
    val severity: Severity,
    val angleDeg: Float, // 0..360
    val radius: Float,   // 0..1
    val timestamp: Long
)

data class ActionItem(
    val id: String,
    val title: String,
    val description: String,
    val severity: Severity,
    val status: String = "Open"
)

data class Connector(
    val name: String,
    val isActive: Boolean,
    val iconRes: Int? = null // Optional icon resource
)

data class KpiMetrics(
    val investigationsTotal: Int,
    val lowSeverity: Int,
    val mediumSeverity: Int,
    val highSeverity: Int,
    val exposedEntities: Int,
    val entitiesDelta: Int // e.g. +5 in last 24h
)

enum class Severity(val label: String, val color: Color) {
    LOW("Low", Color(0xFF4CAF50)),      // Green
    MEDIUM("Medium", Color(0xFFFFC107)), // Amber
    HIGH("High", Color(0xFFF44336)),     // Red
    CRITICAL("Critical", Color(0xFFD32F2F)) // Dark Red
}
