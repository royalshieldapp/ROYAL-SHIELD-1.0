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
            Text("5 ACTIVAS", color = GoldColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "MAPA DE CÁMARAS",
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
        CameraMock("CAM 01", "Entrada", "LIVE"),
        CameraMock("CAM 02", "Lobby", "LIVE"),
        CameraMock("CAM 03", "Parqueo", "LIVE"),
        CameraMock("CAM 04", "Perímetro", "LIVE")
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CÁMARAS EN VIVO", color = Color.White, fontSize = 12.sp)
            Text("VER TODAS", color = Color.Gray, fontSize = 12.sp)
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
        Triple("CÁMARAS", "Monitoreo en tiempo real", Icons.Default.Videocam) to { showCamerasDialog = true },
        Triple("GRABACIONES", "Buscar y reproducir videos", Icons.Default.PlayArrow) to { showRecordingsDialog = true },
        Triple("ALERTAS", "Eventos y notificaciones", Icons.Default.Notifications) to { showAlertsDialog = true },
        Triple("REPORTES", "Informes de seguridad", Icons.Default.Description) to { showReportsDialog = true },
        Triple("CONFIGURACIÓN", "Ajustes del sistema", Icons.Default.Settings) to { showConfigDialog = true }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(CardBackground, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Text(
            text = "PANEL DE CONTROL",
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
                
                Icon(Icons.Default.ChevronRight, contentDescription = "Ir", tint = Color.Gray)
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
            title = { Text("CÁMARAS ACTIVAS", color = GoldColor) },
            text = { Text("Lista de cámaras en red local. Todas conectadas.", color = Color.White) },
            confirmButton = {
                TextButton(onClick = { showCamerasDialog = false }) { Text("CERRAR", color = GoldColor) }
            }
        )
    }

    if (showRecordingsDialog) {
        AlertDialog(
            onDismissRequest = { showRecordingsDialog = false },
            containerColor = CardBackground,
            title = { Text("GRABACIONES RECIENTES", color = GoldColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• 2024-01-15 14:30 - CAM 01 (2:45)", color = Color.White)
                    Text("• 2024-01-15 12:00 - CAM 03 (5:12)", color = Color.White)
                    Text("• 2024-01-14 20:15 - CAM 02 (1:30)", color = Color.White)
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecordingsDialog = false }) { Text("CERRAR", color = GoldColor) }
            }
        )
    }

    if (showAlertsDialog) {
        AlertDialog(
            onDismissRequest = { showAlertsDialog = false },
            containerColor = CardBackground,
            title = { Text("ALERTAS DE SEGURIDAD", color = GoldColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚠️ Movimiento detectado - CAM 01", color = Color.Yellow)
                    Text("❌ Persona no identificada - CAM 03", color = Color.Red)
                    Text("📶 Cámara offline - CAM 05", color = Color.Gray)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlertsDialog = false }) { Text("CERRAR", color = GoldColor) }
            }
        )
    }

    if (showReportsDialog) {
        AlertDialog(
            onDismissRequest = { showReportsDialog = false },
            containerColor = CardBackground,
            title = { Text("REPORTE DE SEGURIDAD", color = GoldColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Eventos hoy: 12", color = Color.White)
                    Text("Cámaras activas: 4/5", color = Color.White)
                    Text("Horas de grabación: 18.5h", color = Color.White)
                    Text("Almacenamiento: 2.3 GB / 10 GB", color = Color.White)
                    LinearProgressIndicator(progress = 0.23f, color = GoldColor, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = { showReportsDialog = false }) { Text("CERRAR", color = GoldColor) }
            }
        )
    }

    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            containerColor = CardBackground,
            title = { Text("CONFIGURACIÓN DE CÁMARAS", color = GoldColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resolución: 1080p", color = Color.White)
                    Text("Visión Nocturna: Auto", color = Color.White)
                    Text("Sensibilidad de Movimiento: Alta", color = Color.White)
                    Text("Grabación Automática: Activado", color = Color.White)
                }
            },
            confirmButton = {
                TextButton(onClick = { showConfigDialog = false }) { Text("GUARDAR", color = GoldColor) }
            }
        )
    }
}
