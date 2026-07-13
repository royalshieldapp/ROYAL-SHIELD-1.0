@file:OptIn(MapsComposeExperimentalApi::class)

package com.royalshield.app.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.royalshield.app.R
import com.royalshield.app.SosManager
import com.royalshield.app.features.trackingshield.TrackingShieldViewModel
import com.royalshield.app.features.trackingshield.data.ChildStatus
import com.royalshield.app.features.trackingshield.data.TrackingMode

// ── Design Tokens (local) ──────────────────────────────────
private val BgBase       = Color(0xFF07070A)
private val NeonCyan     = Color(0xFF00E5FF)
private val NeonGold     = Color(0xFFFFD700)
private val NeonGreen    = Color(0xFF00FF9C)
private val NeonRed      = Color(0xFFFF1E1E)
private val NeonPurple   = Color(0xFFBF5AF2)
private val TsTextPrimary  = Color(0xFFF0F4FF)

@Composable
fun TrackingShieldScreen(
    onBack: () -> Unit = {},
    vm: TrackingShieldViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    val centerRequest by vm.centerRequest.collectAsState()

    // Handle physical back button
    BackHandler(enabled = true) {
        onBack()
    }

    // ── Role Selection Check ───────────────────────────────
    if (state.deviceRole == "UNSET") {
        RoleSelectionScreen(
            onBack = onBack,
            onRoleSelected = { role -> vm.setRole(role) }
        )
        return // Stop rendering the rest of the screen
    }

    // ── Permissions ─────────────────────────────────────────
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ── Child Specific Permissions (Background + Notifications) ──
    LaunchedEffect(state.deviceRole, hasLocationPermission) {
        if (state.deviceRole == "CHILD" && hasLocationPermission) {
            vm.startChildLocationService()
        }
    }

    if (state.deviceRole == "CHILD" && hasLocationPermission) {
        val bgPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* Validated on next check */ }

        LaunchedEffect(Unit) {
            // 1. Post Notifications (Android 13+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    bgPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            
            // 2. Background Location (Android 10+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                     // Note: You should show a rationale dialog before this on Android 11+
                     // For MVP we request directly, but OS might block if not handled carefully.
                    bgPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
        }
    }

    // ── Map Style ───────────────────────────────────────────
    val mapStyleOptions = null // Default Light Map as requested by user
    /*
    val mapStyleOptions = remember {
        try {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.tracking_dark_map_style)
        } catch (_: Exception) { null }
    }
    */

    // ── Camera ──────────────────────────────────────────────
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(25.7617, -80.1918), 15f)
    }

    // Center on child when requested
    LaunchedEffect(centerRequest) {
        centerRequest?.let { target ->
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 16f), 800)
            vm.consumeCenterRequest()
        }
    }

    // Auto-center on first child data
    var hasAutoCentered by remember { mutableStateOf(false) }
    LaunchedEffect(state.child) {
        if (!hasAutoCentered && state.child != null) {
            hasAutoCentered = true
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(state.child!!.latLng, 15f), 1000
            )
        }
    }

    // ── Child Halo Animation ────────────────────────────────
    val haloTransition = rememberInfiniteTransition(label = "halo")
    val haloAlpha by haloTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )
    val haloScale by haloTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )

    // Derived: is LIVE or NAV mode
    val isLiveOrNav = state.mode == TrackingMode.LIVE || state.mode == TrackingMode.NAV

    // ═══════════════════════════════════════════════════════
    // SCREEN LAYOUT
    // ═══════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
    ) {
        // ── LAYER 1: MAP ────────────────────────────────────
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = mapStyleOptions,
                isMyLocationEnabled = hasLocationPermission,
                mapType = MapType.NORMAL
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false,
                mapToolbarEnabled = false
            ),
            onMapLongClick = { latLng ->
                if (state.mode == TrackingMode.LIVE) {
                    vm.startAddZone(latLng)
                }
            }
        ) {
            // ── DRAFT ZONE (Phase 3) ────────────────────────
            if (state.isAddingZone) {
                state.draftZone?.let { draft ->
                    Circle(
                        center = draft.center,
                        radius = draft.radiusMeters,
                        fillColor = zoneColor(draft.type).copy(alpha = 0.2f),
                        strokeColor = zoneColor(draft.type),
                        strokeWidth = 4f
                    )
                    Marker(
                        state = MarkerState(position = draft.center),
                        title = "New Safe Zone",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
            }
            // ── Safe/Risk Zone Circles (LIVE mode only) ─────
            if (state.mode == TrackingMode.LIVE) {
                state.safeZones.forEach { zone ->
                    val color = zoneColor(zone.type)
                    Circle(
                        center = zone.center,
                        radius = zone.radiusMeters,
                        fillColor = color.copy(alpha = 0.08f),
                        strokeColor = color.copy(alpha = 0.6f),
                        strokeWidth = 2f
                    )
                    Marker(
                        state = MarkerState(position = zone.center),
                        title = zone.name,
                        snippet = "${zone.radiusMeters.toInt()}m radius",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            when (zone.type) {
                                com.royalshield.app.features.trackingshield.data.ZoneType.HOME -> BitmapDescriptorFactory.HUE_CYAN
                                com.royalshield.app.features.trackingshield.data.ZoneType.SCHOOL -> BitmapDescriptorFactory.HUE_YELLOW
                                com.royalshield.app.features.trackingshield.data.ZoneType.PARK -> BitmapDescriptorFactory.HUE_GREEN
                                com.royalshield.app.features.trackingshield.data.ZoneType.CUSTOM -> BitmapDescriptorFactory.HUE_VIOLET
                            }
                        ),
                        alpha = 0.7f
                    )
                }

                state.riskZones.forEach { risk ->
                    Circle(
                        center = risk.center,
                        radius = risk.radiusMeters,
                        fillColor = NeonRed.copy(alpha = 0.12f),
                        strokeColor = NeonRed.copy(alpha = 0.7f),
                        strokeWidth = 3f
                    )
                }
            } // Close if (state.mode == TrackingMode.LIVE)

            // ── Forecast Engine Risk Data ───────────────────
            if (state.mode == TrackingMode.LIVE || state.mode == TrackingMode.NAV) {
                state.hotspots.forEach { hotspot ->
                    val color = when(hotspot.riskLevel.uppercase()) {
                        "CRITICAL" -> NeonRed
                        "HIGH" -> androidx.compose.ui.graphics.Color(0xFFFF5722)
                        "MEDIUM" -> NeonGold
                        else -> NeonGreen
                    }
                    Circle(
                        center = LatLng(hotspot.center.lat, hotspot.center.lng),
                        radius = hotspot.radiusMeters,
                        fillColor = color.copy(alpha = 0.2f),
                        strokeColor = color.copy(alpha = 0.8f),
                        strokeWidth = 3f
                    )
                    Marker(
                        state = MarkerState(position = LatLng(hotspot.center.lat, hotspot.center.lng)),
                        title = "Predicted Risk: ${hotspot.riskLevel}",
                        snippet = "${hotspot.eventCount} incidents",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            when(hotspot.riskLevel.uppercase()) {
                                "CRITICAL" -> BitmapDescriptorFactory.HUE_RED
                                "HIGH" -> BitmapDescriptorFactory.HUE_ORANGE
                                "MEDIUM" -> BitmapDescriptorFactory.HUE_YELLOW
                                else -> BitmapDescriptorFactory.HUE_GREEN
                            }
                        ),
                        alpha = 0.7f
                    )
                }
            }

            // ── CHILD MARKER (LIVE + NAV) ───────────────────
            if (isLiveOrNav) {
                state.child?.let { child ->
                    val haloColor = childHaloColor(child.status)

                    Circle(
                        center = child.latLng,
                        radius = child.accuracyMeters.toDouble(),
                        fillColor = haloColor.copy(alpha = 0.06f),
                        strokeColor = haloColor.copy(alpha = 0.2f),
                        strokeWidth = 1f
                    )
                    Circle(
                        center = child.latLng,
                        radius = (30.0 * haloScale),
                        fillColor = haloColor.copy(alpha = haloAlpha * 0.3f),
                        strokeColor = haloColor.copy(alpha = haloAlpha),
                        strokeWidth = 2f
                    )
                    Marker(
                        state = MarkerState(position = child.latLng),
                        title = child.name,
                        snippet = when (child.status) {
                            ChildStatus.SAFE -> "✅ Safe & Active"
                            ChildStatus.WARNING -> "⚠️ Check status"
                            ChildStatus.DANGER -> "\uD83D\uDEA8 ALERT!"
                        },
                        icon = BitmapDescriptorFactory.defaultMarker(
                            when (child.status) {
                                ChildStatus.SAFE -> BitmapDescriptorFactory.HUE_GREEN
                                ChildStatus.WARNING -> BitmapDescriptorFactory.HUE_YELLOW
                                ChildStatus.DANGER -> BitmapDescriptorFactory.HUE_RED
                            }
                        )
                    )
                }
            }

            // ── NAV MODE: Route Polyline + Destination ──────
            if (state.mode == TrackingMode.NAV) {
                state.navRoute?.let { route ->
                    Polyline(
                        points = route.waypoints,
                        color = NeonCyan,
                        width = 8f,
                        pattern = listOf(Dash(20f), Gap(10f))
                    )
                    Marker(
                        state = MarkerState(position = route.destination),
                        title = route.destinationName,
                        snippet = "${route.etaMinutes} min",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_ORANGE
                        )
                    )
                }
            }

            // ── HISTORY MODE: Breadcrumb Trail ──────────────
            if (state.mode == TrackingMode.HISTORY) {
                state.activeSession?.let { session ->
                    // Breadcrumb trail polyline
                    if (session.breadcrumbs.size >= 2) {
                        Polyline(
                            points = session.breadcrumbs.map { it.latLng },
                            color = NeonGreen,
                            width = 6f
                        )
                    }

                    // Speed-colored segments (green = slow, gold = fast)
                    session.breadcrumbs.zipWithNext().forEach { (a, b) ->
                        val segColor = if (a.speedKmh > 5f) NeonGold else NeonGreen
                        Polyline(
                            points = listOf(a.latLng, b.latLng),
                            color = segColor.copy(alpha = 0.7f),
                            width = 5f
                        )
                    }

                    // Start marker
                    session.breadcrumbs.firstOrNull()?.let { start ->
                        Marker(
                            state = MarkerState(position = start.latLng),
                            title = "Start",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_GREEN
                            ),
                            alpha = 0.8f
                        )
                    }

                    // End marker
                    session.breadcrumbs.lastOrNull()?.let { end ->
                        Marker(
                            state = MarkerState(position = end.latLng),
                            title = "End",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_RED
                            ),
                            alpha = 0.8f
                        )
                    }

                    // Animated playback position marker
                    val totalPoints = session.breadcrumbs.size
                    val currentIndex = (state.playbackProgress * (totalPoints - 1)).toInt()
                        .coerceIn(0, totalPoints - 1)
                    val currentPos = session.breadcrumbs[currentIndex].latLng

                    Circle(
                        center = currentPos,
                        radius = 15.0,
                        fillColor = NeonCyan.copy(alpha = 0.5f),
                        strokeColor = NeonCyan,
                        strokeWidth = 3f
                    )
                    Marker(
                        state = MarkerState(position = currentPos),
                        title = "Position",
                        snippet = "${(state.playbackProgress * 100).toInt()}%",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_CYAN
                        )
                    )
                }
            }
        }

        // ── Loading Overlay ─────────────────────────────────
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NeonCyan, strokeWidth = 2.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Acquiring GPS signal...", color = TsTextPrimary, fontSize = 13.sp)
                }
            }
        }

        // ── LAYER 3: TOP UI STACK ───────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            // Back button row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TsTextPrimary
                    )
                }
                Text(
                    "TRACKING SHIELD",
                    color = NeonCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.clickable { onBack() }
                )
                Spacer(Modifier.weight(1f))
                
                // Extra Home Button for clarity
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Home",
                        tint = NeonCyan
                    )
                }

                // Child name badge (Dropdown for selection)
                val children = state.children
                var showChildMenu by remember { mutableStateOf(false) }

                if (children.isNotEmpty()) {
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showChildMenu = true }
                                .padding(4.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonPinCircle,
                                contentDescription = null,
                                tint = state.child?.status?.let { childHaloColor(it) } ?: TsTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                state.child?.name ?: "Select Child",
                                color = TsTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (children.size > 1) {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = TsTextPrimary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showChildMenu,
                            onDismissRequest = { showChildMenu = false },
                            modifier = Modifier.background(BgBase)
                        ) {
                            children.forEach { child ->
                                DropdownMenuItem(
                                    text = { Text(child.name, color = TsTextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = childHaloColor(child.status)
                                        )
                                    },
                                    onClick = {
                                        vm.selectChild(child)
                                        showChildMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Mode Chips Row (always visible)
            ModeChipsRow(
                currentMode = state.mode,
                onModeSelected = { vm.switchMode(it) }
            )

            Spacer(Modifier.height(6.dp))

            // Status bar (LIVE mode only)
            AnimatedVisibility(
                visible = state.mode == TrackingMode.LIVE,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                StatusTopBar(
                    child = state.child,
                    lastUpdateText = state.lastUpdateText
                )
            }

            // Nav Banner (NAV mode only)
            AnimatedVisibility(
                visible = state.mode == TrackingMode.NAV && state.navRoute != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                state.navRoute?.let { route ->
                    NavBanner(navRoute = route)
                }
            }
        }

        // ── LAYER 4: BOTTOM UI STACK ────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // HISTORY MODE: Playback Controls
            AnimatedVisibility(
                visible = state.mode == TrackingMode.HISTORY && state.activeSession != null,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                PlaybackControls(
                    isPlaying = state.isPlaybackPlaying,
                    progress = state.playbackProgress,
                    speed = state.playbackSpeed,
                    activeSession = state.activeSession,
                    onTogglePlayback = { vm.togglePlayback() },
                    onCycleSpeed = { vm.cyclePlaybackSpeed() },
                    onSeek = { vm.seekPlayback(it) }
                )
            }

            // HISTORY MODE: Timeline Session List
            AnimatedVisibility(
                visible = state.mode == TrackingMode.HISTORY,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                HistoryTimelineSheet(
                    sessions = state.historySessions,
                    activeSession = state.activeSession,
                    onSessionSelected = { vm.selectHistorySession(it) }
                )
            }

            // LIVE/NAV MODE: Bottom Dock
            AnimatedVisibility(
                visible = isLiveOrNav,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                BottomDock(
                    onHome = onBack,
                    onCenter = { vm.centerOnChild() },
                    onHistory = { vm.switchMode(TrackingMode.HISTORY) },
                    onZones = {
                        vm.toggleZoneSheet(!state.isZoneSheetOpen)
                    },
                    onControl = {
                        vm.toggleControlPanel(!state.isControlPanelOpen)
                    },
                    onSOS = {
                        vm.triggerSOS()
                        Toast.makeText(context, "\uD83D\uDEA8 SOS Triggered!", Toast.LENGTH_LONG).show()
                    }
                )
            }

            // PARENT MODE: Safe Zones Sheet
            AnimatedVisibility(
                visible = state.isZoneSheetOpen,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                SafeZonesSheet(
                    safeZones = state.safeZones,
                    riskZones = state.riskZones,
                    onDeleteSafe = { vm.deleteSafeZone(it) },
                    onDeleteRisk = { vm.deleteRiskZone(it) },
                    onClose = { vm.toggleZoneSheet(false) }
                )
            }

            // PARENT MODE: Add Child FAB
            AnimatedVisibility(
                visible = isLiveOrNav && state.deviceRole == "PARENT",
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                 Box(modifier = Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 80.dp), contentAlignment = Alignment.BottomEnd) {
                    FloatingActionButton(
                        onClick = { vm.generatePairingCode() },
                        containerColor = com.royalshield.app.ui.theme.RoyalGold,
                        contentColor = Color.Black
                    ) {
                        Icon(Icons.Default.PersonAdd, "Add Child")
                    }
                }
            }

            // PARENT MODE: Child Control Panel Sheet
            AnimatedVisibility(
                visible = state.isControlPanelOpen,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                ChildControlPanelSheet(
                    child = state.child,
                    currentGpsMode = state.child?.gpsMode ?: com.royalshield.app.features.trackingshield.data.GpsMode.BALANCED,
                    onGpsModeSelected = { vm.setGpsMode(it) },
                    onRefreshLocation = { vm.refreshChildLocation() },
                    onPlayAlertSound = {
                        state.child?.let { child ->
                            vm.playAlertSound(child.id)
                            android.widget.Toast.makeText(context, "Alert sound command sent to ${child.name}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClose = { vm.toggleControlPanel(false) }
                )
            }
        }

        // ── Pairing Dialog (Overlay) ────────────────────────
        state.pairingCode?.let { code ->
            PairingDialog(code = code, onDismiss = { vm.dismissPairingDialog() })
        }

        // ── Phase 3: Add Zone Dialog ──
        if (state.isAddingZone && state.draftZone != null) {
            AddZoneDialog(
                draftZone = state.draftZone!!,
                onUpdate = { name, radius, type -> vm.updateDraftZone(name, radius, type) },
                onConfirm = { vm.confirmAddZone() },
                onCancel = { vm.cancelAddZone() }
            )
        }

        // ── Speed / Status Chip (LIVE + NAV only) ───────────
        if (isLiveOrNav) {
            state.child?.let { child ->
                if (child.speedKmh > 1f) {
                    GlassmorphismSurface(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp),
                        cornerRadius = 12.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${child.speedKmh.toInt()}",
                                color = NeonCyan,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "km/h",
                                color = Color(0x8CF0F4FF),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
