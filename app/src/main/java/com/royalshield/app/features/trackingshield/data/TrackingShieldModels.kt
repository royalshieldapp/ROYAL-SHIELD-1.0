package com.royalshield.app.features.trackingshield.data

import com.google.android.gms.maps.model.LatLng
import com.royalshield.app.features.riskprediction.data.remote.Hotspot
import com.royalshield.app.features.riskprediction.data.remote.RiskFeature

// ── Child Status ────────────────────────────────────────────
enum class ChildStatus {
    SAFE,      // Green halo — active, inside safe zone
    WARNING,   // Yellow halo — stale update / low battery
    DANGER     // Red halo — outside zone / SOS / no signal
}

// ── Zone Types ──────────────────────────────────────────────
enum class ZoneType {
    HOME,    // Cyan #00E5FF
    SCHOOL,  // Gold #FFD700
    PARK,    // Green #00FF9C
    CUSTOM   // Purple #BF5AF2
}

// ── GPS Accuracy Mode ───────────────────────────────────────
enum class GpsMode {
    HIGH_ACCURACY,
    BALANCED,
    BATTERY_SAVER
}

// ── Tracking Screen Mode ────────────────────────────────────
enum class TrackingMode {
    LIVE,
    NAV,
    HISTORY
}

// ── Data Classes ────────────────────────────────────────────

data class ChildDevice(
    val id: String = "",
    val name: String = "Child",
    val latLng: LatLng = LatLng(0.0, 0.0),
    val batteryPercent: Int = 100,
    val signalStrength: Int = 4, // 0-4 bars
    val gpsActive: Boolean = true,
    val lastUpdateMillis: Long = System.currentTimeMillis(),
    val status: ChildStatus = ChildStatus.SAFE,
    val accuracyMeters: Float = 10f,
    val speedKmh: Float = 0f,
    val gpsMode: GpsMode = GpsMode.BALANCED
)

data class SafeZone(
    val id: String = "",
    val name: String = "",
    val center: LatLng = LatLng(0.0, 0.0),
    val radiusMeters: Double = 200.0,
    val type: ZoneType = ZoneType.HOME,
    val isEnabled: Boolean = true
)

data class RiskZone(
    val id: String = "",
    val name: String = "",
    val center: LatLng = LatLng(0.0, 0.0),
    val radiusMeters: Double = 300.0,
    val threatLevel: Int = 1 // 1-5
)

// ── Location Breadcrumb (History) ───────────────────────────
data class LocationBreadcrumb(
    val latLng: LatLng,
    val timestamp: Long,        // epoch millis
    val speedKmh: Float = 0f,
    val batteryPercent: Int = 100
)

// ── History Session ─────────────────────────────────────────
data class HistorySession(
    val id: String,
    val label: String,           // "Today", "Yesterday"
    val date: String,            // "Feb 18, 2026"
    val breadcrumbs: List<LocationBreadcrumb>,
    val distanceKm: Float = 0f,
    val durationMinutes: Int = 0
)

// ── Nav Route ───────────────────────────────────────────────
data class NavRoute(
    val destination: LatLng,
    val destinationName: String,
    val waypoints: List<LatLng>,
    val distanceKm: Float,
    val etaMinutes: Int,
    val currentInstruction: String  // "Turn right on Main St"
)

// ── UI State ────────────────────────────────────────────────
data class TrackingUiState(
    val deviceRole: String = "UNSET", // PARENT, CHILD, UNSET
    val pairingCode: String? = null,
    val mode: TrackingMode = TrackingMode.LIVE,
    val child: ChildDevice? = null, // Selected child
    val children: List<ChildDevice> = emptyList(), // All children
    val safeZones: List<SafeZone> = emptyList(),
    val riskZones: List<RiskZone> = emptyList(),
    val parentLocation: LatLng? = null,
    val isConnected: Boolean = true,
    val lastUpdateText: String = "—",
    val isLoading: Boolean = true,
    // Phase 2 — Nav Mode
    val navRoute: NavRoute? = null,
    // Phase 2 — History Playback
    val historySessions: List<HistorySession> = emptyList(),
    val activeSession: HistorySession? = null,
    val playbackProgress: Float = 0f,       // 0..1
    val isPlaybackPlaying: Boolean = false,
    val playbackSpeed: Float = 1f,           // 1x, 2x, 4x
    
    // Phase 3 — Zone Management
    val isAddingZone: Boolean = false,
    val draftZone: SafeZone? = null,
    val isZoneSheetOpen: Boolean = false,
    
    // Phase 3 — Control Panel
    val isControlPanelOpen: Boolean = false,
    
    // Forecast Engine Risk Data
    val hotspots: List<Hotspot> = emptyList(),
    val riskFeatures: List<RiskFeature> = emptyList()
)
