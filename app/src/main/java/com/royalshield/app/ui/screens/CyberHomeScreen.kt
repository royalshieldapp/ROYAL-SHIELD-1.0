package com.royalshield.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.CyberThreatMapActivity
import com.royalshield.app.SecurityActivity
import com.royalshield.app.VulnerabilityManager
import com.royalshield.app.Vulnerability
import com.royalshield.app.managers.PreferencesManager
import com.royalshield.app.ui.components.AnimatedNeonCard
import com.royalshield.app.ui.components.*
import com.royalshield.app.ui.components.CyberButtonRound
import com.royalshield.app.ui.components.CyberButtonRect
import com.royalshield.app.ui.theme.*
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.features.threatmap.LiveCyberThreatMap
import com.royalshield.app.features.threatmap.ThreatMapViewModel

@Composable
fun CyberHomeScreen(
    onBack: () -> Unit = {},
    onNavigateToMap: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToVoiceScam: () -> Unit = {},
    onNavigateToXdr: () -> Unit = {},
    onNavigateToIntel: () -> Unit = {},
    onNavigateToNetworkToolkit: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val threatMapViewModel: ThreatMapViewModel = viewModel()
    val threatMapState by threatMapViewModel.uiState.collectAsState()

    // Vulnerability Analysis State
    val vulnerabilities by VulnerabilityManager.vulnerabilities.collectAsState()
    var showVulnerabilityAlert by remember { mutableStateOf(false) }
    var showUpdatesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        VulnerabilityManager.scan(context)
    }

    /*
    LaunchedEffect(vulnerabilities) {
        if (vulnerabilities.isNotEmpty()) {
            showVulnerabilityAlert = true
        }
    }
    */

    Box(modifier = Modifier.fillMaxSize()) {
        // Cyber Globe Background
        Image(
            painter = painterResource(id = com.royalshield.app.R.drawable.bg_cyber_earth),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // HolographicWaveBackground() // Disabled in favor of static image as requested

        // Additional Dark Overlay for better text readability on top of animation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 100.dp)
            ) {
                // Top Tagline
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "⚡ Next-Level Cybersecurity at Your Fingertips",
                        color = RoyalGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "🔒 Secure Your Digital Life, Effortlessly",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Threat Briefing",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Threat Briefing Icon
                    Image(
                        painter = painterResource(id = com.royalshield.app.R.drawable.ic_threat_briefing_update),
                        contentDescription = "Threat Briefing",
                        modifier = Modifier
                            .size(64.dp)
                            .clickable { showUpdatesDialog = true }
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }

                // Armed Status Section
                var isSystemArmed by remember { mutableStateOf(PreferencesManager.isSystemArmed()) }
                DisposableEffect(Unit) {
                    val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        if (key == PreferencesManager.KEY_SYSTEM_ARMED) {
                            isSystemArmed = PreferencesManager.isSystemArmed()
                        }
                    }
                    PreferencesManager.registerListener(listener)
                    onDispose {
                        PreferencesManager.unregisterListener(listener)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                     ArmedStatusIndicator(
                         isArmed = isSystemArmed,
                         onToggle = {
                             val targetState = !isSystemArmed
                             PreferencesManager.setSystemArmed(targetState)
                             isSystemArmed = targetState
                         }
                     )
                }

                // Quick Actions (4 Circular Buttons as per design)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CyberActionItem(
                        icon = Icons.Default.Link,
                        label = "File Scan",
                        modifier = Modifier.weight(1f)
                    ) {
                        context.startActivity(Intent(context, SecurityActivity::class.java))
                    }

                    CyberActionItem(
                        icon = Icons.Default.Call,
                        label = "Check Num",
                        modifier = Modifier.weight(1f)
                    ) {
                        // Launch Real Number Check
                        context.startActivity(Intent(context, com.royalshield.app.PhoneNumberCheckActivity::class.java))
                    }

                    CyberActionItem(
                        icon = Icons.Default.School,
                        label = "Learn",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCourses
                    )

                    CyberActionItem(
                        icon = Icons.Default.Call,
                        label = "AI Voice",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToVoiceScam
                    )

                    CyberActionItem(
                        icon = Icons.Default.Security,
                        label = "XDR",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToXdr
                    )

                    CyberActionItem(
                        icon = Icons.Default.Public,
                        label = "Intel",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToIntel
                    )

                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF171D27))
                        .border(1.dp, RoyalGold.copy(alpha = 0.42f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onNavigateToNetworkToolkit)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("NETWORK TOOLKIT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("Connectivity and security diagnostics", color = Color.White.copy(alpha = .62f), fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RoyalGold)
                }


                Spacer(modifier = Modifier.height(32.dp))

                // Metrics Section
                Text(
                    "REAL-TIME METRICS",
                    color = RoyalGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween // Space them evenly
                ) {
                    CyberMetricCard("Events", threatMapState.events.size.toString(), NeonRed)
                    CyberMetricCard("Target", threatMapState.primaryTarget, CyberCyan)
                    CyberMetricCard("Alerts", threatMapState.alertCount.toString(), RoyalGold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                LiveCyberThreatMap(
                    state = threatMapState,
                    onRefresh = threatMapViewModel::refresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                CybercrimeCostChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Privacy Advisor Card
                Text(
                    "Privacy Advisor",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Privacy Advisor Card
                AnimatedNeonCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(horizontal = 20.dp)
                        .clickable {
                            val intent = Intent(context, com.royalshield.app.PrivacyAdvisorActivity::class.java)
                            context.startActivity(intent)
                        },
                    neonColors = listOf(CyberCyan, Color.Blue, CyberCyan),
                    containerColor = Color.Black
                ) {
                        // Privacy Advisor background image
                        Image(
                            painter = painterResource(id = com.royalshield.app.R.drawable.privacy_advisor_bg),
                            contentDescription = "Privacy Advisor",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )

                        // Dark Gradient Overlay
                        Box(
                             modifier = Modifier
                                 .fillMaxSize()
                                 .background(
                                     Brush.verticalGradient(
                                         colors = listOf(
                                             Color.Transparent,
                                             Color.Black.copy(alpha = 0.8f)
                                         )
                                     )
                                 )
                        )

                        // Content
                        Box(modifier = Modifier.fillMaxSize()) {
                             Column(
                                 modifier = Modifier
                                     .align(Alignment.Center)
                                     .padding(bottom = 48.dp),
                                 horizontalAlignment = Alignment.CenterHorizontally,
                                 verticalArrangement = Arrangement.Center
                             ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = CyberCyan.copy(alpha = 0.9f),
                                    modifier = Modifier.size(48.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    "PRIVACY ADVISOR",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    letterSpacing = 2.sp
                                )

                                Text(
                                    "App Permission Analysis",
                                    color = CyberCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Gold Cyber Button
                            CyberButtonRect(
                                text = "START SCAN",
                                color = RoyalGold,
                                onClick = {
                                     val intent = Intent(context, com.royalshield.app.PrivacyAdvisorActivity::class.java)
                                     context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 18.dp)
                                    .width(200.dp)
                            )
                        }
                    }
                }


            } // End Column

            // DIALOGS
            if (showVulnerabilityAlert) {
                VulnerabilityAlertDialog(
                    vulnerabilities = vulnerabilities,
                    onDismiss = { showVulnerabilityAlert = false }
                )
            }
            if (showUpdatesDialog) {
                UpdatesDialog { showUpdatesDialog = false }
            }

        } // End Box (Main)
    }

@Composable
private fun CybercrimeCostChart(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "cybercrime-chart")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chart-line-progress"
    )
    val glowOffset by transition.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chart-background-glow"
    )
    val values = remember { listOf(6f, 9f, 8.4f, 11.2f, 12.6f, 13.9f) }
    val years = remember { listOf("2021", "2022", "2023", "2024", "2025", "2026") }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF08080C))
            .border(1.dp, Color(0xFFFF5050).copy(alpha = 0.38f), RoundedCornerShape(24.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x66FF3030), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * glowOffset, size.height * 0.45f),
                    radius = size.width * 0.72f
                )
            )

            val left = 42.dp.toPx()
            val right = size.width - 14.dp.toPx()
            val top = 62.dp.toPx()
            val bottom = size.height - 34.dp.toPx()
            val chartWidth = right - left
            val chartHeight = bottom - top
            val minValue = 5f
            val maxValue = 15f
            fun point(index: Int): androidx.compose.ui.geometry.Offset {
                val x = left + chartWidth * index / values.lastIndex
                val y = bottom - ((values[index] - minValue) / (maxValue - minValue)) * chartHeight
                return androidx.compose.ui.geometry.Offset(x, y)
            }

            listOf(5f, 10f, 15f).forEach { tick ->
                val y = bottom - ((tick - minValue) / (maxValue - minValue)) * chartHeight
                drawLine(Color.White.copy(alpha = 0.10f), androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(right, y), 1.dp.toPx())
            }
            years.indices.forEach { index ->
                val x = point(index).x
                drawLine(Color.White.copy(alpha = 0.08f), androidx.compose.ui.geometry.Offset(x, top), androidx.compose.ui.geometry.Offset(x, bottom), 1.dp.toPx())
            }

            val completedSegment = progress.toInt().coerceIn(0, values.lastIndex)
            val segmentProgress = progress - completedSegment
            val path = androidx.compose.ui.graphics.Path().apply {
                val first = point(0)
                moveTo(first.x, first.y)
                for (index in 1..completedSegment) {
                    val next = point(index)
                    lineTo(next.x, next.y)
                }
                if (completedSegment < values.lastIndex) {
                    val from = point(completedSegment)
                    val to = point(completedSegment + 1)
                    lineTo(
                        from.x + (to.x - from.x) * segmentProgress,
                        from.y + (to.y - from.y) * segmentProgress
                    )
                }
            }
            drawPath(path, Color(0xFFFF3C3C), style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))

            values.indices.forEach { index ->
                val p = point(index)
                val active = index == (completedSegment + 1).coerceAtMost(values.lastIndex)
                drawCircle(Color(0xFFFF3C3C).copy(alpha = if (active) 0.30f else 0.12f), radius = if (active) 10.dp.toPx() else 7.dp.toPx(), center = p)
                drawCircle(Color.White, radius = if (active) 4.5.dp.toPx() else 3.5.dp.toPx(), center = p)
                drawCircle(Color(0xFFFF3C3C), radius = if (active) 4.5.dp.toPx() else 3.5.dp.toPx(), center = p, style = Stroke(1.5.dp.toPx()))
            }

            drawContext.canvas.nativeCanvas.apply {
                val titlePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(255, 90, 90)
                    textSize = 14.sp.toPx()
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText("GLOBAL CYBERCRIME COST EVOLUTION", size.width / 2f, 25.dp.toPx(), titlePaint)
                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(165, 255, 255, 255)
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText("USD TRILLIONS • PROVIDED DATASET • 2021–2026", size.width / 2f, 43.dp.toPx(), labelPaint)
                years.forEachIndexed { index, year -> drawText(year, point(index).x, size.height - 12.dp.toPx(), labelPaint) }
                val yPaint = android.graphics.Paint(labelPaint).apply { textAlign = android.graphics.Paint.Align.RIGHT }
                listOf(5, 10, 15).forEach { tick ->
                    val y = bottom - ((tick - minValue) / (maxValue - minValue)) * chartHeight
                    drawText("$${tick}T", left - 6.dp.toPx(), y + 3.dp.toPx(), yPaint)
                }
            }
        }
    }
}


