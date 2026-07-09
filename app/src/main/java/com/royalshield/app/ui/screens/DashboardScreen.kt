package com.royalshield.app.ui.screens

import android.content.Intent
import java.text.Normalizer
import java.util.regex.Pattern
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.R
import com.royalshield.app.managers.BillingManager
import com.royalshield.app.managers.PreferencesManager
import com.royalshield.app.ui.components.RoyalGradientBackground
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableIntStateOf
import com.royalshield.app.ui.theme.RoyalGold
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.royalshield.app.ui.components.ShieldStatusBackground
import com.royalshield.app.ui.components.CyberButtonRound
import com.royalshield.app.ui.components.CyberButtonRect
import com.royalshield.app.ui.theme.CyberCyan
import com.royalshield.app.ui.theme.RoyalIcons
import com.royalshield.app.ui.theme.RoyalImages
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.ui.dashboard.models.ActionItem
import com.royalshield.app.ui.dashboard.models.Severity
import com.royalshield.app.ui.dashboard.DashboardViewModel


enum class CarouselType { ALERT, PROMO, INFO, SUBSCRIPTION }

data class CarouselItem(
    val type: CarouselType,
    val title: String,
    val headline: String,
    val description: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val gradientColors: List<Color>,
    val onClick: () -> Unit
)

