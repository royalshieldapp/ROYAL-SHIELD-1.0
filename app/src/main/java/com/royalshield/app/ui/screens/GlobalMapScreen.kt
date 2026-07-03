package com.royalshield.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.royalshield.app.ui.components.RoyalFrameCard
import com.royalshield.app.ui.theme.RoyalGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.royalshield.app.features.riskprediction.ui.RiskMapViewModel
import com.royalshield.app.features.riskprediction.ui.RiskMapUiState
import com.royalshield.app.features.riskprediction.ui.riskZonesToWeightedPoints
import com.royalshield.app.features.riskprediction.ui.createHeatmapProvider
import com.royalshield.app.features.riskprediction.ui.calculateBoundingBox
import kotlin.math.*

import com.royalshield.app.models.ThreatAlert
import com.royalshield.app.models.ThreatType
import com.royalshield.app.models.TrafficAlert
import com.royalshield.app.models.TrafficAlertType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalMapScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Services
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    // State
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedThreat by remember { mutableStateOf<ThreatAlert?>(null) }
    var selectedTrafficAlert by remember { mutableStateOf<TrafficAlert?>(null) }
    var currentTab by remember { mutableIntStateOf(1) } // Default to Traffic for the request
    var showCyberMap by remember { mutableStateOf(false) }
    
    // Navigation State
    var isNavigating by remember { mutableStateOf(false) }
    var navigationDestination by remember { mutableStateOf<LatLng?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var distanceToDest by remember { mutableDoubleStateOf(0.0) }
    var showArrivalAlert by remember { mutableStateOf(false) }
    var reminderThreshold by remember { mutableStateOf(50000.0) } // Default 50km
    var lastRecordedLocation by remember { mutableStateOf<LatLng?>(null) }
    var navigationIconType by remember { mutableIntStateOf(0) } // 0: Stealth Arrow, 1: Royal Shield, 2: Cyber Drone
    var isGpsEnabled by remember { mutableStateOf(true) } // User-controlled GPS state
    
    // ── DEBUG OVERLAY STATE ────────────────────────────────────────
    var showDebugOverlay by remember { mutableStateOf(true) }
    var locationUpdateCount by remember { mutableIntStateOf(0) }
    var lastUpdateTime by remember { mutableStateOf("--") }
    var currentSpeed by remember { mutableStateOf("--") }
    var currentAccuracy by remember { mutableStateOf("--") }
    var currentAltitude by remember { mutableStateOf("--") }
    var currentProvider by remember { mutableStateOf("--") }
    
    // Risk Prediction State
    val riskViewModel: RiskMapViewModel = viewModel()
    val riskUiState by riskViewModel.uiState.collectAsState()
    var showRiskHeatmap by remember { mutableStateOf(false) }
    var heatmapProvider by remember { mutableStateOf<HeatmapTileProvider?>(null) }

    // Threat Intelligence State
    val mapViewModel: com.royalshield.app.viewmodels.MapViewModel = viewModel()
    val threats by mapViewModel.threats.collectAsState()
    val isFetchingThreats by mapViewModel.isLoading.collectAsState()
    
    val trafficAlerts = remember {
        listOf(
            TrafficAlert("C1", TrafficAlertType.TRAFFIC_CAMERA, "Camera", 4.6550, -74.0750, "Red Light Camera", Icons.Default.Videocam),
            TrafficAlert("P1", TrafficAlertType.POLICE, "Police", 4.6250, -74.0700, "Patrol Unit", Icons.Default.LocalPolice),
            TrafficAlert("ST1", TrafficAlertType.SPEED_TRAP, "Radar", 4.6300, -74.0850, "Speed Trap", Icons.Default.Speed),
            TrafficAlert("TJ1", TrafficAlertType.TRAFFIC_JAM, "Traffic", 4.6420, -74.0820, "Heavy Traffic", Icons.Default.Traffic)
        )
    }

    // Camera
    val bogota = LatLng(4.6097, -74.0817)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.builder()
            .target(bogota)
            .zoom(18f)
            .tilt(45f) // 3D View tilt
            .build()
    }
    
    // Permission Check & Location Updates
    var hasLocationPermission by remember { mutableStateOf(false) }
    
    // Check permission once
    LaunchedEffect(Unit) {
        hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // ── REAL-TIME LOCATION CALLBACK ────────────────────────────────
    
    // Seed initial threats based on actual GPS location once found
    var initialThreatsSeeded by remember { mutableStateOf(false) }
    LaunchedEffect(userLocation) {
        if (userLocation != null && !initialThreatsSeeded) {
            mapViewModel.refreshThreats(userLocation!!.latitude, userLocation!!.longitude)
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLocation!!, 16f))
            initialThreatsSeeded = true
        }
    }

    @SuppressLint("MissingPermission")
    if (hasLocationPermission && isGpsEnabled) {
        DisposableEffect(fusedLocationClient) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 3000L // Update every 3 seconds
            )
                .setMinUpdateIntervalMillis(1500L)
                .setMinUpdateDistanceMeters(1f)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        val latLng = LatLng(loc.latitude, loc.longitude)
                        userLocation = latLng
                        locationUpdateCount++
                        lastUpdateTime = java.text.SimpleDateFormat(
                            "HH:mm:ss", java.util.Locale.getDefault()
                        ).format(java.util.Date())
                        currentSpeed = if (loc.hasSpeed()) String.format("%.1f km/h", loc.speed * 3.6f) else "N/A"
                        currentAccuracy = if (loc.hasAccuracy()) String.format("%.1f m", loc.accuracy) else "N/A"
                        currentAltitude = if (loc.hasAltitude()) String.format("%.1f m", loc.altitude) else "N/A"
                        currentProvider = loc.provider ?: "fused"

                        android.util.Log.d("RealTimeTracker", "UPDATE #$locationUpdateCount → ${loc.latitude}, ${loc.longitude} | Acc: ${loc.accuracy}m")
                    }
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest, callback, Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                android.util.Log.e("RealTimeTracker", "Permission denied", e)
            }

            onDispose {
                fusedLocationClient.removeLocationUpdates(callback)
                android.util.Log.d("RealTimeTracker", "Location updates STOPPED. Total updates: $locationUpdateCount")
            }
        }
    }

    // Navigation Loop
    LaunchedEffect(isNavigating, userLocation, navigationDestination) {
        if (isNavigating && userLocation != null && navigationDestination != null) {
            val dist = calculateDistance(
                userLocation!!.latitude, userLocation!!.longitude,
                navigationDestination!!.latitude, navigationDestination!!.longitude
            )
            distanceToDest = dist
            routePoints = listOf(userLocation!!, navigationDestination!!)
            
            // TIMELINE RECORDING: Record location if moved > 500m from last record or first record
            if (lastRecordedLocation == null || calculateDistance(userLocation!!.latitude, userLocation!!.longitude, lastRecordedLocation!!.latitude, lastRecordedLocation!!.longitude) > 500) {
                lastRecordedLocation = userLocation
                val currentLat = userLocation!!.latitude
                val currentLng = userLocation!!.longitude
                
                // Launch geocoding in background to avoid blocking UI
                launch(kotlinx.coroutines.Dispatchers.IO) {
                     var addressStr = "Secured Sector"
                     try {
                         val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                         // Check for simple sync method availability or fallbacks
                         if (android.os.Build.VERSION.SDK_INT >= 33) {
                             // We stay synchronous here for simplicity in this flow, or usage of simple callback wrapper could apply.
                             // But effectively using the blocking call on IO thread is safe for immediate sequential logic if supported,
                             // or we stick to the older API that is blocking but deprecated, which works fine on IO thread.
                             // For simplicity and compatibility in this snippet without complex callback nesting:
                             @Suppress("DEPRECATION") 
                             val addresses = geocoder.getFromLocation(currentLat, currentLng, 1)
                             if (!addresses.isNullOrEmpty()) {
                                 addressStr = addresses[0].getAddressLine(0) ?: "Unknown Location"
                             }
                         } else {
                             @Suppress("DEPRECATION")
                             val addresses = geocoder.getFromLocation(currentLat, currentLng, 1)
                             if (!addresses.isNullOrEmpty()) {
                                 addressStr = addresses[0].getAddressLine(0) ?: "Unknown Location"
                             }
                         }
                     } catch (e: Exception) {
                         e.printStackTrace()
                     }
                     com.royalshield.app.managers.PreferencesManager.addVisitedPlace(currentLat, currentLng, addressStr)
                }
            }

            // PROXIMITY REMINDER (50km or 100km check)
            if (dist < reminderThreshold && dist > (reminderThreshold - 1000)) { // Alert within 1km of threshold
                showArrivalAlert = true
            } else if (dist < 150) { // Final Arrival
                showArrivalAlert = true
            } else {
                showArrivalAlert = false
            }
        }
    }

    // Load Risk Data when camera moves or heatmap toggled
    LaunchedEffect(cameraPositionState.isMoving, showRiskHeatmap) {
        if (showRiskHeatmap && !cameraPositionState.isMoving) {
            val bounds = calculateBoundingBox(cameraPositionState.position.target, cameraPositionState.position.zoom)
            riskViewModel.loadRiskMap(
                bounds.southwest.latitude, bounds.southwest.longitude,
                bounds.northeast.latitude, bounds.northeast.longitude
            )
            riskViewModel.loadHotspots(
                bounds.southwest.latitude, bounds.southwest.longitude,
                bounds.northeast.latitude, bounds.northeast.longitude
            )
        }
    }

    // Generate Heatmap Provider when risk zones change
    LaunchedEffect(riskUiState.riskZones) {
        if (riskUiState.riskZones.isNotEmpty()) {
            val weightedPoints = riskZonesToWeightedPoints(riskUiState.riskZones)
            if (weightedPoints.isNotEmpty()) {
                heatmapProvider = createHeatmapProvider(weightedPoints)
            }
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 140.dp,
        sheetContainerColor = Color(0xFF121212),
        sheetContentColor = Color.White,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContent = {
            // BOTTOM PANEL CONTENT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp) // Max height when expanded
                    .padding(16.dp)
            ) {
                // Handle/Gripper
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Gray)
                        .align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                     TabButton("TRAFFIC", currentTab == 1, trafficAlerts.size, Modifier.weight(1f)) { currentTab = 1 }
                     TabButton("THREATS", currentTab == 0, threats.size, Modifier.weight(1f)) { currentTab = 0 }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // List
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    if (currentTab == 1) {
                        items(trafficAlerts) { alert ->
                             TrafficAlertCard(
                                alert = alert,
                                isSelected = selectedTrafficAlert == alert,
                                userLocation = userLocation,
                                onClick = {
                                    selectedTrafficAlert = alert
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(LatLng(alert.lat, alert.lon), 16f)
                                        )
                                    }
                                }
                            )
                        }
                    } else {
                        items(threats) { threat ->
                             ThreatCard(
                                threat = threat,
                                isSelected = selectedThreat == threat,
                                onClick = {
                                    selectedThreat = threat
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(LatLng(threat.lat, threat.lon), 16f)
                                        )
                                    }
                                },
                                onNavigate = {
                                    navigationDestination = LatLng(threat.lat, threat.lon)
                                    isNavigating = true
                                    scope.launch { 
                                        try {
                                            scaffoldState.bottomSheetState.partialExpand()
                                        } catch (e: Exception) {
                                            // Ignore expand errors
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // MAIN CONTENT (MAP)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Padding from sheet is minimal usually
        ) {
            // MAP LAYER
            // MAP LAYER
            Box(modifier = Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = hasLocationPermission && isGpsEnabled,
                        mapType = if (showCyberMap) MapType.TERRAIN else MapType.NORMAL,
                        isBuildingEnabled = true,
                        mapStyleOptions = MapStyleOptions(
                            if (showCyberMap) // "Cyber" Retro Style
                            """
                            [
                              { "elementType": "geometry", "stylers": [{"color": "#000000"}] },
                              { "elementType": "labels.text.fill", "stylers": [{"color": "#00E5FF"}] },
                              { "featureType": "road", "elementType": "geometry", "stylers": [{"color": "#1A1A1A"}] },
                              { "featureType": "water", "elementType": "geometry", "stylers": [{"color": "#002020"}] }
                            ]
                            """.trimIndent()
                            else
                            """
                            [
                              { "elementType": "geometry", "stylers": [{"color": "#212121"}] },
                              { "elementType": "labels.icon", "stylers": [{"visibility": "off"}] },
                              { "elementType": "labels.text.fill", "stylers": [{"color": "#757575"}] },
                              { "elementType": "labels.text.stroke", "stylers": [{"color": "#212121"}] },
                              { "featureType": "road", "elementType": "geometry.fill", "stylers": [{"color": "#2c2c2c"}] },
                              { "featureType": "water", "elementType": "geometry", "stylers": [{"color": "#000000"}] }
                            ]
                            """.trimIndent()
                        )
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        compassEnabled = true
                    )
                ) {
                    // ... (Mismo contenido de Polyline, Markers de tráfico, Heatmap que antes)
                    if (isNavigating && routePoints.isNotEmpty()) {
                         Polyline(
                            points = routePoints,
                            color = if (showCyberMap) Color(0xFFFF00CC) else Color(0xFF00E5FF),
                            width = 15f,
                            geodesic = true,
                            pattern = listOf(Dash(30f), Gap(10f))
                        )
                    }

                    // Native Threats Markers from AlienVault
                    threats.forEach { threat ->
                        Marker(
                            state = MarkerState(position = LatLng(threat.lat, threat.lon)),
                            title = threat.label,
                            snippet = "OTX Intel Alert",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (showCyberMap) BitmapDescriptorFactory.HUE_MAGENTA else BitmapDescriptorFactory.HUE_RED
                            )
                        )
                    }
                    
                    // ... (Mismo contenido de Heatmap y Hotspots)
                    if (showRiskHeatmap && heatmapProvider != null) {
                        TileOverlay(
                            tileProvider = heatmapProvider!!,
                            transparency = 0.3f
                        )
                    }

                    // Indicators for Fetching
                    if (isFetchingThreats) {
                        // (Opcional) Visual feedback for loading data
                    }
                }
            }
            
            // UI OVERLAYS (Top Bar)
             Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                // Header / Nav Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RoyalGold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("ROYAL MAP SYSTEM", color = RoyalGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Text(if(isNavigating) "NAVIGATION ACTIVE" else "MONITORING SECTOR", color = if(isNavigating) Color.Green else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    
                    if (isNavigating) {
                        Button(
                            onClick = { isNavigating = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("STOP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                            // GPS Toggle
                            IconButton(onClick = { isGpsEnabled = !isGpsEnabled }) {
                                Icon(
                                    if(isGpsEnabled) Icons.Default.GpsFixed else Icons.Default.GpsOff, 
                                    null, 
                                    tint = if(isGpsEnabled) RoyalGold else Color.Gray
                                )
                            }
                            // Risk Prediction Toggle
                            IconButton(onClick = { showRiskHeatmap = !showRiskHeatmap }) {
                                Icon(
                                    Icons.Default.QueryStats, 
                                    null, 
                                    tint = if(showRiskHeatmap) Color(0xFFFF3B30) else Color.White
                                )
                            }
                            // Cyber Map Toggle with Label
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = { showCyberMap = !showCyberMap },
                                    modifier = Modifier.background(
                                        if (showCyberMap) RoyalGold.copy(alpha = 0.2f) else Color.Transparent,
                                        CircleShape
                                    )
                                ) {
                                    Icon(
                                        if (showCyberMap) Icons.Default.Map else Icons.Default.Public,
                                        contentDescription = "Cyber Map",
                                        tint = if (showCyberMap) RoyalGold else Color.White
                                    )
                                }
                                Text(
                                    if (showCyberMap) "LOCAL" else "LIVE",
                                    color = if (showCyberMap) RoyalGold else Color.White.copy(alpha = 0.6f),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                         }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Custom Map Controls (Zoom + / - / Locate)
                if (!showCyberMap) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Locate Me Button
                        SmallFloatingActionButton(
                            onClick = {
                                userLocation?.let {
                                    scope.launch {
                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 16f))
                                    }
                                }
                            },
                            containerColor = Color.Black.copy(alpha = 0.8f),
                            contentColor = RoyalGold,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.MyLocation, "Locate Me")
                        }

                        // Zoom In Button
                        SmallFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                                }
                            },
                            containerColor = Color.Black.copy(alpha = 0.8f),
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, "Zoom In")
                        }

                        // Zoom Out Button
                        SmallFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                                }
                            },
                            containerColor = Color.Black.copy(alpha = 0.8f),
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Remove, "Zoom Out")
                        }
                    }
                }
                
                // NAV INFO CARD
                if (isNavigating) {
                     Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DESTINATION TARGET", color = Color.Gray, fontSize = 10.sp)
                                    Text(
                                        if(distanceToDest > 1000) String.format("%.1f KM", distanceToDest / 1000) else "${distanceToDest.toInt()} M",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                // Threshold Toggle
                                TextButton(onClick = { reminderThreshold = if(reminderThreshold == 50000.0) 100000.0 else 50000.0 }) {
                                    Text("ALERT AT: ${reminderThreshold.toInt()/1000}KM", color = RoyalGold, fontSize = 10.sp)
                                }

                                // External Map Button
                                IconButton(
                                    onClick = {
                                        navigationDestination?.let { dest ->
                                            val gmmIntentUri = Uri.parse("google.navigation:q=${dest.latitude},${dest.longitude}")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            mapIntent.setPackage("com.google.android.apps.maps")
                                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                                context.startActivity(mapIntent)
                                            } else {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri)) // Fallback (browser)
                                            }
                                        }
                                    },
                                    modifier = Modifier.background(RoyalGold, CircleShape).size(40.dp)
                                ) {
                                    Icon(Icons.Default.Directions, "Google Maps", tint = Color.Black)
                                }
                            }

                            // Icon Selector Row
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Text("ICON STYLE: ", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterVertically))
                                listOf("ARROW", "SHIELD", "DRONE").forEachIndexed { index, name ->
                                    FilterChip(
                                        selected = navigationIconType == index,
                                        onClick = { navigationIconType = index },
                                        label = { Text(name, fontSize = 9.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = RoyalGold.copy(alpha = 0.2f),
                                            selectedLabelColor = RoyalGold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // ── REAL-TIME TRACKER DEBUG OVERLAY ──────────────────────
            if (showDebugOverlay) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 160.dp)
                        .width(220.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xCC000000) // 80% opacity black
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.verticalGradient(listOf(RoyalGold, Color(0xFF00E5FF)))
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Pulsing green dot
                                val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800),
                                        repeatMode = RepeatMode.Reverse
                                    ), label = "pulseAlpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Green.copy(alpha = pulseAlpha))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "LIVE TRACKER",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            IconButton(
                                onClick = { showDebugOverlay = false },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }

                        HorizontalDivider(color = RoyalGold.copy(alpha = 0.3f), thickness = 0.5.dp)

                        // Location Data
                        DebugRow("LAT", userLocation?.let { String.format("%.6f", it.latitude) } ?: "Waiting...")
                        DebugRow("LNG", userLocation?.let { String.format("%.6f", it.longitude) } ?: "Waiting...")
                        DebugRow("SPEED", currentSpeed)
                        DebugRow("ACCURACY", currentAccuracy)
                        DebugRow("ALTITUDE", currentAltitude)
                        DebugRow("PROVIDER", currentProvider)

                        HorizontalDivider(color = Color(0xFF00E5FF).copy(alpha = 0.3f), thickness = 0.5.dp)

                        // Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("UPDATES", color = Color.Gray, fontSize = 8.sp)
                                Text(
                                    "$locationUpdateCount",
                                    color = RoyalGold,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("LAST UPDATE", color = Color.Gray, fontSize = 8.sp)
                                Text(
                                    lastUpdateTime,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Toggle debug overlay button (always visible)
            if (!showDebugOverlay) {
                SmallFloatingActionButton(
                    onClick = { showDebugOverlay = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 160.dp),
                    containerColor = Color.Black.copy(alpha = 0.8f),
                    contentColor = Color(0xFF00E5FF),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.BugReport, "Show Tracker")
                }
            }

            // ARRIVAL DIALOG REMINDER
            if (showArrivalAlert) {
                AlertDialog(
                    onDismissRequest = { showArrivalAlert = false },
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, null, tint = RoyalGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if(distanceToDest < 500) "TARGET REACHED" else "PROXIMITY ALERT") 
                        }
                    },
                    text = { 
                        Text(
                            if(distanceToDest < 500) "You have reached your destination." 
                            else "You are within ${reminderThreshold.toInt()/1000}KM of your destination sector. Prepare for arrival."
                        ) 
                    },
                    confirmButton = {
                        TextButton(onClick = { 
                            showArrivalAlert = false 
                            if(distanceToDest < 500) isNavigating = false
                        }) {
                            Text("ACKNOWLEDGE")
                        }
                    },
                    containerColor = Color(0xFF222222),
                    titleContentColor = RoyalGold,
                    textContentColor = Color.White
                )
            }
        }
    }
}

