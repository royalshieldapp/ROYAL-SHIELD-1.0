package com.royalshield.app.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Colors mapped to Design 3
val GoldColor = Color(0xFFD4AF37)
val DarkBackground = Color(0xFF0A0A0A)
val CardBackground = Color(0xFF141414)

@Composable
fun CameraRadarMap() {
    var angle by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            angle = (angle + 2f) % 360f
            delay(16)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2

            // Draw concentric circles
            for (i in 1..4) {
                drawCircle(
                    color = GoldColor.copy(alpha = 0.2f),
                    radius = maxRadius * (i / 4f),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Draw crosshairs
            drawLine(
                color = GoldColor.copy(alpha = 0.2f),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = GoldColor.copy(alpha = 0.2f),
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 1.dp.toPx()
            )

            // Draw sweeping radar
            val sweepPath = Path().apply {
                moveTo(center.x, center.y)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        center.x - maxRadius,
                        center.y - maxRadius,
                        center.x + maxRadius,
                        center.y + maxRadius
                    ),
                    startAngleDegrees = angle - 45f,
                    sweepAngleDegrees = 45f,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(
                path = sweepPath,
                color = GoldColor.copy(alpha = 0.3f)
            )
            
            // Draw dummy camera dots
            val dotRadius = 4.dp.toPx()
            drawCircle(GoldColor, dotRadius, Offset(center.x - 50f, center.y - 40f))
            drawCircle(GoldColor, dotRadius, Offset(center.x + 60f, center.y - 20f))
            drawCircle(GoldColor, dotRadius, Offset(center.x - 20f, center.y + 70f))
            drawCircle(GoldColor, dotRadius, Offset(center.x + 80f, center.y + 50f))
            drawCircle(GoldColor, dotRadius, Offset(center.x, center.y + 30f))
        }

        // Overlay Text
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("5 ACTIVE", color = GoldColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "CAMERA MAP",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
    }
}

data class CameraMock(val id: String, val name: String, val status: String)

@Composable
fun CameraMiniatureList() {
    val cameras = listOf(
        CameraMock("CAM 01", "Entrance", "LIVE"),
        CameraMock("CAM 02", "Lobby", "LIVE"),
        CameraMock("CAM 03", "Parking", "LIVE"),
        CameraMock("CAM 04", "Perimeter", "LIVE")
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LIVE CAMERAS", color = Color.White, fontSize = 12.sp)
            Text("VIEW ALL", color = Color.Gray, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cameras) { cam ->
                Column(
                    modifier = Modifier.width(120.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(80.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                    ) {
                        // Icon as placeholder for image
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.Green)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(cam.id, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(cam.name, color = Color.LightGray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun ControlPanelList() {
    var showCamerasDialog by remember { mutableStateOf(false) }
    var showRecordingsDialog by remember { mutableStateOf(false) }
    var showAlertsDialog by remember { mutableStateOf(false) }
    var showReportsDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }

    val items = listOf(
        Triple("CAMERAS", "Real-time monitoring", Icons.Default.Videocam) to { showCamerasDialog = true },
        Triple("RECORDINGS", "Search and play videos", Icons.Default.PlayArrow) to { showRecordingsDialog = true },
        Triple("ALERTS", "Events and notifications", Icons.Default.Notifications) to { showAlertsDialog = true },
        Triple("REPORTS", "Security reports", Icons.Default.Description) to { showReportsDialog = true },
        Triple("SETTINGS", "System settings", Icons.Default.Settings) to { showConfigDialog = true }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(CardBackground, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Text(
            text = "CONTROL PANEL",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 16.dp)
        )

        items.forEachIndexed { index, (item, onClickAction) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClickAction() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GoldColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.third, contentDescription = item.first, tint = GoldColor)
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.first, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(item.second, color = Color.Gray, fontSize = 12.sp)
                }
                
                Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = Color.Gray)
            }
            
            if (index < items.size - 1) {
                Divider(color = Color.DarkGray, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }

    // Dialogs
    if (showCamerasDialog) {
        AlertDialog(
            onDismissRequest = { showCamerasDialog = false },
            containerColor = CardBackground,
            title = { Text("ACTIVE CAMERAS", color = GoldColor) },
            text = { Text("Local network camera list. All connected.", color = Color.White) },
            confirmButton = {
                TextButton(onClick = { showCamerasDialog = false }) { Text("CLOSE", color = GoldColor) }
            }
        )
    }

    if (showRecordingsDialog) {
        AlertDialog(
            onDismissRequest = { showRecordingsDialog = false },
            containerColor = CardBackground,
            title = { Text("RECENT RECORDINGS", color = GoldColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("- 2024-01-15 14:30 - CAM 01 (2:45)", color = Color.White)
                    Text("- 2024-01-15 12:00 - CAM 03 (5:12)", color = Color.White)
                    Text("- 2024-01-14 20:15 - CAM 02 (1:30)", color = Color.White)
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecordingsDialog = false }) { Text("CLOSE", color = GoldColor) }
            }
        )
    }

    if (showAlertsDialog) {
        AlertDialog(
            onDismissRequest = { showAlertsDialog = false },
            containerColor = CardBackground,
            title = { Text("SECURITY ALERTS", color = GoldColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Motion detected - CAM 01", color = Color.Yellow)
                    Text("Unidentified person - CAM 03", color = Color.Red)
                    Text("Camera offline - CAM 05", color = Color.Gray)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlertsDialog = false }) { Text("CLOSE", color = GoldColor) }
            }
        )
    }

    if (showReportsDialog) {
        AlertDialog(
            onDismissRequest = { showReportsDialog = false },
            containerColor = CardBackground,
            title = { Text("SECURITY REPORT", color = GoldColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Events today: 12", color = Color.White)
                    Text("Active cameras: 4/5", color = Color.White)
                    Text("Recording hours: 18.5h", color = Color.White)
                    Text("Storage: 2.3 GB / 10 GB", color = Color.White)
                    LinearProgressIndicator(progress = 0.23f, color = GoldColor, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = { showReportsDialog = false }) { Text("CLOSE", color = GoldColor) }
            }
        )
    }

    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            containerColor = CardBackground,
            title = { Text("CAMERA SETTINGS", color = GoldColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resolution: 1080p", color = Color.White)
                    Text("Night Vision: Auto", color = Color.White)
                    Text("Motion Sensitivity: High", color = Color.White)
                    Text("Automatic Recording: Enabled", color = Color.White)
                }
            },
            confirmButton = {
                TextButton(onClick = { showConfigDialog = false }) { Text("SAVE", color = GoldColor) }
            }
        )
    }
}
