package com.royalshield.app.features.trackingshield.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages family tracking: parent GPS + simulated child data.
 * In production, child data comes from Firebase RTDB listeners.
 */
class FamilyTrackingManager(private val context: Context) {

    companion object {
        private const val TAG = "FamilyTrackingManager"
        private const val UPDATE_INTERVAL_MS = 5000L
    }

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    val repository = FamilyRepository(context)

    // ── Parent Location ─────────────────────────────────────
    private val _parentLocation = MutableStateFlow<LatLng?>(null)
    val parentLocation: StateFlow<LatLng?> = _parentLocation.asStateFlow()

    // ── Child Device ────────────────────────────────────────
    private val _childDevice = MutableStateFlow<ChildDevice?>(null)
    val childDevice: StateFlow<ChildDevice?> = _childDevice.asStateFlow()

    // ── Safe Zones ──────────────────────────────────────────
    private val _safeZones = MutableStateFlow<List<SafeZone>>(emptyList())
    val safeZones: StateFlow<List<SafeZone>> = _safeZones.asStateFlow()

    // ── Risk Zones ──────────────────────────────────────────
    private val _riskZones = MutableStateFlow<List<RiskZone>>(emptyList())
    val riskZones: StateFlow<List<RiskZone>> = _riskZones.asStateFlow()

    /**
     * Starts parent location tracking + loads simulated child/zone data.
     * In production: attach Firebase RTDB listeners for child device.
     */
    @SuppressLint("MissingPermission")
    fun startTracking() {
        // Parent GPS
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val latLng = LatLng(loc.latitude, loc.longitude)
                    _parentLocation.value = latLng
                    // Child data now comes from actual Firebase observation via repository
                    // No more simulation here
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing location permission", e)
        }

        // Observe Real Zones from Repository
        CoroutineScope(Dispatchers.Main).launch {
            repository.observeSafeZones().collect { zones ->
                _safeZones.value = zones
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            repository.observeRiskZones().collect { risks ->
                _riskZones.value = risks
            }
        }
    }

    fun stopTracking() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    // ── History Sessions ─────────────────────────────────────
    private val _historySessions = MutableStateFlow<List<HistorySession>>(emptyList())
    val historySessions: StateFlow<List<HistorySession>> = _historySessions.asStateFlow()

    suspend fun generatePairingCode(): String {
        return repository.generatePairingCode()
    }

    // ── Simulation helpers (replace with Firebase in production) ──

    private fun simulateChildData(parentPos: LatLng) {
        // Method kept for safety but functionality now overridden by real observation
    }

    private fun computeChildStatus(childPos: LatLng): ChildStatus {
        val zones = _safeZones.value
        if (zones.isEmpty()) return ChildStatus.SAFE

        val insideSafeZone = zones.any { zone ->
            zone.isEnabled && distanceMeters(childPos, zone.center) <= zone.radiusMeters
        }
        val insideRiskZone = _riskZones.value.any { risk ->
            distanceMeters(childPos, risk.center) <= risk.radiusMeters
        }

        return when {
            insideRiskZone -> ChildStatus.DANGER
            !insideSafeZone -> ChildStatus.WARNING
            else -> ChildStatus.SAFE
        }
    }

    private fun loadDemoZones(base: LatLng) {
        _safeZones.value = listOf(
            SafeZone(
                id = "zone_home",
                name = "Home",
                center = LatLng(base.latitude + 0.001, base.longitude + 0.001),
                radiusMeters = 250.0,
                type = ZoneType.HOME
            ),
            SafeZone(
                id = "zone_school",
                name = "School",
                center = LatLng(base.latitude + 0.005, base.longitude - 0.003),
                radiusMeters = 300.0,
                type = ZoneType.SCHOOL
            ),
            SafeZone(
                id = "zone_park",
                name = "Central Park",
                center = LatLng(base.latitude - 0.003, base.longitude + 0.004),
                radiusMeters = 200.0,
                type = ZoneType.PARK
            )
        )

        _riskZones.value = listOf(
            RiskZone(
                id = "risk_01",
                name = "High Crime Area",
                center = LatLng(base.latitude - 0.006, base.longitude - 0.005),
                radiusMeters = 350.0,
                threatLevel = 4
            )
        )

        // Load demo history sessions
        loadDemoHistory(base)
    }

    // ── History simulation ──────────────────────────────────
    private fun loadDemoHistory(base: LatLng) {
        val now = System.currentTimeMillis()
        val oneDay = 86_400_000L

        _historySessions.value = listOf(
            generateSession("s_today", "Today", "Feb 18, 2026", base, now - 3_600_000, 30),
            generateSession("s_yesterday", "Yesterday", "Feb 17, 2026",
                LatLng(base.latitude + 0.003, base.longitude - 0.002), now - oneDay, 25),
            generateSession("s_2days", "2 days ago", "Feb 16, 2026",
                LatLng(base.latitude - 0.002, base.longitude + 0.005), now - 2 * oneDay, 35)
        )
    }

    private fun generateSession(
        id: String, label: String, date: String,
        start: LatLng, startTime: Long, pointCount: Int
    ): HistorySession {
        val breadcrumbs = mutableListOf<LocationBreadcrumb>()
        var lat = start.latitude
        var lng = start.longitude
        val stepDelta = 0.0002  // ~22m per step

        for (i in 0 until pointCount) {
            // Simulate walking with slight direction changes
            val angle = Math.toRadians((i * 37.0) % 360)
            lat += stepDelta * kotlin.math.cos(angle)
            lng += stepDelta * kotlin.math.sin(angle) * 0.8

            val speed = when {
                i < 5 -> 3f + (i * 0.2f)         // slow start
                i > pointCount - 5 -> 2f          // slow end
                else -> 4f + (i % 3) * 1.5f       // variable walking
            }

            breadcrumbs.add(LocationBreadcrumb(
                latLng = LatLng(lat, lng),
                timestamp = startTime + (i * 120_000L), // 2min intervals
                speedKmh = speed,
                batteryPercent = 100 - (i * 100 / pointCount)
            ))
        }

        val totalDist = breadcrumbs.zipWithNext().sumOf { (a, b) ->
            distanceMeters(a.latLng, b.latLng)
        } / 1000.0

        return HistorySession(
            id = id, label = label, date = date,
            breadcrumbs = breadcrumbs,
            distanceKm = totalDist.toFloat(),
            durationMinutes = (pointCount * 2)
        )
    }

    // ── Nav Route simulation ────────────────────────────────
    fun generateNavRoute(from: LatLng, to: LatLng): NavRoute {
        val midLat = (from.latitude + to.latitude) / 2
        val midLng = (from.longitude + to.longitude) / 2

        val waypoints = listOf(
            from,
            LatLng(from.latitude + 0.001, from.longitude + 0.0015),
            LatLng(midLat + 0.0008, midLng - 0.0005),
            LatLng(to.latitude - 0.001, to.longitude - 0.0008),
            to
        )

        val dist = waypoints.zipWithNext().sumOf { (a, b) ->
            distanceMeters(a, b)
        } / 1000.0

        return NavRoute(
            destination = to,
            destinationName = "Home",
            waypoints = waypoints,
            distanceKm = dist.toFloat(),
            etaMinutes = (dist / 4.0 * 60).toInt().coerceAtLeast(1), // ~4km/h walking
            currentInstruction = "Head north on Main St"
        )
    }

    private fun distanceMeters(a: LatLng, b: LatLng): Double {
        val results = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0].toDouble()
    }
}
