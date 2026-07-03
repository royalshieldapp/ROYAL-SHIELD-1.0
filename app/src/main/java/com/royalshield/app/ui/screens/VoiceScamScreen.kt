package com.royalshield.app.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.R
import com.royalshield.app.ui.theme.RoyalGold
import kotlinx.coroutines.delay

@Composable
fun VoiceScamScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val roleManager: RoleManager? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        context.getSystemService(RoleManager::class.java)
    } else null

    var isEnabled by remember { 
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && roleManager != null) {
                roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            } else false
        )
    }
    
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && roleManager != null) {
            isEnabled = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        }
    }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    val gold = RoyalGold
    val activeGreen = Color(0xFF00FF94)
    val surfaceColor = Color(0xFF121216)

    var isDeepAnalysisEnabled by remember { mutableStateOf(false) }
    var isAutoBlockEnabled by remember { mutableStateOf(true) }

    // Live metrics simulation
    var callsScreened by remember { mutableIntStateOf(47) }
    var threatsBlocked by remember { mutableIntStateOf(3) }
    var uptimeMinutes by remember { mutableIntStateOf(1440) } // 24h in minutes

    // Simulate live counter
    LaunchedEffect(isEnabled) {
        while (isEnabled) {
            delay(12000) // Every 12 seconds
            callsScreened++
            if ((1..15).random() == 1) threatsBlocked++ // ~6% chance
            uptimeMinutes++
        }
    }

    // Pulse animation for active ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val statusColor by animateColorAsState(
        targetValue = if (isEnabled) activeGreen else Color.Gray,
        label = "statusColor"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.bg_cyber_security),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "AI Voice Protection",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // ── Status Card with Pulse ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated pulse ring
                    Box(contentAlignment = Alignment.Center) {
                        if (isEnabled) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .scale(pulseScale)
                                    .border(2.dp, activeGreen.copy(alpha = pulseAlpha), CircleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(statusColor.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                null,
                                tint = statusColor,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Service connection indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isEnabled) activeGreen else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnabled) "SERVICE CONNECTED" else "SERVICE OFFLINE",
                            color = if (isEnabled) activeGreen else Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isEnabled) "VOICE MONITORING ACTIVE" else "MONITORING DISABLED",
                        color = statusColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Protecting against Deepfake and Voice Cloning scams in real-time.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Live Metrics Dashboard ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiveMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Phone,
                    value = callsScreened.toString(),
                    label = "Screened",
                    accentColor = gold
                )
                LiveMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Shield,
                    value = threatsBlocked.toString(),
                    label = "Blocked",
                    accentColor = Color(0xFFFF5252)
                )
                LiveMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Timer,
                    value = "${uptimeMinutes / 60}h",
                    label = "Uptime",
                    accentColor = activeGreen
                )
            }

            // ── Protection Settings ──
            Text(
                "PROTECTION SETTINGS",
                color = gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            VoiceSettingsToggleItem(
                title = "Live Call Analysis",
                subtitle = "Screen incoming calls for synthetic voices.",
                icon = Icons.Default.Security,
                checked = isEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        // Request Role if Q+
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && roleManager != null) {
                            if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                roleLauncher.launch(intent)
                            } else {
                                isEnabled = true
                            }
                        }
                        
                        // Also check overlay permission
                        if (!Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            overlayLauncher.launch(intent)
                        }
                    } else {
                        // Note: Roles usually need to be unset in system settings by the user
                        isEnabled = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            VoiceSettingsToggleItem(
                title = "Deep Context Scan",
                subtitle = "Analyze speech patterns for urgent scam behaviors.",
                icon = Icons.Default.Analytics,
                checked = isDeepAnalysisEnabled,
                onCheckedChange = { isDeepAnalysisEnabled = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            VoiceSettingsToggleItem(
                title = "Auto-Block High Risk",
                subtitle = "Automatically reject calls above 90% scam score.",
                icon = Icons.Default.Block,
                checked = isAutoBlockEnabled,
                onCheckedChange = { isAutoBlockEnabled = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Recent Analysis ──
            Text(
                "RECENT SCAM ANALYSIS",
                color = gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ScamHistoryItem(
                    phone = "+44 20 7946 0123",
                    time = "10:45 AM",
                    risk = "92% CRITICAL",
                    verdict = "AI Voice Deepfake Detected",
                    color = Color(0xFFFF5252)
                )
                ScamHistoryItem(
                    phone = "+1 800 555 0199",
                    time = "Yesterday",
                    risk = "65% SUSPICIOUS",
                    verdict = "Neural Pattern Mismatch",
                    color = gold
                )
                ScamHistoryItem(
                    phone = "+34 600 123 456",
                    time = "Feb 10",
                    risk = "12% SAFE",
                    verdict = "Verified Human Signature",
                    color = activeGreen
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Info Card ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = gold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Our neural engine analyzes vocal artifacts during the first 5 seconds of the call to verify caller authenticity.",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Live Metric Card ──
@Composable
fun LiveMetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A20)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── Scam History Item ──
@Composable
fun ScamHistoryItem(phone: String, time: String, risk: String, verdict: String, color: Color) {
    Surface(
        color = Color(0xFF1E1E24),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.History, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(phone, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(time, color = Color.Gray, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(risk, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("•", color = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(verdict, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }
    }
}

// ── Settings Toggle ──
@Composable
fun VoiceSettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E24))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = RoyalGold, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RoyalGold,
                checkedTrackColor = RoyalGold.copy(alpha = 0.5f)
            )
        )
    }
}
