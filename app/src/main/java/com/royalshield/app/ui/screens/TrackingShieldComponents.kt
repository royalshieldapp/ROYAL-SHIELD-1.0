package com.royalshield.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.features.trackingshield.data.ChildDevice
import com.royalshield.app.features.trackingshield.data.ChildStatus
import com.royalshield.app.features.trackingshield.data.HistorySession
import com.royalshield.app.features.trackingshield.data.NavRoute
import com.royalshield.app.features.trackingshield.data.*

// ── Design Tokens ───────────────────────────────────────────
private val BgBase       = Color(0xFF07070A)
private val GlassBg      = Color(0xBF07070A)
private val GlassBorder  = Color(0x0FFFFFFF)
private val NeonCyan     = Color(0xFF00E5FF)
private val NeonGold     = Color(0xFFFFD700)
private val NeonGreen    = Color(0xFF00FF9C)
private val NeonRed      = Color(0xFFFF1E1E)
private val NeonPurple   = Color(0xFFBF5AF2)
private val TsTextPrimary  = Color(0xFFF0F4FF)
private val TsTextSecondary= Color(0x8CF0F4FF)

// ═══════════════════════════════════════════════════════════
// NEON BORDER BOX — animated gradient border cycling 3 colors
// ═══════════════════════════════════════════════════════════
@Composable
fun NeonBorderBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "neon")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neonPhase"
    )

    val color1 = lerp3Color(NeonCyan, NeonGold, NeonGreen, phase)
    val color2 = lerp3Color(NeonGold, NeonGreen, NeonCyan, phase)

    Box(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(color1, color2)),
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                    style = Stroke(width = borderWidth.toPx())
                )
            }
    ) {
        content()
    }
}

/** Smooth 3-color lerp cycle */
private fun lerp3Color(c1: Color, c2: Color, c3: Color, t: Float): Color {
    return when {
        t < 0.333f -> lerpColor(c1, c2, t / 0.333f)
        t < 0.666f -> lerpColor(c2, c3, (t - 0.333f) / 0.333f)
        else       -> lerpColor(c3, c1, (t - 0.666f) / 0.334f)
    }
}

private fun lerpColor(a: Color, b: Color, f: Float): Color {
    return Color(
        red   = a.red   + (b.red   - a.red)   * f,
        green = a.green + (b.green - a.green) * f,
        blue  = a.blue  + (b.blue  - a.blue)  * f,
        alpha = a.alpha + (b.alpha - a.alpha) * f
    )
}

// ═══════════════════════════════════════════════════════════
// GLASSMORPHISM SURFACE
// ═══════════════════════════════════════════════════════════
@Composable
fun GlassmorphismSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    NeonBorderBox(
        modifier = modifier,
        cornerRadius = cornerRadius
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(GlassBg)
                .border(1.dp, GlassBorder, RoundedCornerShape(cornerRadius))
        ) {
            content()
        }
    }
}

