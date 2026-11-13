@file:OptIn(MapsComposeExperimentalApi::class, ExperimentalMaterial3Api::class)

package com.royalshield.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.royalshield.app.R
import com.royalshield.app.data.MapReminder
import com.royalshield.app.data.AvailabilityState
import com.royalshield.app.features.riskprediction.data.models.RiskMapStatus
import com.royalshield.app.features.riskprediction.data.models.isRenderableOnMap
import com.royalshield.app.features.riskprediction.ui.RiskMapViewModel
import com.royalshield.app.features.trackingshield.TrackingShieldViewModel
import com.royalshield.app.features.trackingshield.data.GpsMode
import com.royalshield.app.features.trackingshield.data.TrackingMode
import com.royalshield.app.managers.PreferencesManager
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.components.RoyalMapZoneDetailsSheet
import com.royalshield.app.ui.components.verifiedItems
import com.royalshield.app.ui.viewmodels.GlobalMapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.*

private val MapBlack = Color(0xFF050505)
private val MapPanel = Color(0xE6090909)
private val MapGreen = Color(0xFF00FF94)
private val MapCyan = Color(0xFF00E5FF)
private val MapRed = Color(0xFFFF3B30)
private val MapCaution = Color(0xFFFFD54F)

@SuppressLint("MissingPermission")
@Composable
fun RoyalMapScreen(
    onBack: () -> Unit = {},
    forecastAvailability: AvailabilityState = AvailabilityState.Unavailable("STATUS_UNVERIFIED"),
    threatMapAvailability: AvailabilityState = AvailabilityState.Unavailable("STATUS_UNVERIFIED"),
    trackingVm: TrackingShieldViewModel = viewModel(),
    globalVm: GlobalMapViewModel = viewModel(),
    riskVm: RiskMapViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val trackingState by trackingVm.uiState.collectAsState()
    val centerRequest by trackingVm.centerRequest.collectAsState()
    val reminders by globalVm.reminders.collectAsState()
    val riskState by riskVm.uiState.collectAsState()
    var timeline by remember { mutableStateOf(PreferencesManager.getLocationTimeline()) }
    var selectedTab by remember { mutableStateOf(RoyalMapTab.CONTROL) }
    var showAddReminder by remember { mutableStateOf(false) }
    var showTimeline by remember { mutableStateOf(false) }
    var showAgentDialog by remember { mutableStateOf(false) }
    var showCameras by remember { mutableStateOf(true) }
    var showPolice by remember { mutableStateOf(true) }
    var showSpeed by remember { mutableStateOf(true) }
    var showProtest by remember { mutableStateOf(true) }
    var showTheft by remember { mutableStateOf(true) }
    var showCyber by remember { mutableStateOf(true) }
    var is3D by remember { mutableStateOf(false) }
    var isDarkMode by remember { mutableStateOf(true) }
    var isGoogleMapLoaded by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var lastRecordedLocation by remember { mutableStateOf<LatLng?>(null) }
    var lastGpsUpdate by remember { mutableStateOf("--") }
    var selectedFamilyZone by remember { mutableStateOf<RoyalMapZoneDetails?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var isSystemLocationEnabled by remember { mutableStateOf(isDeviceLocationEnabled(context)) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isSystemLocationEnabled = isDeviceLocationEnabled(context)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(isSystemLocationEnabled, hasLocationPermission) {
        if (isSystemLocationEnabled && !hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(isSystemLocationEnabled, hasLocationPermission) {
        if (isSystemLocationEnabled && hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location ?: return@addOnSuccessListener
                currentLocation = LatLng(location.latitude, location.longitude)
                lastGpsUpdate = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(java.util.Date(location.time))
            }
        }
    }

    LaunchedEffect(trackingState.deviceRole, hasLocationPermission) {
        if (trackingState.deviceRole == "CHILD" && hasLocationPermission) {
            trackingVm.startChildLocationService()
        }
    }

    val defaultCenter = LatLng(25.7617, -80.1918)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 13f)
    }
    val mapStyle = remember(isDarkMode) {
        val styleRes = if (isDarkMode) R.raw.map_style_dark else R.raw.royal_grayscale_map_style
        runCatching { MapStyleOptions.loadRawResourceStyle(context, styleRes) }.getOrNull()
    }
    val mapSignals = remember(trackingState.mapLayers, threatMapAvailability) {
        verifiedItems(
            threatMapAvailability,
            trackingState.mapLayers.map { it.toRoyalMapSignal() },
        )
    }
    val mapSignalIcons = remember(context, mapSignals, isGoogleMapLoaded) {
        if (!isGoogleMapLoaded) {
            emptyMap()
        } else {
            mapSignals.associate { signal ->
            signal.id to bitmapDescriptorFromDrawable(context, signal.iconRes, 46)
            }
        }
    }

    fun toggleSignalLayer(label: String, currentlyVisible: Boolean, update: (Boolean) -> Unit) {
        val isVisible = !currentlyVisible
        update(isVisible)
        Toast.makeText(
            context,
            "$label alerts ${if (isVisible) "shown" else "hidden"}",
            Toast.LENGTH_SHORT
        ).show()
    }
    LaunchedEffect(centerRequest) {
        centerRequest?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 16f), 700)
            trackingVm.consumeCenterRequest()
        }
    }

    // Load Forecast Engine data when the map is ready and we have a position
    LaunchedEffect(isGoogleMapLoaded, currentLocation) {
        if (!isGoogleMapLoaded) return@LaunchedEffect
        val loc = currentLocation ?: defaultCenter
        val delta = 0.25
        riskVm.loadRiskMap(
            minLat = loc.latitude - delta,
            minLng = loc.longitude - delta,
            maxLat = loc.latitude + delta,
            maxLng = loc.longitude + delta
        )
        riskVm.loadHotspots(
            minLat = loc.latitude - delta,
            minLng = loc.longitude - delta,
            maxLat = loc.latitude + delta,
            maxLng = loc.longitude + delta
        )
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            if (lastRecordedLocation == null || distanceMeters(lastRecordedLocation!!, loc) > 300) {
                lastRecordedLocation = loc
                scope.launch(Dispatchers.IO) {
                    val address = resolveAddress(context, loc) ?: "Secured GPS sector"
                    PreferencesManager.addVisitedPlace(loc.latitude, loc.longitude, address)
                    timeline = PreferencesManager.getLocationTimeline()
                }
            }
        }
    }

    if (hasLocationPermission && isSystemLocationEnabled) {
        DisposableEffect(fusedLocationClient) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3500L)
                .setMinUpdateIntervalMillis(1800L)
                .setMinUpdateDistanceMeters(1f)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        val latLng = LatLng(loc.latitude, loc.longitude)
                        currentLocation = latLng
                        lastGpsUpdate = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            .format(java.util.Date())
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            onDispose { fusedLocationClient.removeLocationUpdates(callback) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MapBlack)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission && isSystemLocationEnabled,
                mapStyleOptions = mapStyle,
                mapType = MapType.NORMAL,
                isBuildingEnabled = is3D
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                compassEnabled = false,
                myLocationButtonEnabled = false
            ),
            onMapLoaded = { isGoogleMapLoaded = true },
            onMapLongClick = { latLng ->
                showAddReminder = true
                trackingVm.startAddZone(latLng)
            }
        ) {
            currentLocation?.let {
                Circle(
                    center = it,
                    radius = 90.0,
                    fillColor = MapCyan.copy(alpha = 0.10f),
                    strokeColor = MapCyan.copy(alpha = 0.75f),
                    strokeWidth = 3f
                )
            }

            trackingState.safeZones.forEach { zone ->
                key("safe-zone-${zone.id}-${zone.center.latitude}-${zone.center.longitude}") {
                    val points = remember(zone.center, zone.radiusMeters) {
                        geofencePolygon(zone.center, zone.radiusMeters)
                    }
                    val details = remember(zone) { zone.toRoyalMapZoneDetails() }
                    val isSelected = selectedFamilyZone?.id == details.id &&
                        selectedFamilyZone?.center == details.center
                    if (points.size >= 3) {
                        Polygon(
                            points = points,
                            clickable = true,
                            geodesic = true,
                            fillColor = MapGreen.copy(alpha = if (isSelected) 0.26f else 0.14f),
                            strokeColor = MapGreen.copy(alpha = 0.92f),
                            strokeWidth = if (isSelected) 6f else 3f,
                            onClick = {
                                riskVm.clearSelection()
                                selectedFamilyZone = details
                            },
                        )
                    }
                    Marker(
                        state = MarkerState(zone.center),
                        title = zone.name.ifBlank { details.category.label },
                        snippet = details.category.label,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                        onClick = {
                            riskVm.clearSelection()
                            selectedFamilyZone = details
                            false
                        },
                    )
                }
            }

            trackingState.riskZones.forEach { zone ->
                key("risk-zone-${zone.id}-${zone.center.latitude}-${zone.center.longitude}") {
                    val points = remember(zone.center, zone.radiusMeters) {
                        geofencePolygon(zone.center, zone.radiusMeters)
                    }
                    val details = remember(zone) { zone.toRoyalMapZoneDetails() }
                    val isSelected = selectedFamilyZone?.id == details.id &&
                        selectedFamilyZone?.center == details.center
                    if (points.size >= 3) {
                        Polygon(
                            points = points,
                            clickable = true,
                            geodesic = true,
                            fillColor = MapRed.copy(alpha = if (isSelected) 0.27f else 0.15f),
                            strokeColor = MapRed.copy(alpha = 0.94f),
                            strokeWidth = if (isSelected) 6f else 3f,
                            onClick = {
                                riskVm.clearSelection()
                                selectedFamilyZone = details
                            },
                        )
                    }
                    Marker(
                        state = MarkerState(zone.center),
                        title = zone.name.ifBlank { details.category.label },
                        snippet = "${details.category.label} · Threat ${zone.threatLevel.coerceIn(1, 5)}/5",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                        onClick = {
                            riskVm.clearSelection()
                            selectedFamilyZone = details
                            false
                        },
                    )
                }
            }

            verifiedItems(forecastAvailability, trackingState.hotspots).forEach { hotspot ->
                val center = LatLng(hotspot.center.lat, hotspot.center.lng)
                val color = when (hotspot.riskLevel.uppercase()) {
                    "CRITICAL", "HIGH" -> MapRed
                    "MEDIUM" -> RoyalGold
                    else -> MapGreen
                }
                Circle(
                    center = center,
                    radius = hotspot.radiusMeters,
                    fillColor = color.copy(alpha = 0.18f),
                    strokeColor = color.copy(alpha = 0.78f),
                    strokeWidth = 3f
                )
                Marker(
                    state = MarkerState(center),
                    title = "Forecast ${hotspot.riskLevel}",
                    snippet = "${hotspot.eventCount} signals",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (color == MapRed) BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_YELLOW
                    )
                )
            }

            mapSignals.forEach { signal ->
                val isVisible = when (signal.type) {
                    RoyalMapSignalType.CAMERA -> showCameras
                    RoyalMapSignalType.POLICE -> showPolice
                    RoyalMapSignalType.SPEED -> showSpeed
                    RoyalMapSignalType.PROTEST -> showProtest
                    RoyalMapSignalType.THEFT -> showTheft
                    RoyalMapSignalType.CYBER -> showCyber
                }
                if (isVisible) {
                    SignalCircle(signal)
                    Marker(
                        state = MarkerState(signal.position),
                        title = signal.title,
                        snippet = signal.snippet,
                        icon = mapSignalIcons[signal.id] ?: BitmapDescriptorFactory.defaultMarker(signal.hue)
                    )
                }
            }

            // === Forecast Engine: H3 risk zones (normalized) ===
            if (riskState.mapStatus == RiskMapStatus.LOADED) {
                riskState.normalizedRiskZones
                    .filter { it.polygon.size >= 3 }
                    .forEach { zone ->
                    val details = zone.toRoyalMapZoneDetails()
                    val riskColor = when (details.category) {
                        RoyalMapZoneCategory.SAFE -> MapGreen
                        RoyalMapZoneCategory.CAUTION -> MapCaution
                        RoyalMapZoneCategory.RISK -> MapRed
                    }
                    val isSelected = riskState.selectedNormalizedZone?.h3Cell == zone.h3Cell
                    Polygon(
                        points = zone.polygon,
                        clickable = true,
                        geodesic = true,
                        fillColor = riskColor.copy(alpha = if (isSelected) 0.28f else 0.16f),
                        strokeColor = riskColor.copy(alpha = 0.92f),
                        strokeWidth = if (isSelected) 6f else 3f,
                        onClick = {
                            selectedFamilyZone = null
                            riskVm.selectNormalizedZone(zone)
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(details.center, 16f),
                                    650,
                                )
                            }
                        },
                    )
                    if (isSelected) {
                        Marker(
                            state = MarkerState(details.center),
                            title = details.name,
                            snippet = "${details.category.label} · ${zone.riskScore.toInt()}/100",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                when (details.category) {
                                    RoyalMapZoneCategory.SAFE -> BitmapDescriptorFactory.HUE_GREEN
                                    RoyalMapZoneCategory.CAUTION -> BitmapDescriptorFactory.HUE_YELLOW
                                    RoyalMapZoneCategory.RISK -> BitmapDescriptorFactory.HUE_RED
                                },
                            ),
                        )
                    }
                }
            }

            // Forecast hotspots are independent from the H3 zone-layer request.
            if (riskState.showHotspots) {
                riskState.normalizedHotspots
                    .filter { it.isRenderableOnMap() }
                    .forEach { hotspot ->
                        val center = LatLng(hotspot.latitude, hotspot.longitude)
                        val riskColor = when (hotspot.riskLevel) {
                            com.royalshield.app.features.riskprediction.data.models.RiskLevel.CRITICAL,
                            com.royalshield.app.features.riskprediction.data.models.RiskLevel.HIGH -> MapRed
                            com.royalshield.app.features.riskprediction.data.models.RiskLevel.ELEVATED -> RoyalGold
                            else -> MapGreen
                        }
                        Circle(
                            center = center,
                            radius = hotspot.radiusMeters.coerceIn(1.0, 50_000.0),
                            fillColor = riskColor.copy(alpha = 0.20f),
                            strokeColor = riskColor.copy(alpha = 0.80f),
                            strokeWidth = 2.5f
                        )
                        Marker(
                            state = MarkerState(center),
                            title = "Forecast ${hotspot.riskLevel.label}",
                            snippet = "${hotspot.eventCount} verified signals",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (riskColor == MapRed) BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_YELLOW
                            )
                        )
                    }
            }

            reminders.forEach { reminder ->
                val position = LatLng(reminder.lat, reminder.lon)
                Marker(
                    state = MarkerState(position),
                    title = reminder.name,
                    snippet = reminder.address,
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (reminder.isActive) BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_VIOLET
                    )
                )
            }

            val gpsPoints = timeline.take(20).map { LatLng(it.latitude, it.longitude) }
            if (gpsPoints.size >= 2) {
                Polyline(points = gpsPoints.reversed(), color = Color.Black, width = 9f)
                Polyline(points = gpsPoints.reversed(), color = MapGreen.copy(alpha = 0.72f), width = 5f)
            }

            trackingState.navRoute?.let { route ->
                Polyline(points = route.waypoints, color = MapCyan, width = 7f)
                Marker(
                    state = MarkerState(route.destination),
                    title = route.destinationName,
                    snippet = "${route.etaMinutes} min",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )
            }
        }

        RoyalMapHeader(
            onBack = onBack,
            onOpenAgent = { showAgentDialog = true },
            onCenter = {
                val target = currentLocation ?: trackingState.child?.latLng ?: defaultCenter
                scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 16f), 600) }
            },
            forecast = when {
                riskState.mapStatus == RiskMapStatus.LOADED && riskState.forecastNotice != null -> {
                    riskState.forecastNotice.orEmpty()
                }
                forecastAvailability is AvailabilityState.Ready -> forecastSummary(trackingState.hotspots)
                else -> "Unavailable"
            },
            lastGpsUpdate = lastGpsUpdate,
            isDarkMode = isDarkMode,
            onToggleDarkMode = {
                isDarkMode = !isDarkMode
                Toast.makeText(
                    context,
                    if (isDarkMode) "Map Dark Mode: Activated" else "Map Standard Mode: Activated",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        val locationStatus = resolveMapLocationStatus(
            locationEnabled = isSystemLocationEnabled,
            hasPermission = hasLocationPermission,
            hasFix = currentLocation != null,
        )
        AnimatedVisibility(
            visible = locationStatus != MapLocationStatus.AVAILABLE,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 88.dp, start = 12.dp, end = 12.dp),
        ) {
            MapLocationStatusCard(
                status = locationStatus,
                onEnableLocation = {
                    locationSettingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                },
                onGrantPermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                },
            )
        }

        // MODEL UNAVAILABLE banner — shown when Forecast Engine backend is down (503/timeout)
        AnimatedVisibility(
            visible = riskState.mapStatus == RiskMapStatus.BACKEND_ERROR ||
                riskState.mapStatus == RiskMapStatus.OFFLINE,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = if (locationStatus != MapLocationStatus.AVAILABLE) 140.dp else 88.dp, start = 12.dp, end = 12.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0000)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MapRed.copy(alpha = 0.72f)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MapRed, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "MODEL UNAVAILABLE",
                            color = MapRed,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Forecast Engine offline. Risk zones are not verified.",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoyalMapTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            if (threatMapAvailability is AvailabilityState.Ready) {
                MapSignalLayerPanel(
                    showCameras = showCameras,
                    showPolice = showPolice,
                    showSpeed = showSpeed,
                    onToggleCameras = { toggleSignalLayer("Camera", showCameras) { showCameras = it } },
                    onTogglePolice = { toggleSignalLayer("Police", showPolice) { showPolice = it } },
                    onToggleSpeed = { toggleSignalLayer("Speed", showSpeed) { showSpeed = it } },
                    showProtest = showProtest,
                    showTheft = showTheft,
                    showCyber = showCyber,
                    onToggleProtest = { toggleSignalLayer("Protest", showProtest) { showProtest = it } },
                    onToggleTheft = { toggleSignalLayer("Theft", showTheft) { showTheft = it } },
                    onToggleCyber = { toggleSignalLayer("Cyber", showCyber) { showCyber = it } }
                )
            } else {
                Text(
                    "Advanced map intelligence unavailable",
                    color = RoyalGold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            AnimatedVisibility(visible = selectedTab == RoyalMapTab.CONTROL, enter = fadeIn(), exit = fadeOut()) {
                RoyalMapControlPanel(
                    trackingState = trackingState,
                    onLive = { trackingVm.switchMode(TrackingMode.LIVE) },
                    onNav = { trackingVm.switchMode(TrackingMode.NAV) },
                    onHistory = {
                        trackingVm.switchMode(TrackingMode.HISTORY)
                        showTimeline = true
                    },
                    onZones = { trackingVm.toggleZoneSheet(!trackingState.isZoneSheetOpen) },
                    onPanel = { trackingVm.toggleControlPanel(!trackingState.isControlPanelOpen) },
                    onGpsMode = { trackingVm.setGpsMode(it) },
                    onRefresh = { trackingVm.refreshChildLocation() },
                    is3D = is3D,
                    onToggle3D = {
                        is3D = !is3D
                        val target = CameraPosition.Builder(cameraPositionState.position)
                            .tilt(if (is3D) 55f else 0f)
                            .build()
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(target),
                                900
                            )
                        }
                    },
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = {
                        isDarkMode = !isDarkMode
                        Toast.makeText(
                            context,
                            if (isDarkMode) "Map Dark Mode: Activated" else "Map Standard Mode: Activated",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            AnimatedVisibility(visible = selectedTab == RoyalMapTab.REMINDERS, enter = fadeIn(), exit = fadeOut()) {
                ReminderPanel(
                    reminders = reminders,
                    onAdd = { showAddReminder = true },
                    onDirections = { reminder ->
                        // Native camera animation — no external intent, user stays in Royal Shield
                        val dest = LatLng(reminder.lat, reminder.lon)
                        riskVm.setDestination(dest, reminder.name)
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(dest, 16f), 800
                            )
                        }
                    }
                )
            }
            AnimatedVisibility(visible = selectedTab == RoyalMapTab.SAFE, enter = fadeIn(), exit = fadeOut()) {
                SafeRecommendationsPanel(
                    currentLocation = currentLocation,
                    reminders = reminders,
                    hotspotCount = verifiedItems(forecastAvailability, trackingState.hotspots)
                        .count { it.riskLevel.uppercase() in listOf("HIGH", "CRITICAL") }
                )
            }
            AnimatedVisibility(visible = selectedTab == RoyalMapTab.HISTORY || showTimeline, enter = fadeIn(), exit = fadeOut()) {
                GpsHistoryPanel(
                    timeline = timeline,
                    onClear = {
                        PreferencesManager.clearLocationTimeline()
                        timeline = emptyList()
                        showTimeline = false
                    }
                )
            }
        }

        if (trackingState.isZoneSheetOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                SafeZonesSheet(
                    safeZones = trackingState.safeZones,
                    riskZones = trackingState.riskZones,
                    onDeleteSafe = { trackingVm.deleteSafeZone(it) },
                    onDeleteRisk = { trackingVm.deleteRiskZone(it) },
                    onClose = { trackingVm.toggleZoneSheet(false) }
                )
            }
        }

        if (trackingState.isControlPanelOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                ChildControlPanelSheet(
                    child = trackingState.child,
                    currentGpsMode = trackingState.child?.gpsMode ?: GpsMode.BALANCED,
                    onGpsModeSelected = { trackingVm.setGpsMode(it) },
                    onRefreshLocation = { trackingVm.refreshChildLocation() },
                    onPlayAlertSound = {
                        trackingState.child?.let {
                            trackingVm.playAlertSound(it.id)
                            Toast.makeText(context, "Alert sound command sent", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClose = { trackingVm.toggleControlPanel(false) }
                )
            }
        }
    }

    val selectedZoneDetails = selectedFamilyZone ?: riskState.selectedNormalizedZone?.toRoyalMapZoneDetails()
    selectedZoneDetails?.let { details ->
        RoyalMapZoneDetailsSheet(
            details = details,
            onDismiss = {
                selectedFamilyZone = null
                riskVm.clearSelection()
            },
            onCenterZone = {
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(details.center, 16f),
                        650,
                    )
                }
            },
        )
    }

    if (showAddReminder) {
        AddReminderDialog(
            currentLocation = currentLocation ?: defaultCenter,
            onDismiss = { showAddReminder = false },
            onSave = { name, address, latLng ->
                globalVm.addReminder(name, address, latLng.latitude, latLng.longitude)
                showAddReminder = false
            }
        )
    }

    if (showAgentDialog) {
        com.royalshield.app.ui.components.VirtualAgentDialog(
            onDismiss = { showAgentDialog = false }
        )
    }
}

