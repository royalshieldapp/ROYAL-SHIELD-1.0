package com.royalshield.app.features.riskprediction.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.features.riskprediction.data.remote.Hotspot
import com.royalshield.app.features.riskprediction.data.remote.RiskFeature
import com.royalshield.app.features.riskprediction.data.remote.ZoneDetailsResponse
import com.royalshield.app.features.riskprediction.data.repository.RiskPredictionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

fun unavailableRiskMapState(current: RiskMapUiState, reason: String): RiskMapUiState =
    current.copy(
        riskZones = emptyList(),
        hotspots = emptyList(),
        selectedZone = null,
        isLoading = false,
        error = reason,
        lastUpdate = 0L,
    )

class RiskMapViewModel : ViewModel() {
    private val repository = RiskPredictionRepository()
    private val _uiState = MutableStateFlow(RiskMapUiState())
    val uiState: StateFlow<RiskMapUiState> = _uiState.asStateFlow()

    fun loadRiskMap(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getRiskMap(minLat, minLng, maxLat, maxLng)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        riskZones = response.features,
                        isLoading = false,
                        lastUpdate = System.currentTimeMillis(),
                    )
                }
                .onFailure { error ->
                    Log.e(TAG, "Verified risk-map source unavailable", error)
                    _uiState.value = unavailableRiskMapState(
                        _uiState.value,
                        "RISK_MAP_DATA_NOT_VERIFIED",
                    )
                }
        }
    }

    fun loadHotspots(
        minLat: Double? = null,
        minLng: Double? = null,
        maxLat: Double? = null,
        maxLng: Double? = null,
    ) {
        viewModelScope.launch {
            repository.getHotspots(minLat, minLng, maxLat, maxLng)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        hotspots = response.hotspots,
                        error = null,
                    )
                }
                .onFailure { error ->
                    Log.e(TAG, "Verified hotspot source unavailable", error)
                    _uiState.value = unavailableRiskMapState(
                        _uiState.value,
                        "HOTSPOT_DATA_NOT_VERIFIED",
                    )
                }
        }
    }

    fun toggleHeatmap() {
        _uiState.value = _uiState.value.copy(showHeatmap = !_uiState.value.showHeatmap)
    }

    fun toggleHotspots() {
        _uiState.value = _uiState.value.copy(showHotspots = !_uiState.value.showHotspots)
    }

    fun selectZone(h3Cell: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedZone = null)
            repository.getZoneDetails(h3Cell)
                .onSuccess { details ->
                    _uiState.value = _uiState.value.copy(
                        selectedZone = details,
                        isLoading = false,
                        error = null,
                    )
                }
                .onFailure { error ->
                    Log.e(TAG, "Verified zone details unavailable", error)
                    _uiState.value = _uiState.value.copy(
                        selectedZone = null,
                        isLoading = false,
                        error = "RISK_MAP_DATA_NOT_VERIFIED",
                    )
                }
        }
    }

    fun onMapClick(lat: Double, lng: Double) {
        val clickedZone = _uiState.value.riskZones.find { zone ->
            if (zone.geometry.type != "Polygon") return@find false
            val polygon = zone.geometry.coordinates.firstOrNull() ?: return@find false
            isPointInPolygon(lat, lng, polygon)
        }
        clickedZone?.let { selectZone(it.properties.h3Cell) } ?: clearSelection()
    }

    private fun isPointInPolygon(lat: Double, lng: Double, polygon: List<List<Double>>): Boolean {
        var inside = false
        var previous = polygon.size - 1
        for (current in polygon.indices) {
            val first = polygon[current]
            val second = polygon[previous]
            if (first.size < 2 || second.size < 2) continue
            val crosses = (first[1] > lat) != (second[1] > lat) &&
                lng < (second[0] - first[0]) * (lat - first[1]) /
                (second[1] - first[1]) + first[0]
            if (crosses) inside = !inside
            previous = current
        }
        return inside
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedZone = null)
    }

    private companion object {
        const val TAG = "RiskMapViewModel"
    }
}

data class RiskMapUiState(
    val riskZones: List<RiskFeature> = emptyList(),
    val hotspots: List<Hotspot> = emptyList(),
    val selectedZone: ZoneDetailsResponse? = null,
    val showHeatmap: Boolean = true,
    val showHotspots: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdate: Long = 0L,
)
