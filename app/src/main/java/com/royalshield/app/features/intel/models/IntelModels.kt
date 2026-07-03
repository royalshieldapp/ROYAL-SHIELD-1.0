package com.royalshield.app.features.intel.models

import com.google.firebase.Timestamp

enum class IocType {
    IP, DOMAIN, URL, HASH
}

enum class ConfidenceLevel {
    LOW, MEDIUM, HIGH
}

enum class AlertCategory {
    PHISHING, RANSOMWARE, MALWARE, CREDENTIAL_LEAK, OTHER,
    MALICIOUS_FILE, UNUSUAL_OUTBOUND_CONNECTION, BRUTE_FORCE, PRIVILEGE_ESCALATION,
    KEYLOGGER_DETECTED, UNSECURE_WIFI, CAMERA_MIC_HIJACKING
}

data class Monitor(
    val id: String = "",
    val value: String = "",
    val type: String = "EMAIL", // "EMAIL" or "DOMAIN"
    val createdAt: Timestamp = Timestamp.now()
)

data class IOC(
    val id: String = "",
    val type: IocType = IocType.DOMAIN,
    val value: String = "",
    val confidence: ConfidenceLevel = ConfidenceLevel.MEDIUM,
    val notes: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class IntelAlert(
    val id: String = "",
    val title: String = "",
    val category: AlertCategory = AlertCategory.OTHER,
    val severity: String = "MEDIUM",
    val description: String = "",
    val recommendedActions: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
)

data class AppSettings(
    val complianceMode: Boolean = true
)
