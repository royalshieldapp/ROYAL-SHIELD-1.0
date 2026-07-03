package com.royalshield.app.features.ir.models

import com.google.firebase.Timestamp

enum class IncidentType {
    RANSOMWARE, PHISHING_BEC, DATA_BREACH, MALWARE, OTHER
}

enum class IncidentSeverity {
    LOW, MEDIUM, HIGH
}

enum class IncidentStatus {
    ACTIVE, MITIGATED, RESOLVED, CLOSED
}

data class Incident(
    val id: String = "",
    val title: String = "",
    val type: IncidentType = IncidentType.OTHER,
    val severity: IncidentSeverity = IncidentSeverity.MEDIUM,
    val status: IncidentStatus = IncidentStatus.ACTIVE,
    val affectedAssets: String = "",
    val notes: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class TimelineEvent(
    val id: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val actionTaken: String = "",
    val who: String = "",
    val evidenceLink: String? = null
)

data class ChecklistItem(
    val id: String = "",
    val title: String = "",
    val isCompleted: Boolean = false,
    val type: IncidentType = IncidentType.OTHER
)

data class Asset(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val priority: String = "MEDIUM"
)