// ── Debug Overlay Row ───────────────────────────────────────
@Composable
fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Color(0xFF00E5FF).copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End
        )
    }
}

// Helper to convert Resource to BitmapDescriptor for custom icons
fun painterToBitmapDescriptor(context: android.content.Context, resId: Int, sizeDp: Int): BitmapDescriptor {
    val px = (sizeDp * context.resources.displayMetrics.density).toInt()
    val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, resId)
    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, px, px, false)
    return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
}

// Helper methods from older file retained below...
// (calculateDistance, TabButton, ThreatCard, TrafficAlertCard) -> These need to remain.


@Composable
fun TabButton(label: String, isSelected: Boolean, count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) RoyalGold.copy(alpha = 0.2f) else Color(0xFF2A2A2A),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, RoyalGold) else null,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = if (isSelected) RoyalGold else Color.LightGray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = if (isSelected) RoyalGold else Color.Gray
            ) {
                Text(
                    count.toString(),
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ThreatCard(
    threat: ThreatAlert,
    isSelected: Boolean,
    onClick: () -> Unit,
    onNavigate: () -> Unit
) {
    val cardColor = when (threat.type) {
        ThreatType.THEFT -> Color(0xFFFF5252)
        ThreatType.MANIFESTATION -> Color(0xFFFF9800)
        ThreatType.SUSPICIOUS -> Color(0xFFFFEB3B)
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Background Image using RoyalFrameCard or custom image for list items
        Image(
            painter = painterResource(id = com.royalshield.app.R.drawable.card_frame_gold),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds
        )

        // Content
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = cardColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(50.dp) // Larger icon
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = threat.label,
                                color = cardColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = threat.type.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${String.format("%.4f", threat.lat)}, ${String.format("%.4f", threat.lon)}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                
                IconButton(
                    onClick = onNavigate,
                    modifier = Modifier
                        .size(48.dp)
                        .background(RoyalGold.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Navigation, "Navigate", tint = RoyalGold, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun TrafficAlertCard(
    alert: TrafficAlert,
    isSelected: Boolean,
    userLocation: LatLng?,
    onClick: () -> Unit
) {
    val distance = userLocation?.let {
        calculateDistance(it.latitude, it.longitude, alert.lat, alert.lon)
    }
    
    val cardColor = when (alert.type) {
        TrafficAlertType.TRAFFIC_CAMERA -> Color(0xFF2196F3)
        TrafficAlertType.POLICE -> Color(0xFF00BCD4)
        TrafficAlertType.SPEED_TRAP -> Color(0xFFFF9800)
        TrafficAlertType.ACCIDENT -> Color(0xFFFF5252)
        TrafficAlertType.TRAFFIC_JAM -> Color(0xFFFFEB3B)
        TrafficAlertType.ROAD_HAZARD -> Color(0xFF9C27B0)
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
         // Background Image
        Image(
            painter = painterResource(id = com.royalshield.app.R.drawable.card_frame_gold),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds
        )
        
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = cardColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(alert.icon, null, tint = cardColor, modifier = Modifier.size(24.dp))
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.description,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    distance?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MyLocation, null, tint = RoyalGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (it < 1000) "${it.toInt()}m away" else "${String.format("%.1f", it / 1000)}km away",
                                color = RoyalGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Calculate distance between two lat/lon points in meters
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000.0 // meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}