// ═══════════════════════════════════════════════════════════
// LIVE BADGE — pulsating green dot + "LIVE"
// ═══════════════════════════════════════════════════════════
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveAlpha"
    )

    Row(
        modifier = modifier
            .background(Color(0xFF0A2A10), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(NeonGreen.copy(alpha = alpha))
        )
        Text(
            "LIVE",
            color = NeonGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════
// STATUS TOP BAR
// ═══════════════════════════════════════════════════════════
@Composable
fun StatusTopBar(
    child: ChildDevice?,
    lastUpdateText: String,
    modifier: Modifier = Modifier
) {
    GlassmorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LiveBadge()

            Text(
                text = "Updated: $lastUpdateText",
                color = TsTextSecondary,
                fontSize = 11.sp
            )

            // Battery
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = batteryColor(child?.batteryPercent ?: 100),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "${child?.batteryPercent ?: 0}%",
                    color = TsTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // GPS
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = if (child?.gpsActive == true) NeonGreen else NeonRed,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    if (child?.gpsActive == true) "GPS ✓" else "GPS ✗",
                    color = TsTextPrimary,
                    fontSize = 11.sp
                )
            }

            // Signal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    Icons.Default.SignalCellularAlt,
                    contentDescription = null,
                    tint = if ((child?.signalStrength ?: 0) >= 2) NeonCyan else NeonRed,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "Signal ✓",
                    color = TsTextPrimary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun batteryColor(percent: Int): Color = when {
    percent > 50 -> NeonGreen
    percent > 20 -> NeonGold
    else         -> NeonRed
}

// ═══════════════════════════════════════════════════════════
// DOCK BUTTON
// ═══════════════════════════════════════════════════════════
@Composable
fun DockButton(
    icon: ImageVector,
    label: String,
    tint: Color = NeonCyan,
    isAlert: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isAlert) NeonRed else tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = if (isAlert) NeonRed else TsTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ═══════════════════════════════════════════════════════════
// BOTTOM DOCK
// ═══════════════════════════════════════════════════════════
@Composable
fun BottomDock(
    onHome: () -> Unit,
    onCenter: () -> Unit,
    onHistory: () -> Unit,
    onZones: () -> Unit,
    onControl: () -> Unit,
    onSOS: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DockButton(Icons.Default.Home, "Home", onClick = onHome)
            DockButton(Icons.Default.MyLocation, "Center", onClick = onCenter)
            DockButton(Icons.Default.Timeline, "History", onClick = onHistory)
            DockButton(Icons.Default.Shield, "Zones", tint = NeonGold, onClick = onZones)
            DockButton(Icons.Default.Settings, "Control", onClick = onControl)
            DockButton(Icons.Default.Warning, "SOS", isAlert = true, onClick = onSOS)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// CHILD STATUS HALO COLOR
// ═══════════════════════════════════════════════════════════
fun childHaloColor(status: ChildStatus): Color = when (status) {
    ChildStatus.SAFE    -> NeonGreen
    ChildStatus.WARNING -> NeonGold
    ChildStatus.DANGER  -> NeonRed
}

// ═══════════════════════════════════════════════════════════
// ZONE TYPE to Display Color
// ═══════════════════════════════════════════════════════════
fun zoneColor(type: com.royalshield.app.features.trackingshield.data.ZoneType): Color = when (type) {
    com.royalshield.app.features.trackingshield.data.ZoneType.HOME   -> NeonCyan
    com.royalshield.app.features.trackingshield.data.ZoneType.SCHOOL -> NeonGold
    com.royalshield.app.features.trackingshield.data.ZoneType.PARK   -> NeonGreen
    com.royalshield.app.features.trackingshield.data.ZoneType.CUSTOM -> NeonPurple
}

// ═══════════════════════════════════════════════════════════
// MODE CHIPS ROW (LIVE / NAV / HISTORY)
// ═══════════════════════════════════════════════════════════
@Composable
fun ModeChipsRow(
    currentMode: TrackingMode,
    onModeSelected: (TrackingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TrackingMode.entries.forEach { mode ->
                val isSelected = mode == currentMode
                val chipColor = when (mode) {
                    TrackingMode.LIVE -> NeonGreen
                    TrackingMode.NAV -> NeonCyan
                    TrackingMode.HISTORY -> NeonGold
                }
                val chipIcon = when (mode) {
                    TrackingMode.LIVE -> Icons.Default.MyLocation
                    TrackingMode.NAV -> Icons.Default.Navigation
                    TrackingMode.HISTORY -> Icons.Default.Timeline
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) chipColor.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) chipColor else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onModeSelected(mode) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        chipIcon,
                        contentDescription = mode.name,
                        tint = if (isSelected) chipColor else TsTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        mode.name,
                        color = if (isSelected) chipColor else TsTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// NAV BANNER — turn-by-turn instruction overlay
// ═══════════════════════════════════════════════════════════
@Composable
fun NavBanner(
    navRoute: NavRoute,
    modifier: Modifier = Modifier
) {
    GlassmorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        cornerRadius = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Instruction
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    navRoute.currentInstruction,
                    color = TsTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))
            // Distance + ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = NeonGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        navRoute.destinationName,
                        color = NeonGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    "${String.format("%.1f", navRoute.distanceKm)} km",
                    color = TsTextSecondary,
                    fontSize = 11.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "${navRoute.etaMinutes} min",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// SESSION CARD — single history entry
// ═══════════════════════════════════════════════════════════
@Composable
fun SessionCard(
    session: HistorySession,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) NeonCyan else NeonCyan.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.08f) else GlassBg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        session.label,
                        color = TsTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        session.date,
                        color = TsTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        String.format("%.1f", session.distanceKm),
                        color = NeonGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("km", color = TsTextSecondary, fontSize = 9.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${session.durationMinutes}",
                        color = NeonGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("min", color = TsTextSecondary, fontSize = 9.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// HISTORY TIMELINE SHEET — scrollable session list
// ═══════════════════════════════════════════════════════════
@Composable
fun HistoryTimelineSheet(
    sessions: List<HistorySession>,
    activeSession: HistorySession?,
    onSessionSelected: (HistorySession) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TsTextSecondary.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "LOCATION HISTORY",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(sessions) { session ->
                    SessionCard(
                        session = session,
                        isSelected = session.id == activeSession?.id,
                        onClick = { onSessionSelected(session) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// PLAYBACK CONTROLS — play/pause, speed, progress slider
// ═══════════════════════════════════════════════════════════
@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    progress: Float,
    speed: Float,
    activeSession: HistorySession?,
    onTogglePlayback: () -> Unit,
    onCycleSpeed: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        cornerRadius = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Time label
            activeSession?.let { session ->
                val totalPoints = session.breadcrumbs.size
                val currentIndex = (progress * (totalPoints - 1)).toInt().coerceIn(0, totalPoints - 1)
                val currentBreadcrumb = session.breadcrumbs.getOrNull(currentIndex)
                val timeText = currentBreadcrumb?.let {
                    val elapsed = ((it.timestamp - session.breadcrumbs.first().timestamp) / 60000).toInt()
                    "${elapsed}m elapsed"
                } ?: "—"

                Text(
                    timeText,
                    color = TsTextSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(Modifier.height(4.dp))

            // Slider
            Slider(
                value = progress,
                onValueChange = onSeek,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = NeonCyan.copy(alpha = 0.2f)
                )
            )

            // Buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGold.copy(alpha = 0.1f))
                        .border(1.dp, NeonGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onCycleSpeed)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${speed.toInt()}x",
                        color = NeonGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Play / Pause
                IconButton(
                    onClick = onTogglePlayback,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan, CircleShape)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Progress percentage
                Text(
                    "${(progress * 100).toInt()}%",
                    color = TsTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// SAFE ZONES SHEET — list and manage zones
// ═══════════════════════════════════════════════════════════
@Composable
fun SafeZonesSheet(
    safeZones: List<com.royalshield.app.features.trackingshield.data.SafeZone>,
    riskZones: List<com.royalshield.app.features.trackingshield.data.RiskZone>,
    onDeleteSafe: (String) -> Unit,
    onDeleteRisk: (String) -> Unit,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    GlassmorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                "ACTIVE SHIELD ZONES",
                color = NeonGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            TextButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Close", color = NeonGold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            
            if (safeZones.isEmpty() && riskZones.isEmpty()) {
                Text(
                    "No zones set. Long-press on the map to add a new safe zone.",
                    color = TsTextSecondary,
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(safeZones) { zone ->
                    ZoneItem(
                        name = zone.name,
                        details = "${zone.radiusMeters.toInt()}m radius • ${zone.type}",
                        color = zoneColor(zone.type),
                        onDelete = { onDeleteSafe(zone.id) }
                    )
                }
                items(riskZones) { risk ->
                    ZoneItem(
                        name = risk.name,
                        details = "${risk.radiusMeters.toInt()}m radius • Level ${risk.threatLevel}",
                        color = NeonRed,
                        onDelete = { onDeleteRisk(risk.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ZoneItem(
    name: String,
    details: String,
    color: Color,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.05f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name, color = TsTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(details, color = TsTextSecondary, fontSize = 10.sp)
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, null, tint = NeonRed, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ChildControlPanelSheet(
    child: ChildDevice?,
    currentGpsMode: GpsMode,
    onGpsModeSelected: (GpsMode) -> Unit,
    onRefreshLocation: () -> Unit,
    onPlayAlertSound: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONTROL PANEL",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = TsTextSecondary)
                }
            }

            // Child Target Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = child?.status?.let { childHaloColor(it) } ?: TsTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = child?.name ?: "No Child Selected",
                        color = TsTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Remote Management Active",
                        color = NeonGreen,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val battery = child?.batteryPercent ?: 0
                        val signal = child?.signalStrength ?: 4
                        val signalText = when (signal) {
                            4 -> "Excellent"
                            3 -> "Good"
                            2 -> "Fair"
                            else -> "Poor"
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = batteryColor(battery),
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Battery: $battery%", color = TsTextSecondary, fontSize = 11.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.SignalCellularAlt,
                                contentDescription = null,
                                tint = if (signal >= 2) NeonCyan else NeonRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Signal: $signalText", color = TsTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            HorizontalDivider(color = GlassBorder)

            // Actions
            Text("QUICK ACTIONS", color = TsTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRefreshLocation,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Refresh Loc", color = NeonCyan, fontSize = 12.sp)
                }

                Button(
                    onClick = onPlayAlertSound,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).border(1.dp, NeonRed, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = NeonRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Play Alert", color = NeonRed, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = GlassBorder)

            // GPS Mode
            Text("GPS POLLING MODE", color = TsTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GpsModeOption(
                    mode = GpsMode.HIGH_ACCURACY,
                    title = "High Accuracy",
                    description = "Updates every 5 seconds. High battery drain.",
                    isSelected = currentGpsMode == GpsMode.HIGH_ACCURACY,
                    icon = Icons.Default.GpsFixed,
                    color = NeonRed,
                    onClick = { onGpsModeSelected(GpsMode.HIGH_ACCURACY) }
                )
                GpsModeOption(
                    mode = GpsMode.BALANCED,
                    title = "Balanced",
                    description = "Updates every 30 seconds. Recommended.",
                    isSelected = currentGpsMode == GpsMode.BALANCED,
                    icon = Icons.Default.NetworkWifi,
                    color = NeonCyan,
                    onClick = { onGpsModeSelected(GpsMode.BALANCED) }
                )
                GpsModeOption(
                    mode = GpsMode.BATTERY_SAVER,
                    title = "Battery Saver",
                    description = "Updates every 5 minutes. Low battery drain.",
                    isSelected = currentGpsMode == GpsMode.BATTERY_SAVER,
                    icon = Icons.Default.BatterySaver,
                    color = NeonGreen,
                    onClick = { onGpsModeSelected(GpsMode.BATTERY_SAVER) }
                )
            }
        }
    }
}

@Composable
private fun GpsModeOption(
    mode: GpsMode,
    title: String,
    description: String,
    isSelected: Boolean,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent
    val borderColor = if (isSelected) color else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TsTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = TsTextSecondary, fontSize = 11.sp)
        }
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}