private enum class RoyalMapTab { CONTROL, REMINDERS, SAFE, HISTORY }

private enum class RoyalMapSignalType { CAMERA, POLICE, SPEED, PROTEST, THEFT, CYBER }

private data class RoyalMapSignal(
    val id: String,
    val type: RoyalMapSignalType,
    val title: String,
    val snippet: String,
    val position: LatLng,
    val radiusMeters: Double,
    val hue: Float,
    val color: Color,
    val iconRes: Int
)

private fun com.royalshield.app.features.riskprediction.data.remote.MapLayerPoint.toRoyalMapSignal(): RoyalMapSignal {
    val signalType = when (type.lowercase()) {
        "camera", "cameras", "surveillance" -> RoyalMapSignalType.CAMERA
        "police" -> RoyalMapSignalType.POLICE
        "speed", "speed_trap", "speed-zone" -> RoyalMapSignalType.SPEED
        "protest" -> RoyalMapSignalType.PROTEST
        "theft", "thief" -> RoyalMapSignalType.THEFT
        "cyber", "hacker" -> RoyalMapSignalType.CYBER
        else -> RoyalMapSignalType.CYBER
    }
    val color = when (signalType) {
        RoyalMapSignalType.CAMERA -> MapCyan
        RoyalMapSignalType.POLICE -> Color(0xFF4DA3FF)
        RoyalMapSignalType.SPEED -> RoyalGold
        RoyalMapSignalType.PROTEST -> Color(0xFFFFA726)
        RoyalMapSignalType.THEFT -> Color(0xFFB27CFF)
        RoyalMapSignalType.CYBER -> MapGreen
    }
    val icon = when (signalType) {
        RoyalMapSignalType.CAMERA -> R.drawable.pin_surveillance_map
        RoyalMapSignalType.POLICE -> R.drawable.pin_police_map
        RoyalMapSignalType.SPEED -> R.drawable.pin_speed_map
        RoyalMapSignalType.PROTEST -> R.drawable.pin_protest_map
        RoyalMapSignalType.THEFT -> R.drawable.pin_thief_map
        RoyalMapSignalType.CYBER -> R.drawable.pin_hacker_map
    }
    val hue = when (signalType) {
        RoyalMapSignalType.CAMERA -> BitmapDescriptorFactory.HUE_AZURE
        RoyalMapSignalType.POLICE -> BitmapDescriptorFactory.HUE_BLUE
        RoyalMapSignalType.SPEED, RoyalMapSignalType.PROTEST -> BitmapDescriptorFactory.HUE_ORANGE
        RoyalMapSignalType.THEFT -> BitmapDescriptorFactory.HUE_VIOLET
        RoyalMapSignalType.CYBER -> BitmapDescriptorFactory.HUE_GREEN
    }
    return RoyalMapSignal(
        id = id,
        type = signalType,
        title = title,
        snippet = "$snippet | $source",
        position = LatLng(lat, lng),
        radiusMeters = when (severity.uppercase()) {
            "HIGH", "CRITICAL" -> 150.0
            "MEDIUM" -> 120.0
            else -> 90.0
        },
        hue = hue,
        color = color,
        iconRes = icon
    )
}

