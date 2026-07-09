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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
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

@Composable
fun CyberHomeScreen(
    onBack: () -> Unit = {},
    onNavigateToMap: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToVoiceScam: () -> Unit = {},
    onNavigateToXdr: () -> Unit = {},
    onNavigateToIntel: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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

                // Dynamic Metrics State
                var attacksCount by remember { mutableLongStateOf(1240500L) }
                var botnetsCount by remember { mutableIntStateOf(892) }
                var iotCount by remember { mutableIntStateOf(45200) }

                // Simulation Loop
                LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(800) // Update every 800ms
                        attacksCount += (1..50).random()
                        // Random fluctuation for botnets
                        if ((0..1).random() == 1) botnetsCount += (1..3).random() else botnetsCount -= (1..3).random()
                        iotCount += (1..10).random()
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween // Space them evenly
                ) {
                    CyberMetricCard("Attacks", String.format("%,d", attacksCount), NeonRed)
                    CyberMetricCard("Target", "Banking", CyberCyan)
                    CyberMetricCard("Botnets", botnetsCount.toString(), RoyalGold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cyber Stats Graph
                Image(
                    painter = painterResource(id = com.royalshield.app.R.drawable.cyber_stats_graph),
                    contentDescription = "Global Cybercrime Cost Evolution",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp) // Slightly taller to show the graph clearly
                        .padding(horizontal = 0.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CyberEmbedCard(
                        title = "THREAT EDUCATION",
                        assetPath = "file:///android_asset/threat_education_embed.html",
                        modifier = Modifier.weight(1f).height(190.dp)
                    )
                    CyberEmbedCard(
                        title = "ROYAL TERMS",
                        assetPath = "file:///android_asset/terms_embed.html",
                        modifier = Modifier.weight(1f).height(190.dp)
                    )
                }

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
private fun CyberEmbedCard(
    title: String,
    assetPath: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0D0D12))
            .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = android.webkit.WebViewClient()
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    loadUrl(assetPath)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopStart)
        ) {
            Text(
                title,
                color = CyberCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
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
