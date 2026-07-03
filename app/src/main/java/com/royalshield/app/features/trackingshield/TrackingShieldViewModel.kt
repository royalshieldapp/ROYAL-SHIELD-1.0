package com.royalshield.app.features.trackingshield

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.royalshield.app.features.trackingshield.data.*
import com.royalshield.app.managers.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.royalshield.app.features.riskprediction.data.repository.RiskPredictionRepository
import com.royalshield.app.features.riskprediction.data.remote.Hotspot

class TrackingShieldViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = FamilyTrackingManager(application)
    private val riskRepo = RiskPredictionRepository()

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    // Camera target request (consumed once by the map)
    private val _centerRequest = MutableStateFlow<LatLng?>(null)
    val centerRequest: StateFlow<LatLng?> = _centerRequest.asStateFlow()

    private var playbackJob: Job? = null

    init {
        checkRole()
        // Only start tracking if we are PARENT or CHILD (logic tailored later)
        // For now, if role is set, we proceed.
        if (PreferencesManager.getDeviceRole() != "UNSET") {
             manager.startTracking()
        }
        observeData()
        startLastUpdateTicker()
        loadHotspots()
    }

    private fun checkRole() {
        val role = PreferencesManager.getDeviceRole()
        _uiState.value = _uiState.value.copy(deviceRole = role)
        
        if (role == "CHILD") {
             startLocationService()
        }
    }

    fun setRole(role: String) {
        PreferencesManager.setDeviceRole(role)
        _uiState.value = _uiState.value.copy(deviceRole = role)
        
        if (role == "PARENT") {
            manager.startTracking()
        } else if (role == "CHILD") {
            startLocationService()
        }
    }
    
    private fun startLocationService() {
        val context = com.royalshield.app.RoyalShieldApp.instance
        val intent = android.content.Intent(context, com.royalshield.app.services.LocationTrackerService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun observeData() {
        // 1. Observe Children (Real Data from Firebase)
        viewModelScope.launch {
            manager.repository.observeChildren().collect { childrenList ->
                val currentChildId = _uiState.value.child?.id
                val newChild = childrenList.find { it.id == currentChildId } ?: childrenList.firstOrNull()
                
                _uiState.value = _uiState.value.copy(
                     children = childrenList,
                     child = newChild,
                     isConnected = childrenList.isNotEmpty(),
                     isLoading = false
                )
            }
        }

        // 2. Observe Parent Location & Zones
        viewModelScope.launch {
            combine(
                manager.parentLocation,
                manager.safeZones,
                manager.riskZones
            ) { parentLoc, safe, risk ->
                Triple(parentLoc, safe, risk)
            }.collect { (parentLoc, safe, risk) ->
                _uiState.value = _uiState.value.copy(
                    parentLocation = parentLoc,
                    safeZones = safe,
                    riskZones = risk,
                    isLoading = parentLoc == null && _uiState.value.children.isEmpty()
                )
            }
        }

        // 3. Observe History Sessions
        viewModelScope.launch {
             manager.historySessions.collect { history ->
                 _uiState.value = _uiState.value.copy(historySessions = history)
             }
        }
    }

    private fun loadHotspots() {
        viewModelScope.launch {
            try {
                val result = riskRepo.getHotspots(null, null, null, null)
                result.onSuccess { response ->
                    _uiState.value = _uiState.value.copy(hotspots = response.hotspots)
                }.onFailure {
                    loadFallbackHotspots()
                }
            } catch (e: Exception) {
                loadFallbackHotspots()
            }
        }
    }

    private fun loadFallbackHotspots() {
        val baseLat = 25.7617
        val baseLng = -80.1918
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
        _uiState.value = _uiState.value.copy(hotspots = simulated)
    }

    /** Refreshes "last update: Xs ago" text every second */
    private fun startLastUpdateTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val child = _uiState.value.child ?: continue
                val ago = (System.currentTimeMillis() - child.lastUpdateMillis) / 1000
                val text = when {
                    ago < 60 -> "${ago}s ago"
                    ago < 3600 -> "${ago / 60}m ago"
                    else -> "${ago / 3600}h ago"
                }
                _uiState.value = _uiState.value.copy(lastUpdateText = text)
            }
        }
    }

    // ── Mode Switching ──────────────────────────────────────
    fun switchMode(mode: TrackingMode) {
        stopPlayback()
        _uiState.value = _uiState.value.copy(
            mode = mode,
            navRoute = if (mode == TrackingMode.NAV) generateNavRouteFromState() else null,
            activeSession = if (mode != TrackingMode.HISTORY) null else _uiState.value.activeSession,
            playbackProgress = 0f,
            isPlaybackPlaying = false
        )
    }

    private fun generateNavRouteFromState(): NavRoute? {
        val child = _uiState.value.child ?: return null
        val parent = _uiState.value.parentLocation ?: return null
        return manager.generateNavRoute(from = child.latLng, to = parent)
    }

    // ── History Playback ────────────────────────────────────
    fun selectHistorySession(session: HistorySession) {
        stopPlayback()
        _uiState.value = _uiState.value.copy(
            activeSession = session,
            playbackProgress = 0f,
            isPlaybackPlaying = false
        )
        // Center on first breadcrumb
        session.breadcrumbs.firstOrNull()?.let { _centerRequest.value = it.latLng }
    }

    fun togglePlayback() {
        val state = _uiState.value
        if (state.activeSession == null) return

        if (state.isPlaybackPlaying) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        _uiState.value = _uiState.value.copy(isPlaybackPlaying = true)
        playbackJob = viewModelScope.launch {
            while (_uiState.value.playbackProgress < 1f && _uiState.value.isPlaybackPlaying) {
                delay((50 / _uiState.value.playbackSpeed).toLong())
                val newProgress = (_uiState.value.playbackProgress + 0.002f).coerceAtMost(1f)
                _uiState.value = _uiState.value.copy(playbackProgress = newProgress)

                // Move camera to current breadcrumb position
                val session = _uiState.value.activeSession ?: break
                val index = (newProgress * (session.breadcrumbs.size - 1)).toInt()
                if (index < session.breadcrumbs.size) {
                    _centerRequest.value = session.breadcrumbs[index].latLng
                }

                if (newProgress >= 1f) {
                    _uiState.value = _uiState.value.copy(isPlaybackPlaying = false)
                }
            }
        }
    }

    private fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _uiState.value = _uiState.value.copy(isPlaybackPlaying = false)
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun cyclePlaybackSpeed() {
        val current = _uiState.value.playbackSpeed
        val next = when {
            current < 1.5f -> 2f
            current < 3f -> 4f
            else -> 1f
        }
        setPlaybackSpeed(next)
    }

    fun seekPlayback(progress: Float) {
        _uiState.value = _uiState.value.copy(playbackProgress = progress.coerceIn(0f, 1f))
        // Center on the breadcrumb at this position
        val session = _uiState.value.activeSession ?: return
        val index = (progress * (session.breadcrumbs.size - 1)).toInt()
        if (index < session.breadcrumbs.size) {
            _centerRequest.value = session.breadcrumbs[index].latLng
        }
    }

    // ── Existing actions ────────────────────────────────────
    fun centerOnChild() {
        _uiState.value.child?.let { _centerRequest.value = it.latLng }
    }

    fun consumeCenterRequest() {
        _centerRequest.value = null
    }

    fun selectChild(child: ChildDevice) {
        _uiState.value = _uiState.value.copy(child = child)
        // Also center on new child
        _centerRequest.value = child.latLng
    }

    fun generatePairingCode() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val code = manager.generatePairingCode()
                _uiState.value = _uiState.value.copy(pairingCode = code, isLoading = false)
            } catch (e: Exception) {
                // handle error
                _uiState.value = _uiState.value.copy(isLoading = false)
                e.printStackTrace()
            }
        }
    }

    fun dismissPairingDialog() {
        _uiState.value = _uiState.value.copy(pairingCode = null)
    }

    fun triggerSOS() {
        val context = getApplication<android.app.Application>()
        com.royalshield.app.SosManager.triggerSilentSos(context)
    }

    // ── Zone Management ─────────────────────────────────────
    fun toggleZoneSheet(open: Boolean) {
        _uiState.value = _uiState.value.copy(isZoneSheetOpen = open)
    }

    fun startAddZone(latLng: LatLng) {
        _uiState.value = _uiState.value.copy(
            isAddingZone = true,
            draftZone = SafeZone(
                name = "New Zone",
                center = latLng,
                radiusMeters = 200.0,
                type = ZoneType.HOME
            )
        )
    }

    fun updateDraftZone(name: String, radius: Double, type: ZoneType) {
        _uiState.value = _uiState.value.copy(
            draftZone = _uiState.value.draftZone?.copy(
                name = name,
                radiusMeters = radius,
                type = type
            )
        )
    }

    fun confirmAddZone() {
        val zone = _uiState.value.draftZone ?: return
        viewModelScope.launch {
            manager.repository.saveSafeZone(zone)
            _uiState.value = _uiState.value.copy(isAddingZone = false, draftZone = null)
        }
    }

    fun cancelAddZone() {
        _uiState.value = _uiState.value.copy(isAddingZone = false, draftZone = null)
    }

    fun deleteSafeZone(id: String) {
        viewModelScope.launch {
            manager.repository.deleteSafeZone(id)
        }
    }

    fun deleteRiskZone(id: String) {
        viewModelScope.launch {
            manager.repository.deleteRiskZone(id)
        }
    }

    // ── Control Panel ───────────────────────────────────────
    fun toggleControlPanel(open: Boolean) {
        _uiState.value = _uiState.value.copy(isControlPanelOpen = open)
    }

    fun setGpsMode(mode: GpsMode) {
        viewModelScope.launch {
            val childId = _uiState.value.child?.id ?: return@launch
            manager.repository.updateGpsMode(childId, mode)
        }
    }

    fun refreshChildLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            delay(1500) // Simulate network request for manual refresh
            _uiState.value = _uiState.value.copy(isLoading = false)
            // Actual refresh logic would talk to the remote device/server
        }
    }

    fun playAlertSound(childId: String) {
        viewModelScope.launch {
            try {
                manager.repository.triggerAlertSound(childId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        manager.stopTracking()
    }
}