@Composable
private fun SignalCircle(signal: RoyalMapSignal) {
    Circle(
        center = signal.position,
        radius = signal.radiusMeters,
        fillColor = signal.color.copy(alpha = 0.12f),
        strokeColor = signal.color.copy(alpha = 0.82f),
        strokeWidth = 2.5f
    )
}

private fun isDeviceLocationEnabled(context: android.content.Context): Boolean {
    val manager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        manager.isLocationEnabled
    } else {
        runCatching {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }
}

@Composable
private fun MapLocationStatusCard(
    status: MapLocationStatus,
    onEnableLocation: () -> Unit,
    onGrantPermission: () -> Unit,
) {
    val message = when (status) {
        MapLocationStatus.LOCATION_OFF -> "Location is off. Enable it to show your real device point."
        MapLocationStatus.PERMISSION_REQUIRED -> "Location permission is required to show your device point."
        MapLocationStatus.WAITING_FOR_FIX -> "Waiting for a real GPS fix…"
        MapLocationStatus.AVAILABLE -> return
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MapPanel),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGold.copy(alpha = 0.62f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.LocationOff, contentDescription = null, tint = RoyalGold)
            Text(message, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
            when (status) {
                MapLocationStatus.LOCATION_OFF -> TextButton(onClick = onEnableLocation) {
                    Text("ENABLE", color = RoyalGold, fontWeight = FontWeight.Bold)
                }
                MapLocationStatus.PERMISSION_REQUIRED -> TextButton(onClick = onGrantPermission) {
                    Text("GRANT", color = RoyalGold, fontWeight = FontWeight.Bold)
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun RoyalMapHeader(
    onBack: () -> Unit,
    onCenter: () -> Unit,
    forecast: String,
    lastGpsUpdate: String,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onOpenAgent: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(12.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MapPanel)
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("ROYAL MAP", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.5.sp)
            Text("Forecast: $forecast · GPS $lastGpsUpdate", color = Color.White.copy(alpha = 0.62f), fontSize = 11.sp, maxLines = 2)
        }
        IconButton(
            onClick = onToggleDarkMode,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(36.dp)
                .background(
                    if (isDarkMode) RoyalGold.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f),
                    CircleShape
                )
                .border(
                    1.dp,
                    if (isDarkMode) RoyalGold.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.30f),
                    CircleShape
                )
        ) {
            Icon(
                if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = if (isDarkMode) "Disable Dark Mode" else "Enable Dark Mode",
                tint = if (isDarkMode) RoyalGold else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onCenter) {
            Icon(Icons.Default.MyLocation, contentDescription = "Center", tint = RoyalGold)
        }
        IconButton(
            onClick = onOpenAgent,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(36.dp)
                .background(RoyalGold.copy(alpha = 0.20f), CircleShape)
                .border(1.dp, RoyalGold.copy(alpha = 0.85f), CircleShape)
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = "AI Agent",
                tint = RoyalGold,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun RoyalMapTabs(selectedTab: RoyalMapTab, onTabSelected: (RoyalMapTab) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(RoyalMapTab.values().toList()) { tab ->
            val selected = tab == selectedTab
            val label = when (tab) {
                RoyalMapTab.CONTROL -> "Control"
                RoyalMapTab.REMINDERS -> "Reminders"
                RoyalMapTab.SAFE -> "Safe Zones"
                RoyalMapTab.HISTORY -> "GPS History"
            }
            Text(
                text = label,
                color = if (selected) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) RoyalGold else MapPanel)
                    .border(1.dp, RoyalGold.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
private fun MapSignalLayerPanel(
    showCameras: Boolean,
    showPolice: Boolean,
    showSpeed: Boolean,
    showProtest: Boolean,
    showTheft: Boolean,
    showCyber: Boolean,
    onToggleCameras: () -> Unit,
    onTogglePolice: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleProtest: () -> Unit,
    onToggleTheft: () -> Unit,
    onToggleCyber: () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SignalLayerChip(
                label = "Cameras",
                iconRes = R.drawable.pin_surveillance_gold,
                color = MapCyan,
                selected = showCameras,
                onClick = onToggleCameras
            )
        }
        item {
            SignalLayerChip(
                label = "Police",
                iconRes = R.drawable.pin_police_gold,
                color = Color(0xFF4DA3FF),
                selected = showPolice,
                onClick = onTogglePolice
            )
        }
        item {
            SignalLayerChip(
                label = "Speed",
                iconRes = R.drawable.pin_speed_map,
                color = RoyalGold,
                selected = showSpeed,
                onClick = onToggleSpeed
            )
        }
        item {
            SignalLayerChip(
                label = "Protest",
                iconRes = R.drawable.pin_protest_gold,
                color = Color(0xFFFFA726),
                selected = showProtest,
                onClick = onToggleProtest
            )
        }
        item {
            SignalLayerChip(
                label = "Theft",
                iconRes = R.drawable.pin_thief_gold,
                color = Color(0xFFB27CFF),
                selected = showTheft,
                onClick = onToggleTheft
            )
        }
        item {
            SignalLayerChip(
                label = "Cyber",
                iconRes = R.drawable.pin_hacker_map,
                color = MapGreen,
                selected = showCyber,
                onClick = onToggleCyber
            )
        }
    }
}

@Composable
private fun SignalLayerChip(
    label: String,
    iconRes: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) color.copy(alpha = 0.20f) else MapPanel)
            .border(1.dp, color.copy(alpha = if (selected) 0.80f else 0.30f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp)
        )
        Text(label, color = if (selected) Color.White else Color.White.copy(alpha = 0.58f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RoyalMapControlPanel(
    trackingState: com.royalshield.app.features.trackingshield.data.TrackingUiState,
    onLive: () -> Unit,
    onNav: () -> Unit,
    onHistory: () -> Unit,
    onZones: () -> Unit,
    onPanel: () -> Unit,
    onGpsMode: (GpsMode) -> Unit,
    onRefresh: () -> Unit,
    is3D: Boolean,
    onToggle3D: () -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    RoyalMapSheet {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ControlAction("Live", Icons.Default.Radar, trackingState.mode == TrackingMode.LIVE, Modifier.weight(1f), onLive)
            ControlAction("Nav", Icons.Default.Route, trackingState.mode == TrackingMode.NAV, Modifier.weight(1f), onNav)
            ControlAction("History", Icons.Default.Timeline, trackingState.mode == TrackingMode.HISTORY, Modifier.weight(1f), onHistory)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ControlAction("Zones", Icons.Default.Shield, false, Modifier.weight(1f), onZones)
            ControlAction("Panel", Icons.Default.Tune, false, Modifier.weight(1f), onPanel)
            ControlAction("Refresh", Icons.Default.Refresh, false, Modifier.weight(1f), onRefresh)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ControlAction(
                label = if (isDarkMode) "Dark Map ON" else "Dark Map OFF",
                icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                active = isDarkMode,
                modifier = Modifier.weight(1f),
                onClick = onToggleDarkMode
            )
            ControlAction(
                label = if (is3D) "2D View" else "3D View",
                icon = Icons.Default.Layers,
                active = is3D,
                modifier = Modifier.weight(1f),
                onClick = onToggle3D
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("GPS polling", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(trackingState.child?.name ?: "Personal tracking mode", color = Color.White.copy(alpha = 0.58f), fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(GpsMode.HIGH_ACCURACY, GpsMode.BALANCED, GpsMode.BATTERY_SAVER).forEach { mode ->
                    Text(
                        text = when (mode) {
                            GpsMode.HIGH_ACCURACY -> "High"
                            GpsMode.BALANCED -> "Bal"
                            GpsMode.BATTERY_SAVER -> "Eco"
                        },
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(RoyalGold)
                            .clickable { onGpsMode(mode) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderPanel(reminders: List<MapReminder>, onAdd: () -> Unit, onDirections: (MapReminder) -> Unit) {
    RoyalMapSheet {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Direction reminders", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            TextButton(onClick = onAdd) { Text("ADD", color = RoyalGold, fontWeight = FontWeight.Bold) }
        }
        reminders.take(4).forEach { reminder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable { onDirections(reminder) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.NotificationsActive, null, tint = if (reminder.isActive) MapGreen else Color.Gray)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(reminder.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(reminder.address, color = Color.White.copy(alpha = 0.56f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Default.Directions, null, tint = RoyalGold, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
private fun SafeRecommendationsPanel(currentLocation: LatLng?, reminders: List<MapReminder>, hotspotCount: Int) {
    val recommendation = when {
        currentLocation == null -> "Enable GPS to calculate the nearest safer area."
        hotspotCount > 2 -> "High-risk forecast nearby. Prefer saved places and avoid lingering in unknown sectors."
        reminders.any { it.isActive } -> "Your active reminders include trusted places. Use them as safe arrival anchors."
        else -> "Area looks calm. Add Home, Work, or School as trusted safe zones."
    }
    RoyalMapSheet {
        Text("AI safe-zone recommendations", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(recommendation, color = Color.White.copy(alpha = 0.74f), fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecommendationChip("Safer route", MapGreen)
            RecommendationChip("Trusted place", RoyalGold)
            RecommendationChip("Risk forecast", MapRed)
        }
    }
}

@Composable
private fun GpsHistoryPanel(timeline: List<PreferencesManager.VisitedPlace>, onClear: () -> Unit) {
    RoyalMapSheet {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("GPS history", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            TextButton(onClick = onClear) { Text("CLEAR", color = MapRed, fontWeight = FontWeight.Bold) }
        }
        LazyColumn(modifier = Modifier.heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (timeline.isEmpty()) {
                item { Text("No GPS history recorded yet.", color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp) }
            } else {
                items(timeline.take(8)) { place ->
                    Text(
                        text = "${place.dateTime}  ${place.address}",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.055f))
                            .padding(9.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RoyalMapSheet(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MapPanel)
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun ControlAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) RoyalGold else Color.White.copy(alpha = 0.07f))
            .border(1.dp, RoyalGold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (active) Color.Black else RoyalGold, modifier = Modifier.size(18.dp))
        Text(label, color = if (active) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecommendationChip(label: String, color: Color) {
    Text(
        text = label,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.11f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp)
    )
}

@Composable
private fun AddReminderDialog(currentLocation: LatLng, onDismiss: () -> Unit, onSave: (String, String, LatLng) -> Unit) {
    var name by remember { mutableStateOf("Safe destination") }
    var address by remember { mutableStateOf("Current GPS position") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101010),
        title = { Text("Add direction reminder", color = RoyalGold, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address note") }, singleLine = true)
                Text("Reminder will use your current GPS point for this version.", color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.ifBlank { "Safe destination" }, address.ifBlank { "Current GPS position" }, currentLocation) }) {
                Text("SAVE", color = RoyalGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray) }
        }
    )
}

private fun forecastSummary(hotspots: List<com.royalshield.app.features.riskprediction.data.remote.Hotspot>): String {
    val high = hotspots.count { it.riskLevel.uppercase() in listOf("HIGH", "CRITICAL") }
    return when {
        high >= 3 -> "High risk nearby"
        high > 0 -> "Watch zones"
        hotspots.isNotEmpty() -> "Stable"
        else -> "Learning"
    }
}

// openDirections removed: navigation is now fully native via camera animation in RoyalMapScreen.
// The reminder destinations are handled by riskVm.setDestination() + cameraPositionState.animate().

private fun distanceMeters(a: LatLng, b: LatLng): Double {
    val radius = 6371000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val h = sin(dLat / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2.0)
    return 2 * radius * asin(sqrt(h))
}

private fun resolveAddress(context: android.content.Context, latLng: LatLng): String? {
    return runCatching {
        @Suppress("DEPRECATION")
        Geocoder(context, Locale.getDefault())
            .getFromLocation(latLng.latitude, latLng.longitude, 1)
            ?.firstOrNull()
            ?.getAddressLine(0)
    }.getOrNull()
}

private fun bitmapDescriptorFromDrawable(
    context: android.content.Context,
    resId: Int,
    sizeDp: Int
): com.google.android.gms.maps.model.BitmapDescriptor? {
    val drawable = ContextCompat.getDrawable(context, resId) ?: return null
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).roundToInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