@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToSystemStatus: () -> Unit,
    onNavigateToSOS: () -> Unit,
    onNavigateToAutomation: () -> Unit,
    onNavigateToAiHub: () -> Unit,
    onNavigateToFileScan: () -> Unit,
    onNavigateToPhishing: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToLoyalty: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToBusinessDashboard: () -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    onNavigateToVpn: () -> Unit = {},
    onNavigateToTrackingShield: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToSoundDetection: () -> Unit = {},
    onNavigateToSecurityCamera: () -> Unit = {},
    onNavigateToSolutionEngine: () -> Unit = {},
    billingManager: BillingManager
) {
    val scrollState = rememberScrollState()
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    var showVoiceAssistant by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Neural Dashboard ViewModel
    val dashboardViewModel: DashboardViewModel = viewModel()
    val dashboardState by dashboardViewModel.state.collectAsState()


    // SOS Pulsing Animation
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background - Pure Canvas gradient (no PNG = no HWUI overload)
        Image(
            painter = painterResource(id = RoyalImages.DashboardParticles),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Subtle radial glow top-center
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A3A6A).copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                    radius = size.width * 0.8f
                )
            )
        }

        // Use transparent container so gradient shows through
        RoyalGradientBackground(containerColor = Color.Transparent) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .drawBehind {
                        // Subtle Cyber Grid
                        val gridSize = 40.dp.toPx()
                        val gridColor = Color.White.copy(alpha = 0.03f)

                        for (x in 0..(size.width / gridSize).toInt()) {
                            drawLine(gridColor, Offset(x * gridSize, 0f), Offset(x * gridSize, size.height), 1f)
                        }
                        for (y in 0..(size.height / gridSize).toInt()) {
                            drawLine(gridColor, Offset(0f, y * gridSize), Offset(size.width, y * gridSize), 1f)
                        }
                    }
            ) {

                Spacer(modifier = Modifier.height(24.dp))

                // Header
                // Header removed as per request

                Spacer(modifier = Modifier.height(12.dp))
                // TOP BAR
                val topBarContentColor = if (isLight) Color.Gray else Color.White
                val topBarSurfaceColor = if (isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.1f)
                val topBarBorderColor = if (isLight) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f)
                val showLegacyDashboardStatusStrip = false

                // Premium Status
                val hasPremium by billingManager.hasPremiumAccess.collectAsState()

                if (showLegacyDashboardStatusStrip) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Gold Lux Frame for Elite Status
                    com.royalshield.app.ui.components.GoldLuxFrame(
                        modifier = Modifier.clickable { onNavigateToPremium() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            com.royalshield.app.ui.components.GoldLuxText(
                                text = if (hasPremium) "ELITE ACTIVE" else "FREE MODE",
                                fontSize = 12
                            )
                        }
                    }

                    // Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                // Navigate to Intel Hub (Threat alerts/notifications) via Business Dashboard or similar
                                onNavigateToBusinessDashboard()
                                android.widget.Toast.makeText(context, "Redirecting to Threat Radar Alerts", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .border(1.dp, topBarBorderColor, CircleShape)
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Notifications, null, tint = topBarContentColor)
                        }
                        IconButton(
                            onClick = onNavigateToBusinessDashboard,
                            modifier = Modifier
                                .size(40.dp)
                                .border(1.dp, RoyalGold, CircleShape) // Gold border always matches
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Equalizer, null, tint = RoyalGold)
                        }
                        IconButton(
                            onClick = onNavigateToContacts,
                            modifier = Modifier
                                .size(40.dp)
                                .border(1.dp, RoyalGold, CircleShape)
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Person, null, tint = RoyalGold)
                        }
                    }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // CAROUSEL / ALERT CARD - DYNAMICALLY CONNECTED TO BACKGROUND SERVICES & PROMOTIONS (SPLIT LAYOUT)
                val isSoundEnabled = remember { PreferencesManager.isSoundEnabled() }
                val isLocationEnabled = remember { PreferencesManager.isLocationEnabled() }
                val isBgCameraEnabled = remember { PreferencesManager.isBackgroundCameraEnabled() }

                val statusItems = remember(isSoundEnabled, isLocationEnabled, isBgCameraEnabled) {
                    val items = mutableListOf<CarouselItem>()
                    // 1. Audio Shield
                    items.add(
                        CarouselItem(
                            type = CarouselType.INFO,
                            title = if (isSoundEnabled) "AUDIO ACTIVE" else "AUDIO OFF",
                            headline = "Audio Guard",
                            description = if (isSoundEnabled) "Monitoring audio" else "Tap to enable guard",
                            color = if (isSoundEnabled) Color(0xFF00FF94) else Color(0xFFFF5252),
                            icon = if (isSoundEnabled) androidx.compose.material.icons.Icons.Default.Mic else androidx.compose.material.icons.Icons.Default.MicOff,
                            gradientColors = if (isSoundEnabled) listOf(Color(0xFF00FF94), Color(0xFF00E676), Color(0xFF00FFFF)) else listOf(Color(0xFFFF5252), Color(0xFFFF1744), Color(0xFFFFD700)),
                            onClick = onNavigateToSoundDetection
                        )
                    )
                    // 2. Geospatial Guard
                    items.add(
                        CarouselItem(
                            type = CarouselType.INFO,
                            title = if (isLocationEnabled) "GPS ACTIVE" else "GPS OFF",
                            headline = "Geospatial",
                            description = if (isLocationEnabled) "Zones active" else "Tap to share GPS",
                            color = if (isLocationEnabled) Color(0xFF00E5FF) else Color(0xFFFF5252),
                            icon = if (isLocationEnabled) androidx.compose.material.icons.Icons.Default.MyLocation else androidx.compose.material.icons.Icons.Default.LocationOff,
                            gradientColors = if (isLocationEnabled) listOf(Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFF00E5FF)) else listOf(Color(0xFFFF5252), Color(0xFFFF8F00), Color(0xFFFFD700)),
                            onClick = onNavigateToMap
                        )
                    )
                    // 3. SOS Sensor
                    items.add(
                        CarouselItem(
                            type = CarouselType.INFO,
                            title = "SOS ARMED",
                            headline = "Impact Guard",
                            description = "Gravity sensors active",
                            color = Color(0xFF00E5FF),
                            icon = androidx.compose.material.icons.Icons.Default.Shield,
                            gradientColors = listOf(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF00E5FF)),
                            onClick = onNavigateToSOS
                        )
                    )
                    // 4. Emergency Camera
                    items.add(
                        CarouselItem(
                            type = CarouselType.INFO,
                            title = if (isBgCameraEnabled) "CAM ACTIVE" else "CAM OFF",
                            headline = "Surveillance",
                            description = if (isBgCameraEnabled) "Camera ready" else "Tap to arm cam",
                            color = if (isBgCameraEnabled) Color(0xFF00FF94) else Color.Gray,
                            icon = if (isBgCameraEnabled) androidx.compose.material.icons.Icons.Default.Videocam else androidx.compose.material.icons.Icons.Default.VideocamOff,
                            gradientColors = if (isBgCameraEnabled) listOf(Color(0xFF00FF94), Color(0xFF00E5FF), Color(0xFF00FF94)) else listOf(Color.Gray, Color.DarkGray, Color.Gray),
                            onClick = onNavigateToSecurityCamera
                        )
                    )
                    items
                }

                val promoItems = remember {
                    listOf(
                        CarouselItem(
                            type = CarouselType.PROMO,
                            title = "ROYAL ELITE",
                            headline = "Elite Access",
                            description = "AI, VPN & support",
                            color = RoyalGold,
                            icon = androidx.compose.material.icons.Icons.Default.Diamond,
                            gradientColors = listOf(RoyalGold, Color(0xFFFFA000), Color(0xFFFFFF00)),
                            onClick = onNavigateToPremium
                        ),
                        CarouselItem(
                            type = CarouselType.SUBSCRIPTION,
                            title = "ROYAL VPN",
                            headline = "Secure Tunnel",
                            description = "Encrypt connection",
                            color = Color(0xFFAA00FF),
                            icon = androidx.compose.material.icons.Icons.Default.VpnLock,
                            gradientColors = listOf(Color(0xFFAA00FF), Color(0xFFEA80FC), Color(0xFFD500F9)),
                            onClick = onNavigateToVpn
                        )
                    )
                }

                var currentStatusIndex by remember { mutableIntStateOf(0) }
                var currentPromoIndex by remember { mutableIntStateOf(0) }

                LaunchedEffect(Unit) {
                    while(true) {
                        delay(3500) // Staggered delays for a dynamic, reactive flow
                        currentStatusIndex = (currentStatusIndex + 1) % statusItems.size
                    }
                }
                LaunchedEffect(Unit) {
                    while(true) {
                        delay(4800)
                        currentPromoIndex = (currentPromoIndex + 1) % promoItems.size
                    }
                }

                if (showLegacyDashboardStatusStrip) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    // LEFT CAROUSEL: STATUS (Height: 90.dp)
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = statusItems[currentStatusIndex],
                            transitionSpec = {
                                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                            },
                            label = "StatusCarousel"
                        ) { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { item.onClick() }
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF161616).copy(alpha = 0.95f),
                                                Color(0xFF0C0C0C).copy(alpha = 0.9f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.sweepGradient(item.gradientColors),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(item.color.copy(alpha = 0.12f), CircleShape)
                                            .border(1.dp, item.color.copy(alpha = 0.25f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = item.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = item.title,
                                            color = item.color,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.headline,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.description,
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // RIGHT CAROUSEL: PROMOTIONS & MARKETING (Height: 90.dp)
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = promoItems[currentPromoIndex],
                            transitionSpec = {
                                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                            },
                            label = "PromoCarousel"
                        ) { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { item.onClick() }
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF161616).copy(alpha = 0.95f),
                                                Color(0xFF0C0C0C).copy(alpha = 0.9f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.sweepGradient(item.gradientColors),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(item.color.copy(alpha = 0.12f), CircleShape)
                                            .border(1.dp, item.color.copy(alpha = 0.25f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = item.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = item.title,
                                            color = item.color,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.headline,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.description,
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }



                var isArmed by remember { mutableStateOf(PreferencesManager.isSystemArmed()) }
                DisposableEffect(Unit) {
                    val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        if (key == PreferencesManager.KEY_SYSTEM_ARMED) {
                            isArmed = PreferencesManager.isSystemArmed()
                        }
                    }
                    PreferencesManager.registerListener(listener)
                    onDispose {
                        PreferencesManager.unregisterListener(listener)
                    }
                }
                var showArmedDialog by remember { mutableStateOf(false) }

                if (showArmedDialog) {
                    val statusColor = if (isArmed) Color(0xFF00FF94) else Color(0xFFFF3B30)
                    val statusItems = listOf(
                        "Real-time Threat Detection",
                        "Network Traffic Analysis",
                        "File System Watcher",
                        "Malware Scanner",
                        "Privacy Guard",
                        "VPN Shield"
                    )
                    AlertDialog(
                        onDismissRequest = { showArmedDialog = false },
                        title = {
                            Text(
                                if (isArmed) "System Armed" else "System Disarmed",
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isArmed)
                                        "Your device is protected by Royal Shield active monitoring:"
                                    else
                                        "Active monitoring is disabled. Enable it to secure your device:",
                                    color = if (isLight) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                statusItems.forEach { item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    ) {
                                        Canvas(modifier = Modifier.size(8.dp)) {
                                            if (isArmed) {
                                                drawCircle(color = Color(0xFF00FF94))
                                            } else {
                                                drawCircle(
                                                    color = Color(0xFFFF3B30),
                                                    style = Stroke(width = 1.5.dp.toPx())
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isArmed) item else "$item (Inactive)",
                                            color = if (isLight) Color.Black.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val targetState = !isArmed
                                PreferencesManager.setSystemArmed(targetState)
                                isArmed = targetState
                                showArmedDialog = false
                            }) {
                                Text(
                                    if (isArmed) "DISARM" else "ARM",
                                    color = if (isArmed) Color(0xFFFF3B30) else Color(0xFF00FF94),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showArmedDialog = false }) {
                                Text("CLOSE", color = RoyalGold)
                            }
                        },
                        containerColor = if (isLight) Color.White else Color(0xFF1E1E1E),
                        textContentColor = if (isLight) Color.Black else Color.White
                    )
                }

                // ARMED STATUS (Lock) - Restructured for perfect centering and SYSTEM SECURITY header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(230.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .clickable { showArmedDialog = true }
                        .background(Color(0xFF0F0F13).copy(alpha = 0.4f))
                        .border(1.dp, RoyalGold.copy(alpha = 0.15f), RoundedCornerShape(30.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val rotatingColor = if (isArmed) Color(0xFF00FF94) else Color.Red

                    val rotationTransition = rememberInfiniteTransition(label = "lock_rotation")
                    val angle by rotationTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(3000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ), label = "angle"
                    )

                    // Title
                    Text(
                        text = "SYSTEM SECURITY",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Premium multi-ring pulsating circle + padlock
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background Hex Grid / Shield Status (Visual Effect)
                        ShieldStatusBackground(modifier = Modifier.fillMaxSize().alpha(0.3f))

                        // Pulsating ring animations (3 concentric rings at different speeds)
                        val ringTransition = rememberInfiniteTransition(label = "ring_pulse")
                        val ring1Alpha by ringTransition.animateFloat(
                            initialValue = 0.15f, targetValue = 0.45f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = "ring1"
                        )
                        val ring2Alpha by ringTransition.animateFloat(
                            initialValue = 0.35f, targetValue = 0.12f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2800, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = "ring2"
                        )
                        val ring3Alpha by ringTransition.animateFloat(
                            initialValue = 0.08f, targetValue = 0.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(3500, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = "ring3"
                        )
                        val glowPulse by ringTransition.animateFloat(
                            initialValue = 0.05f, targetValue = 0.25f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2400, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = "glow"
                        )

                        // Radial glow pulse (outermost wave effect)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        rotatingColor.copy(alpha = glowPulse),
                                        rotatingColor.copy(alpha = glowPulse * 0.4f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.minDimension / 2f
                                )
                            )
                        }

                        // Outer ring (largest, slowest pulse)
                        Canvas(modifier = Modifier.size(130.dp)) {
                            drawCircle(
                                color = rotatingColor.copy(alpha = ring3Alpha),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }

                        // Middle ring
                        Canvas(modifier = Modifier.size(112.dp)) {
                            drawCircle(
                                color = rotatingColor.copy(alpha = ring2Alpha),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Inner ring (smallest, fastest pulse)
                        Canvas(modifier = Modifier.size(96.dp)) {
                            drawCircle(
                                color = rotatingColor.copy(alpha = ring1Alpha),
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }

                        // Gradient rotating arc (enhanced)
                        Canvas(modifier = Modifier.size(120.dp)) {
                            rotate(angle) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            rotatingColor.copy(alpha = 0.3f),
                                            rotatingColor,
                                            rotatingColor.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    ),
                                    startAngle = 0f,
                                    sweepAngle = 280f,
                                    useCenter = false,
                                    style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                            }
                        }

                        // Inner glow fill circle
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            rotatingColor.copy(alpha = 0.12f),
                                            rotatingColor.copy(alpha = 0.04f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                                .border(1.dp, rotatingColor.copy(alpha = ring1Alpha * 0.6f), CircleShape)
                        )

                        // Padlock Image - Perfectly centered
                        Image(
                            painter = painterResource(id = RoyalIcons.LockGoldLarge),
                            contentDescription = "ArmedStatus",
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ARMED Text with Gold Lux Frame
                    com.royalshield.app.ui.components.GoldLuxFrame {
                        com.royalshield.app.ui.components.GoldLuxText(
                            text = if(isArmed) "SYSTEM ARMED" else "SYSTEM DISARMED",
                            fontSize = 13,
                            letterSpacing = 1.5f
                        )
                    }
                }

                // Quick Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 1. HOME
                    QuickActionButton(
                        iconRes = 0,
                        label = "Home",
                        vectorIcon = Icons.Default.Home,
                        onClick = onNavigateToSystemStatus
                    )

                    // 2. MAP
                    QuickActionButton(
                        iconRes = 0,
                        label = "Map",
                        vectorIcon = Icons.Default.LocationOn,
                        onClick = onNavigateToMap
                    )

                    // 3. SECURITY
                    QuickActionButton(
                        iconRes = 0,
                        label = "Security",
                        vectorIcon = Icons.Default.Fingerprint, // Escudo con huella
                        onClick = onNavigateToFileScan
                    )

                    // 4. AUTOMATION
                    QuickActionButton(
                        iconRes = 0,
                        label = "Automation",
                        vectorIcon = Icons.Default.SettingsSuggest,
                        onClick = onNavigateToAutomation
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // EMERGENCY SOS BUTTON â€” Compact Transparent Red Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Emergency Alert Button (compact pill)
                    Button(
                        onClick = onNavigateToSOS,
                        modifier = Modifier
                            .width(280.dp)
                            .height(52.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            },
                        shape = RoundedCornerShape(26.dp), // pill shape
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x66FF0000) // transparent red
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp, // Increased thickness for visibility
                            Color.Red.copy(alpha = pulseAlpha)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Emergency Alert",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Floating Mic Button â€” separated, aligned to end with offset
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 8.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700)) // Gold
                            .clickable { showVoiceAssistant = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Assistant",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (showVoiceAssistant) {
                    com.royalshield.app.ui.components.DidAgentDialog(
                        onDismiss = { showVoiceAssistant = false },
                        onVoiceCommand = { command ->
                            showVoiceAssistant = false
                            // Normalize text to lowercase and remove accents/diacritics
                            val tempNormalized = Normalizer.normalize(command, Normalizer.Form.NFD)
                            val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                            val normalized = pattern.matcher(tempNormalized).replaceAll("").lowercase().trim()

                            val fileScanKeywords = listOf(
                                "scan", "escanear", "escaner", "analizar", "analisis", "buscar virus",
                                "chequear", "comprobar", "iniciar analisis", "escanear archivos",
                                "escanear dispositivo", "scan device", "file scan", "malware scan",
                                "run scan", "check device", "check for malware", "start scan", "scan files"
                            )
                            val settingsKeywords = listOf(
                                "settings", "go to settings", "show settings", "open settings", "preferences",
                                "configure", "configuration", "ajustes", "configuracion", "abrir configuracion",
                                "ir a ajustes", "preferencias", "opciones"
                            )
                            val vpnKeywords = listOf(
                                "vpn", "connect vpn", "start vpn", "enable vpn", "virtual private network",
                                "open vpn", "protect network", "conectar vpn", "abrir vpn", "iniciar vpn",
                                "activar vpn", "red privada virtual"
                            )
                            val sosKeywords = listOf(
                                "sos", "emergency", "alert", "panic", "help", "send alert", "send emergency",
                                "panic button", "emergencia", "alerta", "panico", "auxilio", "ayuda",
                                "boton de panico", "enviar alerta"
                            )
                            val mapKeywords = listOf(
                                "map", "show map", "global map", "threat map", "view map", "live map",
                                "mapa", "ver mapa", "mostrar mapa", "mapa global", "mapa de amenazas"
                            )
                            val automationKeywords = listOf(
                                "automation", "smart home", "home automation", "automations", "iot",
                                "automatizacion", "casa inteligente", "automatizaciones", "domotica"
                            )
                            val aiHubKeywords = listOf(
                                "ai assistant", "ai hub", "neural hub", "chat with ai", "assistant",
                                "ai chatbot", "artificial intelligence", "asistente", "ia", "asistente de ia",
                                "chat con ia", "inteligencia artificial", "hub de ia"
                            )
                            val solutionEngineKeywords = listOf(
                                "solution engine", "resolution engine", "resolucion", "solucion",
                                "motor de solucion", "motor de resolucion", "resolver", "fix issues"
                            )
                            val securityCameraKeywords = listOf(
                                "security cam", "camera", "security camera", "cam", "video surveillance",
                                "surveillance", "camara", "camara de seguridad", "vigilancia", "camara de vigilancia"
                            )
                            val soundDetectionKeywords = listOf(
                                "audio guard", "sound detection", "listen", "mic guard", "microphone",
                                "sound guard", "proteccion de audio", "deteccion de sonido", "microfono"
                            )
                            val trackingShieldKeywords = listOf(
                                "tracking shield", "track shield", "location tracking", "anti tracking",
                                "prevent tracking", "proteccion de rastreo", "escudo de rastreo", "evitar rastreo",
                                "antirastreo", "antirrastreo"
                            )
                            val galleryKeywords = listOf(
                                "gallery", "vault gallery", "secure gallery", "photos", "videos",
                                "galeria", "galeria segura", "fotos"
                            )
                            val coursesKeywords = listOf(
                                "academy", "courses", "learn", "security tips", "training",
                                "academia", "cursos", "aprender", "consejos de seguridad", "entrenamiento"
                            )

                            if (fileScanKeywords.any { normalized.contains(it) }) {
                                onNavigateToFileScan()
                            } else if (vpnKeywords.any { normalized.contains(it) }) {
                                onNavigateToVpn()
                            } else if (sosKeywords.any { normalized.contains(it) }) {
                                onNavigateToSOS()
                            } else if (mapKeywords.any { normalized.contains(it) }) {
                                onNavigateToMap()
                            } else if (settingsKeywords.any { normalized.contains(it) }) {
                                onNavigateToSettings()
                            } else if (automationKeywords.any { normalized.contains(it) }) {
                                onNavigateToAutomation()
                            } else if (aiHubKeywords.any { normalized.contains(it) }) {
                                onNavigateToAiHub()
                            } else if (solutionEngineKeywords.any { normalized.contains(it) }) {
                                onNavigateToSolutionEngine()
                            } else if (securityCameraKeywords.any { normalized.contains(it) }) {
                                onNavigateToSecurityCamera()
                            } else if (soundDetectionKeywords.any { normalized.contains(it) }) {
                                onNavigateToSoundDetection()
                            } else if (trackingShieldKeywords.any { normalized.contains(it) }) {
                                onNavigateToTrackingShield()
                            } else if (galleryKeywords.any { normalized.contains(it) }) {
                                onNavigateToGallery()
                            } else if (coursesKeywords.any { normalized.contains(it) }) {
                                onNavigateToCourses()
                            }
                        }
                    )
                }

                // Spacer(modifier = Modifier.height(24.dp))
                // Elite Card Removed

                Spacer(modifier = Modifier.height(32.dp))

                // FEATURE GRID - 2x2 Layout matching design reference
                Text(
                    text = "SECURITY HUB",
                    color = RoyalGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // DASHBOARD GRID - Custom 2-1-2 Layout (Based on Image Reference)
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ROW 1: Learn & Secure Vault
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FeatureCard(
                            title = "Learn",
                            icon = Icons.Filled.School,
                            imageRes = com.royalshield.app.R.drawable.screen_dasboard_education_tecnology,
                            neonColor = com.royalshield.app.ui.theme.NeonOrange,
                            onClick = onNavigateToCourses,
                            modifier = Modifier.weight(1f).height(125.dp),
                            borderAnimation = true
                        )
                        FeatureCard(
                            title = "Secure Vault",
                            icon = Icons.Filled.FolderOpen,
                            imageRes = RoyalIcons.FileScan,
                            neonColor = com.royalshield.app.ui.theme.NeonOrange,
                            onClick = onNavigateToFileScan,
                            modifier = Modifier.weight(1f).height(125.dp),
                            borderAnimation = true
                        )
                    }

                    // ROW 2: AI Neural Hub & Smart Home
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeatureCard(
                            title = "AI Neural Hub",
                            icon = Icons.Filled.SmartToy,
                            imageRes = RoyalIcons.AiNeuralHub,
                            neonColor = com.royalshield.app.ui.theme.NeonPink,
                            onClick = onNavigateToAiHub,
                            modifier = Modifier.weight(1f).height(125.dp),
                            borderAnimation = true
                        )
                        FeatureCard(
                            title = "Smart Home",
                            icon = Icons.Filled.SettingsSuggest,
                            imageRes = RoyalIcons.SmartHome,
                            neonColor = com.royalshield.app.ui.theme.NeonAqua,
                            onClick = onNavigateToAutomation,
                            modifier = Modifier.weight(1f).height(125.dp),
                            borderAnimation = true
                        )
                    }

                    // ROW 3: SMS Phishing & Royal VPN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeatureCard(
                            title = "SMS Phishing",
                            icon = Icons.Filled.Message,
                            imageRes = RoyalIcons.SmsPhishing,
                            neonColor = Color.Red,
                            onClick = onNavigateToPhishing,
                            modifier = Modifier.weight(1f).height(125.dp),
                            borderAnimation = true
                        )
                        FeatureCard(
                            title = "Royal VPN",
                            icon = Icons.Filled.VpnLock,
                            imageRes = RoyalIcons.RoyalVpn,
                            neonColor = com.royalshield.app.ui.theme.NeonGreen,
                            onClick = onNavigateToVpn,
                            modifier = Modifier.weight(1f).height(125.dp),
                            borderAnimation = true
                        )
                    }

                    // ROW 4: Malware Scanner & Surveillance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MalwareScannerCard(
                            title = "Scan Malware",
                            onClick = {
                                context.startActivity(
                                    Intent(context, com.royalshield.app.ScanResultsActivity::class.java)
                                )
                            },
                            modifier = Modifier.weight(1f).height(125.dp)
                        )
                        FeatureCard(
                            title = "Surveillance",
                            icon = Icons.Filled.Videocam,
                            imageRes = com.royalshield.app.R.drawable.dashboard_card_surveillance,
                            neonColor = com.royalshield.app.ui.theme.NeonOrange,
                            onClick = onNavigateToSecurityCamera,
                            modifier = Modifier.weight(1f).height(125.dp),
                            borderAnimation = true
                        )
                    }

                    // ROW 5: Tracking Shield
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeatureCard(
                            title = "Tracking Shield",
                            icon = Icons.Filled.LocationOn,
                            imageRes = RoyalIcons.TrackingChild,
                            neonColor = com.royalshield.app.ui.theme.NeonGreen,
                            onClick = onNavigateToTrackingShield,
                            modifier = Modifier.weight(1f).height(125.dp),
                            borderAnimation = true
                        )
                        Box(modifier = Modifier.weight(1f))
                    }

                    // ROW 6: Integrated Surveillance Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0xFFD4AF37).copy(alpha=0.5f), RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F13).copy(alpha = 0.8f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "MONITORING & SURVEILLANCE",
                                color = Color.White.copy(alpha=0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CyberButtonRect(
                                    text = "Audio Guard",
                                    icon = Icons.Filled.Mic,
                                    color = com.royalshield.app.ui.theme.NeonOrange,
                                    onClick = onNavigateToSoundDetection,
                                    modifier = Modifier.weight(1f)
                                )
                                CyberButtonRect(
                                    text = "Security Cam",
                                    icon = Icons.Filled.Videocam,
                                    color = Color(0xFFD4AF37),
                                    onClick = onNavigateToSecurityCamera,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                CommandLogStatusPanel(
                    items = dashboardState.actionItems,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun CommandLogStatusPanel(
    items: List<ActionItem>,
    modifier: Modifier = Modifier
) {
    val openItems = items.filterNot { it.status.equals("Resolved", ignoreCase = true) }
    val resolvedItems = items.size - openItems.size

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "COMMAND LOG",
                color = RoyalGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CommandArrowBadge(
                    iconRes = R.drawable.arrow_up,
                    tint = Color(0xFF00FF94),
                    label = resolvedItems.coerceAtLeast(0).toString()
                )
                CommandArrowBadge(
                    iconRes = R.drawable.arrow_down,
                    tint = Color(0xFFFF3B30),
                    label = openItems.size.toString()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.68f))
                .border(1.dp, RoyalGold.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (items.isEmpty()) {
                CommandLogRow(
                    title = "Royal Shield active",
                    description = "No critical command events pending",
                    severity = Severity.LOW,
                    status = "Resolved"
                )
            } else {
                items.take(4).forEach { item ->
                    CommandLogRow(
                        title = item.title,
                        description = item.description,
                        severity = item.severity,
                        status = item.status
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandArrowBadge(
    iconRes: Int,
    tint: Color,
    label: String
) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CommandLogRow(
    title: String,
    description: String,
    severity: Severity,
    status: String
) {
    val isResolved = status.equals("Resolved", ignoreCase = true)
    val accent = if (isResolved) Color(0xFF00FF94) else when (severity) {
        Severity.LOW -> Color(0xFF00FF94)
        Severity.MEDIUM -> RoyalGold
        Severity.HIGH, Severity.CRITICAL -> Color(0xFFFF3B30)
    }
    val arrowRes = if (isResolved || severity == Severity.LOW) R.drawable.arrow_up else R.drawable.arrow_down

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = arrowRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(accent),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 11.sp,
                maxLines = 2
            )
        }
        Image(
            painter = painterResource(id = if (isResolved) R.drawable.ic_action_checkmark_green else R.drawable.ic_action_checkmark_gold),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * A Feature Card that plays a looping video background.
 */
@Composable
fun VideoFeatureCard(
    title: String,
    videoResId: Int,
    neonColor: Color = RoyalGold,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brush = Brush.sweepGradient(
        colors = listOf(
            neonColor.copy(alpha = 0.1f),
            neonColor,
            neonColor.copy(alpha = 0.1f),
        )
    )

    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, brush)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Video Background
            com.royalshield.app.ui.components.VideoBackground(
                videoResId = videoResId,
                modifier = Modifier.fillMaxSize()
            )

            // Overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )

            // Title Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .background(Color.Red, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("LIVE FEED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title.uppercase(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    imageRes: Int = 0,
    neonColor: Color = RoyalGold,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderAnimation: Boolean = false
) {
    // DEFERRED image loading â€” images only load AFTER first frame renders
    // This prevents HWUI from decoding all PNGs at once (black screen fix)
    var imageVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(400L) // Let first frame render, then load image
        imageVisible = true
    }

    // Sliding Gradient Animation
    val infiniteTransition = rememberInfiniteTransition(label = "neon_slide")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "offset"
    )

    val brush = if (borderAnimation) {
        Brush.linearGradient(
            0.0f to neonColor.copy(alpha = 0.1f),
            0.3f to neonColor,
            0.5f to Color.White,
            0.7f to neonColor,
            1.0f to neonColor.copy(alpha = 0.1f),
            start = androidx.compose.ui.geometry.Offset(offset * 2000f - 1000f, 0f),
            end = androidx.compose.ui.geometry.Offset(offset * 2000f, 1000f),
            tileMode = androidx.compose.ui.graphics.TileMode.Mirror
        )
    } else {
        Brush.linearGradient(
            colors = listOf(neonColor, neonColor.copy(alpha = 0.3f))
        )
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .drawBehind {
                if (borderAnimation) {
                    drawRoundRect(
                        brush = brush,
                        alpha = 0.5f,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                        style = Stroke(width = 6.dp.toPx())
                    )
                    drawRoundRect(
                        brush = brush,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
        border = if (!borderAnimation) androidx.compose.foundation.BorderStroke(2.dp, brush) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!imageVisible) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = neonColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Image fades in after initial frame renders
            if (imageRes != 0) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = imageVisible,
                    enter = androidx.compose.animation.fadeIn(tween(400)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                    )
                }
            } else {
                // No image: show full neon-gradient background + centered icon
                androidx.compose.animation.AnimatedVisibility(
                    visible = imageVisible,
                    enter = androidx.compose.animation.fadeIn(tween(400)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = neonColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Edge gradient overlay for depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.05f),
                            0.7f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.6f)
                        )
                    )
            )

            // Title Overlay (Fixed: Now showing the title)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = title.uppercase(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}


@Composable
fun QuickActionButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    vectorIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        // Use vector icon immediately (no PNG decode overhead)
        // Falls back to CyberButtonRound with PNG only if no vector provided
        if (vectorIcon != null) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF1A2A3A), Color(0xFF0A1220))
                        ),
                        CircleShape
                    )
                    .border(1.5.dp, RoyalGold.copy(alpha = 0.7f), CircleShape)
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = vectorIcon,
                    contentDescription = label,
                    tint = RoyalGold,
                    modifier = Modifier.size(30.dp)
                )
            }
        } else {
            CyberButtonRound(
                size = 72.dp,
                icon = iconRes,
                contentDescription = label,
                onClick = onClick
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
@Composable
fun BadgeItem(
    resId: Int,
    label: String,
    isActive: Boolean = false,
    isElite: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(if (isActive) 1f else 0.4f)
    ) {
        Box(
            modifier = Modifier.size(if (isElite) 56.dp else 48.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow for Elite
            if (isElite) {
               Canvas(modifier = Modifier.fillMaxSize()) {
                   drawCircle(
                       brush = Brush.radialGradient(
                           colors = listOf(RoyalGold.copy(alpha=0.6f), Color.Transparent)
                       )
                   )
               }
            }

            Image(
                painter = painterResource(id = resId),
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label.uppercase(),
            color = if (isElite) RoyalGold else Color.Gray,
            fontSize = 10.sp,
            fontWeight = if (isElite) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun MalwareScannerCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_sweep")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_progress"
    )

    val silverColor = Color.White

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .drawBehind {
                drawRect(Color(0xFF0D0D12)) // Dark cyber background
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, silverColor.copy(alpha = 0.7f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Matrix Code Layer
            val matrixText = """
                0x0045: SCAN_INIT = TRUE;
                0x0046: BYPASS_RING0 = OK;
                0x0047: NEURAL_HEURISTICS_ON;
                0x0048: MALWARE_SIGS_LOADED;
                0x0049: ANOMALY = FALSE;
                [ SYSTEM STEALTH MODE ]
            """.trimIndent()

            Text(
                text = matrixText,
                color = Color(0xFF00FF00).copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                lineHeight = 14.sp,
                modifier = Modifier.padding(12.dp)
            )

            // The Scanner Line Overlay
            Box(
                 modifier = Modifier.fillMaxSize().drawBehind {
                     val scannerX = size.width * sweepProgress
                     if (scannerX in 0f..size.width) {
                         drawLine(
                             color = Color.White,
                             start = Offset(scannerX, 0f),
                             end = Offset(scannerX, size.height),
                             strokeWidth = 3.dp.toPx()
                         )
                         val startX = maxOf(0f, scannerX - 120.dp.toPx())
                         val endX = maxOf(0f, minOf(scannerX, size.width))
                         val rectWidth = endX - startX
                         if (rectWidth > 0f) {
                             drawRect(
                                 color = silverColor.copy(alpha = 0.3f),
                                 topLeft = Offset(startX, 0f),
                                 size = Size(rectWidth, size.height)
                             )
                         }
                     }
                 }
            )

            // Icon & Title over everything
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                 Icon(
                     imageVector = Icons.Default.Warning,
                     contentDescription = null,
                     tint = silverColor,
                     modifier = Modifier.size(24.dp)
                 )
                 Spacer(modifier = Modifier.height(8.dp))
                 Text(
                     text = title.uppercase(),
                     color = Color.White,
                     fontSize = 14.sp,
                     fontWeight = FontWeight.Bold,
                 )
            }
        }
    }
}




