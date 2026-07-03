package com.royalshield.app.features.riskprediction.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.royalshield.app.features.riskprediction.data.remote.Hotspot
import com.royalshield.app.features.riskprediction.data.remote.RiskFeature
import com.royalshield.app.features.riskprediction.data.repository.RiskPredictionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Risk Map Feature
 * Manages risk heatmap, hotspots, and zone details
 */
class RiskMapViewModel : ViewModel() {
    
    private val repository = RiskPredictionRepository()
    private val TAG = "RiskMapViewModel"
    
    // State
    private val _uiState = MutableStateFlow(RiskMapUiState())
    val uiState: StateFlow<RiskMapUiState> = _uiState.asStateFlow()
    
    /**
     * Load risk map for current viewport
     */
    fun loadRiskMap(
        minLat: Double,
        minLng: Double,
        maxLat: Double,
        maxLng: Double
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val result = repository.getRiskMap(minLat, minLng, maxLat, maxLng)
                
                result.onSuccess { response ->
                    Log.d(TAG, "Risk map loaded: ${response.features.size} zones")
                    _uiState.value = _uiState.value.copy(
                        riskZones = response.features,
                        isLoading = false,
                        lastUpdate = System.currentTimeMillis()
                    )
                }.onFailure { error ->
                    Log.e(TAG, "Failed to load risk map: ${error.message}", error)
                    
                    // Offline Fallback: Generate mock risk zones to populate the heatmap
                    val baseLat = (minLat + maxLat) / 2
                    val baseLng = (minLng + maxLng) / 2
                    val simulatedZones = listOf(
                        RiskFeature(
                            type = "Feature",
                            properties = com.royalshield.app.features.riskprediction.data.remote.RiskProperties(
                                h3Cell = "sim_cell1",
                                riskScore = 0.95,
                                riskLevel = "CRITICAL",
                                eventCount = 10,
                                crimeCount = 5,
                                fireCount = 0,
                                recent7d = 2,
                                recent30d = 8
                            ),
                            geometry = com.royalshield.app.features.riskprediction.data.remote.Geometry(
                                type = "Polygon",
                                coordinates = listOf(
                                    listOf(
                                        listOf(baseLng - 0.015, baseLat - 0.015),
                                        listOf(baseLng - 0.015, baseLat + 0.015),
                                        listOf(baseLng + 0.015, baseLat + 0.015),
                                        listOf(baseLng + 0.015, baseLat - 0.015),
                                        listOf(baseLng - 0.015, baseLat - 0.015)
                                    )
                                )
                            )
                        ),
                        RiskFeature(
                            type = "Feature",
                            properties = com.royalshield.app.features.riskprediction.data.remote.RiskProperties(
                                h3Cell = "sim_cell2",
                                riskScore = 0.75,
                                riskLevel = "HIGH",
                                eventCount = 6,
                                crimeCount = 3,
                                fireCount = 1,
                                recent7d = 1,
                                recent30d = 4
                            ),
                            geometry = com.royalshield.app.features.riskprediction.data.remote.Geometry(
                                type = "Polygon",
                                coordinates = listOf(
                                    listOf(
                                        listOf(baseLng - 0.005, baseLat - 0.025),
                                        listOf(baseLng - 0.005, baseLat - 0.005),
                                        listOf(baseLng + 0.025, baseLat - 0.005),
                                        listOf(baseLng + 0.025, baseLat - 0.025),
                                        listOf(baseLng - 0.005, baseLat - 0.025)
                                    )
                                )
                            )
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        riskZones = simulatedZones,
                        isLoading = false,
                        error = error.message ?: "Failed to load risk map"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    /**
     * Load hotspots
     */
    fun loadHotspots(
        minLat: Double? = null,
        minLng: Double? = null,
        maxLat: Double? = null,
        maxLng: Double? = null
    ) {
        viewModelScope.launch {
            try {
                val result = repository.getHotspots(minLat, minLng, maxLat, maxLng)
                
                result.onSuccess { response ->
                    Log.d(TAG, "Hotspots loaded: ${response.hotspots.size} clusters")
                    _uiState.value = _uiState.value.copy(
                        hotspots = response.hotspots
                    )
                }.onFailure { error ->
                    Log.e(TAG, "Failed to load hotspots: ${error.message}", error)
                    
                    // Offline Fallback: Generate simulated hotspots near the user location
                    val baseLat = minLat?.let { (minLat + (maxLat ?: minLat)) / 2 } ?: 25.7617
                    val baseLng = minLng?.let { (minLng + (maxLng ?: minLng)) / 2 } ?: -80.1918
                    val simulated = listOf(
                        Hotspot(
                            hotspotId = 1,
                            center = com.royalshield.app.features.riskprediction.data.remote.HotspotCenter(baseLat + 0.005, baseLng - 0.005),
                            h3Cell = "sim_cell_h1",
                            radiusMeters = 500.0,
                            eventCount = 15,
                            eventTypes = emptyMap(),
                            severities = emptyMap(),
                            riskScore = 0.9,
                            riskLevel = "CRITICAL"
                        ),
                        Hotspot(
                            hotspotId = 2,
                            center = com.royalshield.app.features.riskprediction.data.remote.HotspotCenter(baseLat - 0.008, baseLng + 0.006),
                            h3Cell = "sim_cell_h2",
                            radiusMeters = 400.0,
                            eventCount = 8,
                            eventTypes = emptyMap(),
                            severities = emptyMap(),
                            riskScore = 0.7,
                            riskLevel = "HIGH"
                        ),
                        Hotspot(
                            hotspotId = 3,
                            center = com.royalshield.app.features.riskprediction.data.remote.HotspotCenter(baseLat + 0.003, baseLng + 0.009),
                            h3Cell = "sim_cell_h3",
                            radiusMeters = 300.0,
                            eventCount = 4,
                            eventTypes = emptyMap(),
                            severities = emptyMap(),
                            riskScore = 0.5,
                            riskLevel = "MEDIUM"
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        hotspots = simulated
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading hotspots: ${e.message}", e)
                
                val baseLat = minLat?.let { (minLat + (maxLat ?: minLat)) / 2 } ?: 25.7617
                val baseLng = minLng?.let { (minLng + (maxLng ?: minLng)) / 2 } ?: -80.1918
                val simulated = listOf(
                    Hotspot(
                        hotspotId = 1,
                        center = com.royalshield.app.features.riskprediction.data.remote.HotspotCenter(baseLat + 0.005, baseLng - 0.005),
                        h3Cell = "sim_cell_h1",
                        radiusMeters = 500.0,
                        eventCount = 15,
                        eventTypes = emptyMap(),
                        severities = emptyMap(),
                        riskScore = 0.9,
                        riskLevel = "CRITICAL"
                    )
                )
                _uiState.value = _uiState.value.copy(
                    hotspots = simulated
                )
            }
        }
    }
    
    /**
     * Toggle heatmap layer
     */
    fun toggleHeatmap() {
        _uiState.value = _uiState.value.copy(
            showHeatmap = !_uiState.value.showHeatmap
        )
    }
    
    /**
     * Toggle hotspots layer
     */
    fun toggleHotspots() {
        _uiState.value = _uiState.value.copy(
            showHotspots = !_uiState.value.showHotspots
        )
    }
    
    /**
     * Select zone for details
     */
    fun selectZone(h3Cell: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val result = repository.getZoneDetails(h3Cell)
                
                result.onSuccess { details ->
                    Log.d(TAG, "Zone details loaded for $h3Cell")
                    _uiState.value = _uiState.value.copy(
                        selectedZone = details,
                        isLoading = false
                    )
                }.onFailure { error ->
                    Log.e(TAG, "Failed to load zone details: ${error.message}", error)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading zone details: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    /**
     * Handle map click
     */
    fun onMapClick(lat: Double, lng: Double) {
        val currentZones = _uiState.value.riskZones
        
        // Find which zone contains this point
        val clickedZone = currentZones.find { zone ->
            // Assuming Polygon geometry type for now
            if (zone.geometry.type == "Polygon") {
                // outer ring is at index 0
                val polygon = zone.geometry.coordinates.firstOrNull() ?: emptyList()
                isPointInPolygon(lat, lng, polygon)
            } else {
                false
            }
        }
        
        if (clickedZone != null) {
            Log.d(TAG, "Clicked zone: ${clickedZone.properties.h3Cell}")
            selectZone(clickedZone.properties.h3Cell)
        } else {
            clearSelection()
        }
    }
    
    /**
     * Ray casting algorithm for point in polygon
     * polygon is list of [lng, lat]
     */
    private fun isPointInPolygon(lat: Double, lng: Double, polygon: List<List<Double>>): Boolean {
        var isInside = false
        var j = polygon.size - 1
        
        for (i in polygon.indices) {
            val p1 = polygon[i]
            val p2 = polygon[j]
            
            // p[0] is lung, p[1] is lat
            val lng1 = p1[0]
            val lat1 = p1[1]
            val lng2 = p2[0]
            val lat2 = p2[1]
            
            if (((lat1 > lat) != (lat2 > lat)) &&
                (lng < (lng2 - lng1) * (lat - lat1) / (lat2 - lat1) + lng1)) {
                isInside = !isInside
            }
            j = i
        }
        return isInside
    }

    /**
     * Clear selected zone
     */
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedZone = null)
    }
}

/**
 * UI State for Risk Map
 */
data class RiskMapUiState(
    val riskZones: List<RiskFeature> = emptyList(),
    val hotspots: List<Hotspot> = emptyList(),
    val selectedZone: com.royalshield.app.features.riskprediction.data.remote.ZoneDetailsResponse? = null,
    val showHeatmap: Boolean = true,
    val showHotspots: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdate: Long = 0L
)
