package com.royalshield.app.models

enum class PhishingRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class PhishingDetectionResult(
    val riskLevel: PhishingRiskLevel,
    val explanations: List<String>
)
