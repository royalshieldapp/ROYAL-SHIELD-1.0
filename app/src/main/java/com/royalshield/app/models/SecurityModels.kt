package com.royalshield.app.models

import android.graphics.drawable.Drawable

enum class RiskLevel {
    LOW, MEDIUM, HIGH
}

data class AppScanResult(
    val appName: String,
    val packageName: String,
    val appIcon: Drawable?,
    val permissions: List<String>,
    val riskScore: Int
) {
    val riskLevel: RiskLevel get() = when {
        riskScore >= 50 -> RiskLevel.HIGH
        riskScore >= 20 -> RiskLevel.MEDIUM
        else -> RiskLevel.LOW
    }
}

data class AppRiskMetrics(
    val totalApps: Int = 0,
    val avgRiskScore: Double = 0.0,
    val minRiskScore: Int = 0,
    val maxRiskScore: Int = 0,
    val low: Int = 0,
    val medium: Int = 0,
    val high: Int = 0,
    val totalPermissions: Int = 0,
    val dangerousPermissions: Int = 0,
    val topDangerousPermissions: List<PermissionCount> = emptyList(),
    val byPermission: Map<String, PermissionStats> = emptyMap()
)

data class PermissionCount(
    val permission: String,
    val count: Int
)

data class PermissionStats(
    val frequency: Int,
    val averageRiskContribution: Double
)

data class AppScanSummary(
    val apps: List<AppScanResult>,
    val metrics: AppRiskMetrics,
    val scanTimestamp: String
)