@Composable
fun VulnerabilityAlertDialog(
    vulnerabilities: List<Vulnerability>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = NeonRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Security Risk Detected", color = NeonRed, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "Royal Shield has detected potential vulnerabilities on your device:",
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                vulnerabilities.forEach { v ->
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                         Text("• ${v.title}", fontWeight = FontWeight.Bold, color = RoyalGold)
                         Text(v.description, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        },
        containerColor = Color(0xFF1E1E1E),
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ACKNOWLEDGE", color = NeonRed, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun UpdatesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = RoyalGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Threat Briefing", color = RoyalGold, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text("🔴 Critical: New ransomware targeting Android devices", color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("⚠️ High: Phishing campaign via SMS delivery scams", color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text("🟡 Medium: Public WiFi vulnerabilities detected", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Stay vigilant and keep your security up to date.", color = Color.Gray, fontSize = 12.sp)
            }
        },
        containerColor = Color(0xFF1E1E1E),
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("ACKNOWLEDGE", color = RoyalGold, fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
fun CyberActionItem(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        // Cyber Round Button
        CyberButtonRound(
            size = 72.dp,
            icon = if (label == "Learn") com.royalshield.app.R.drawable.graduation_cap_icon else icon,
            contentDescription = label,
            onClick = onClick
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

data class AlertItem(val title: String, val subtitle: String, val icon: ImageVector, val color: Color)

@Composable
fun AlertRow(item: AlertItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.05f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(item.subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        // Verified Cyan Circle Icon
        Surface(
            shape = CircleShape,
            color = CyberCyan.copy(alpha = 0.2f),
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = "Verified",
                    tint = CyberCyan,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun CyberMetricCard(title: String, value: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .width(110.dp)
            .height(110.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // Outer glow layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            RoyalGold.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Card Background (Dark & Semi-transparent)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.8f),
                            Color.Transparent,
                            accentColor.copy(alpha = 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title.uppercase(),
                color = RoyalGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                style = MaterialTheme.typography.bodySmall.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = RoyalGold.copy(alpha = 0.8f),
                        blurRadius = 8f
                    )
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.bodyLarge.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.White.copy(alpha = 0.5f),
                        blurRadius = 6f
                    )
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Enhanced neon underline with glow
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(1.5.dp)
                    )
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                accentColor,
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )
        }
    }
}

@Composable
fun ArmedStatusIndicator(
    isArmed: Boolean,
    onToggle: () -> Unit
) {
    val color = if (isArmed) Color(0xFF00E676) else Color(0xFFFF3B30) // Green vs Red
    val statusText = if (isArmed) "SYSTEM ARMED" else "SYSTEM DISARMED"
    val ringColor = if (isArmed) Color(0xFF00E676) else Color(0xFFFF3B30)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
    ) {
        // Interactive Neon Circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .clickable(onClick = onToggle)
        ) {
            // Outer Glow
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ringColor.copy(alpha = 0.4f), Color.Transparent)
                    ),
                    radius = size.minDimension / 2
                )
            }

            // Neon Ring
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(130.dp),
                color = ringColor,
                strokeWidth = 4.dp,
                trackColor = ringColor.copy(alpha = 0.2f),
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isArmed) Icons.Default.Shield else Icons.Default.Warning,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isArmed) "ARMED" else "OFF",
                    color = color,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Details
        AnimatedVisibility(
            visible = isArmed,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "ACTIVE DEFENSES",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatusDetailItem("Firewall", true)
                    StatusDetailItem("AI Monitor", true)
                    StatusDetailItem("VPN", true)
                }
            }
        }
    }
}

@Composable
fun StatusDetailItem(label: String, isActive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(if (isActive) Color(0xFF00E676) else Color.Gray, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}
// AnimatedNeonCard moved to com.royalshield.app.ui.components
