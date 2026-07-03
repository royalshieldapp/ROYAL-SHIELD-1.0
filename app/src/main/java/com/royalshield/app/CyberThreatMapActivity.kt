package com.royalshield.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.royalshield.app.features.riskprediction.data.remote.Hotspot
import com.royalshield.app.features.riskprediction.data.remote.RiskFeature
import com.royalshield.app.features.riskprediction.ui.*
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.Royal_shieldTheme
import kotlinx.coroutines.delay

class CyberThreatMapActivity : ComponentActivity() {
    
    // Initialize RiskMapViewModel
    private val riskViewModel: RiskMapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Royal_shieldTheme {
                CyberThreatMapScreen(
                    onBack = { finish() },
                    riskViewModel = riskViewModel
                )
            }
        }
    }
}

enum class MapTab { THREATS, TRAFFIC, SAFE_ROUTES, REPORTS }

@Composable
fun CyberThreatMapScreen(
    onBack: () -> Unit,
    riskViewModel: RiskMapViewModel
) {
    val riskState by riskViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf(MapTab.THREATS) }
    var shieldModeActive by remember { mutableStateOf(false) }
    var showRiskHeatmap by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf(LatLng(25.7617, -80.1918)) } // Default
    var isInitialLocationSet by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        
        while(true) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            userLocation = LatLng(loc.latitude, loc.longitude)
                            isInitialLocationSet = true
                        }
                    }
            }
            delay(15000)
        }
    }

    val cameraPositionState = com.google.maps.android.compose.rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(userLocation, 12f)
    }

    LaunchedEffect(isInitialLocationSet) {
        if (isInitialLocationSet) {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(userLocation, 14f)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        
        // 1. MAP LAYER
        MapSimulation(
            cameraPositionState = cameraPositionState,
            tab = activeTab,
            shieldActive = shieldModeActive,
            showRiskHeatmap = showRiskHeatmap,
            riskZones = riskState.riskZones,
            hotspots = riskState.hotspots,
            userLocation = userLocation,
            onMapClick = { latLng -> 
                if (showRiskHeatmap) {
                    riskViewModel.onMapClick(latLng.latitude, latLng.longitude)
                }
            }
        )

        // 2. HEADS UP DISPLAY (HUD)
        Column(modifier = Modifier.fillMaxSize()) {
            
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .border(1.dp, RoyalGold.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = RoyalGold)
                }

                // Title
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .border(1.dp, RoyalGold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, null, tint = RoyalGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("THREAT COMMAND", color = RoyalGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                
                // RIGHT CONTROLS
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    
                    // Risk Heatmap Toggle (New)
                    IconButton(
                        onClick = { 
                            showRiskHeatmap = !showRiskHeatmap
                            if (showRiskHeatmap) {
                                // Load data for user area
                                val offset = 0.1
                                riskViewModel.loadRiskMap(
                                    userLocation.latitude - offset, 
                                    userLocation.longitude - offset, 
                                    userLocation.latitude + offset, 
                                    userLocation.longitude + offset
                                )
                                riskViewModel.loadHotspots(
                                    userLocation.latitude - offset, 
                                    userLocation.longitude - offset, 
                                    userLocation.latitude + offset, 
                                    userLocation.longitude + offset
                                )
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if(showRiskHeatmap) Color(0xFFFF3B30).copy(alpha=0.3f) 
                                else Color.Black.copy(alpha=0.6f), 
                                CircleShape
                            )
                            .border(
                                1.dp, 
                                if(showRiskHeatmap) Color(0xFFFF3B30) 
                                else RoyalGold.copy(alpha=0.3f), 
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Layers, 
                            "Risk Map", 
                            tint = if(showRiskHeatmap) Color(0xFFFF3B30) else RoyalGold
                        )
                    }

                    // Shield Toggle
                    IconButton(
                        onClick = { shieldModeActive = !shieldModeActive },
                        modifier = Modifier
                            .size(42.dp)
                            .background(if(shieldModeActive) Color(0xFF00E676).copy(alpha=0.2f) else Color.Black.copy(alpha=0.6f), CircleShape)
                            .border(1.dp, if(shieldModeActive) Color(0xFF00E676) else RoyalGold.copy(alpha=0.3f), CircleShape)
                    ) {
                        Icon(Icons.Default.Security, "Shield", tint = if(shieldModeActive) Color(0xFF00E676) else RoyalGold)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            // Risk Legend (Only visible when heatmap active)
            if (showRiskHeatmap) {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp, bottom = 200.dp) // Positioned above bottom panel
                        .align(Alignment.End)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .border(1.dp, RoyalGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("RISK LEVEL", color = RoyalGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        RiskLegendItem(Color(0xFFFF3B30), "CRITICAL")
                        RiskLegendItem(Color(0xFFFF9800), "HIGH")
                        RiskLegendItem(Color(0xFFFFEB3B), "MEDIUM")
                        RiskLegendItem(Color(0xFF00E676), "LOW")
                    }
                }
            }
        }

        // 3. SLIDE-UP PANEL (THREAT COMMAND PANEL)
        // Hide this panel if the Risk Explanation Sheet is showing
        if (riskState.selectedZone == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f) // Takes up bottom 45%
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E1E24).copy(alpha = 0.98f),
                                Color(0xFF0F1014)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(RoyalGold.copy(alpha=0.5f), Color.Transparent)),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Drag Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
    
                    // TABS ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PanelTab("THREATS", activeTab == MapTab.THREATS) { activeTab = MapTab.THREATS }
                        PanelTab("TRAFFIC", activeTab == MapTab.TRAFFIC) { activeTab = MapTab.TRAFFIC }
                        PanelTab("SAFE ROUTES", activeTab == MapTab.SAFE_ROUTES) { activeTab = MapTab.SAFE_ROUTES }
                        PanelTab("REPORTS", activeTab == MapTab.REPORTS) { activeTab = MapTab.REPORTS }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))
    
                    // PANEL CONTENT
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Show Loading or Error logic if needed
                        if (riskState.isLoading && showRiskHeatmap) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = RoyalGold,
                                trackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        }
    
                        when (activeTab) {
                            MapTab.THREATS -> {
                                PanelHeader("LIVE THREAT RADAR")
                                
                                if (riskState.hotspots.isEmpty()) {
                                    Text(
                                        "No immediate threats detected in your sector. Monitoring...",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                } else {
                                    riskState.hotspots.forEach { hotspot ->
                                        // Real Dynamic Card
                                        ThreatDetailedCard(
                                            type = hotspot.riskLevel.uppercase(),
                                            typeColor = getRiskColorFromLevel(hotspot.riskLevel),
                                            distance = "${hotspot.eventCount} recent incidents",
                                            time = "Updated: Just now",
                                            icon = when(hotspot.riskLevel) {
                                                "CRITICAL" -> Icons.Default.Warning
                                                "HIGH" -> Icons.Default.Security
                                                else -> Icons.Default.Visibility
                                            },
                                            onClick = {
                                                // Animate camera to hotspot
                                                val target = LatLng(hotspot.center.lat, hotspot.center.lng)
                                                coroutineScope.launch {
                                                    cameraPositionState.animate(
                                                        com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(target, 15f)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                                
                                // Keep a smaller, real-connected version of the user request layout
                                if (riskState.hotspots.any { it.riskLevel == "CRITICAL" }) {
                                    ThreatDetailedCard(
                                        type = "SYSTEM WARNING",
                                        typeColor = Color(0xFFFF3B30),
                                        distance = "Active Threat Perimeter",
                                        time = "Real-time sync",
                                        icon = Icons.Default.Error
                                    )
                                }
                            }
                            MapTab.TRAFFIC -> {
                                PanelHeader("TRAFFIC CONDITIONS")
                                Text("Traffic is flowing normally in your safe zone.", color = Color.Gray)
                            }
                            MapTab.SAFE_ROUTES -> {
                                PanelHeader("NAVIGATION")
                                Button(
                                    onClick = {},
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("NAVIGATE SAFEST ROUTE HOME", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                            MapTab.REPORTS -> {
                                PanelHeader("COMMUNITY REPORTS")
                                ThreatDetailedCard(
                                    type = "ASSAULT",
                                    typeColor = Color(0xFFFF3B30),
                                    distance = "1.2km away",
                                    time = "15 min ago",
                                    icon = Icons.Default.Report,
                                    onClick = {
                                        // Mock relocation near user
                                        coroutineScope.launch {
                                            cameraPositionState.animate(
                                                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(userLocation.latitude + 0.0033, userLocation.longitude - 0.0032), 16f
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 4. RISK EXPLANATION SHEET (NEW)
        // Slides up when a zone is selected
        if (riskState.selectedZone != null) {
            RiskExplanationSheet(
                zoneDetails = riskState.selectedZone!!,
                onDismiss = { riskViewModel.clearSelection() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun RiskLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(label, color = Color.White, fontSize = 9.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
    }
}

fun getRiskColorFromLevel(level: String): Color {
    return when(level) {
        "CRITICAL" -> Color(0xFFFF3B30)
        "HIGH" -> Color(0xFFFF9800)
        "MEDIUM" -> Color(0xFFFFEB3B)
        else -> Color(0xFF00E676)
    }
}

@Composable
fun ThreatDetailedCard(
    type: String,
    typeColor: Color,
    distance: String,
    time: String,
    icon: ImageVector,
    onClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(typeColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = typeColor, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type,
                    color = typeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍 $distance", color = Color.Gray, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🕒 $time", color = Color.Gray, fontSize = 12.sp)
                }
            }
            
            // Action Button
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    "View on Map",
                    color = RoyalGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MapSimulation(
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    tab: MapTab, 
    shieldActive: Boolean,
    showRiskHeatmap: Boolean = false,
    riskZones: List<RiskFeature> = emptyList(),
    hotspots: List<Hotspot> = emptyList(),
    userLocation: LatLng,
    onMapClick: (LatLng) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Custom Dark Map Style
    val mapStyleOptions = remember {
        com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
    }

    // Map Properties
    val mapProperties by remember {
        mutableStateOf(
            com.google.maps.android.compose.MapProperties(
                isMyLocationEnabled = false, // True if permission granted (handled elsewhere)
                mapStyleOptions = mapStyleOptions,
                mapType = com.google.maps.android.compose.MapType.NORMAL
            )
        )
    }

    val mapUiSettings by remember {
        mutableStateOf(
            com.google.maps.android.compose.MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = false,
                myLocationButtonEnabled = false
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        
        com.google.maps.android.compose.GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapClick = onMapClick
        ) {
            // User Marker (Gold)
            Marker(
                state = MarkerState(position = userLocation),
                title = "You",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
            )

            // Threat Markers (Standard)
            if (tab == MapTab.THREATS || tab == MapTab.REPORTS) {
                // Mock Theft near user
                Marker(
                    state = MarkerState(position = LatLng(userLocation.latitude + 0.0033, userLocation.longitude - 0.0032)),
                    title = "THEFT REPORTED",
                    snippet = "450m away - 2 min ago",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }
            
            // ========== RISK HEATMAP LAYER ==========
            if (showRiskHeatmap && riskZones.isNotEmpty()) {
                val weightedPoints = remember(riskZones) {
                    riskZonesToWeightedPoints(riskZones)
                }
                
                if (weightedPoints.isNotEmpty()) {
                    val heatmapProvider by produceState<com.google.maps.android.heatmaps.HeatmapTileProvider?>(initialValue = null, weightedPoints) {
                        try {
                            value = createHeatmapProvider(weightedPoints)
                        } catch (e: Exception) {
                            android.util.Log.e("CyberThreatMap", "Error creating heatmap", e)
                        }
                    }
                    
                    heatmapProvider?.let { provider ->
                        com.google.maps.android.compose.TileOverlay(
                            tileProvider = provider
                        )
                    }
                }
            }

            // ========== HOTSPOT MARKERS ==========
            if (showRiskHeatmap && hotspots.isNotEmpty()) {
                hotspots.forEach { hotspot ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(hotspot.center.lat, hotspot.center.lng)
                        ),
                        title = "HOTSPOT: ${hotspot.riskLevel}",
                        snippet = "${hotspot.eventCount} incidents",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            when(hotspot.riskLevel) {
                                "CRITICAL" -> BitmapDescriptorFactory.HUE_RED
                                "HIGH" -> BitmapDescriptorFactory.HUE_ORANGE
                                else -> BitmapDescriptorFactory.HUE_YELLOW
                            }
                        )
                    )
                }
            }
        }

        // Radar / Shield Overlay (Visual Effect only)
        if(shieldActive) {
             Canvas(modifier = Modifier.fillMaxSize()) {
                 val w = size.width
                 drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00E676).copy(alpha=0.15f), Color.Transparent),
                        center = center,
                        radius = w * 0.6f
                    ),
                    radius = w * 0.6f,
                    center = center
                )
             }
        }
    }
}

@Composable
fun BoxScope.MapMarker(x: Float, y: Float, type: MarkerType, isActive: Boolean) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    
    val pulse = rememberInfiniteTransition().animateFloat(
        initialValue = 0.8f, targetValue = 1.2f, 
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .offset(x = (screenWidth * x) - 15.dp, y = (screenHeight * y) - 15.dp)
            .size(30.dp),
        contentAlignment = Alignment.Center
    ) {
        when(type) {
            MarkerType.USER -> {
                Box(
                    modifier = Modifier
                        .size(if(isActive) 200.dp else 80.dp)
                        .scale(pulse.value)
                        .background(Brush.radialGradient(listOf(RoyalGold.copy(alpha=0.2f), Color.Transparent)))
                )
                Icon(Icons.Default.Navigation, null, tint = RoyalGold, modifier = Modifier.size(24.dp).rotate(-45f))
            }
            MarkerType.THREAT_HIGH -> Icon(Icons.Default.LocationOn, null, tint = Color.Red, modifier = Modifier.size(32.dp))
            MarkerType.THREAT_MEDIUM -> Icon(Icons.Default.LocationOn, null, tint = Color(0xFFFF9800), modifier = Modifier.size(28.dp))
            MarkerType.POLICE -> Icon(Icons.Default.Star, null, tint = Color(0xFF2979FF))
            MarkerType.SAFE_ZONE -> Icon(Icons.Default.Security, null, tint = Color(0xFF00E676))
            else -> {}
        }
    }
}

@Composable
fun PanelTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text,
            color = if (selected) RoyalGold else Color.Gray,
            fontWeight = if(selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (selected) {
            Box(modifier = Modifier.width(20.dp).height(2.dp).background(RoyalGold))
        }
    }
}

@Composable
fun PanelHeader(text: String) {
    Text(text, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
}

enum class MarkerType { USER, THREAT_HIGH, THREAT_MEDIUM, POLICE, SAFE_ZONE }
