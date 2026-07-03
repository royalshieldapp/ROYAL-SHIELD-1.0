package com.royalshield.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.NeonRed
import com.royalshield.app.viewmodels.XdrViewModel
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// XDR Theme Colors
val XdrBackground = Color(0xFF0F111A)
val XdrSurface = Color(0xFF1A1C26)
val DangerRed = Color(0xFFFF3B30)
val WarningOrange = Color(0xFFFF9500)
val SafeBlue = Color(0xFF5856D6)
val TextPrimary = Color.White
val TextSecondary = Color(0xFF8F9BB3)

@Composable
fun XdrScreen(onBack: () -> Unit) {
    val xdrViewModel: XdrViewModel = viewModel()
    val telemetry by xdrViewModel.telemetry.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(XdrBackground)
    ) {
        // Grid Pattern Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 40.dp.toPx()
            val lineColor = Color.White.copy(alpha = 0.03f)
            
            for (x in 0..size.width.toInt() step step.toInt()) {
                drawLine(
                    color = lineColor,
                    start = Offset(x.toFloat(), 0f),
                    end = Offset(x.toFloat(), size.height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..size.height.toInt() step step.toInt()) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y.toFloat()),
                    end = Offset(size.width, y.toFloat()),
                    strokeWidth = 1f
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            XdrHeader(onBack)
            
            // Main Content Area
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel - Sidebar / Mini Stats
                val context = androidx.compose.ui.platform.LocalContext.current
                var selectedTab by remember { mutableStateOf("Dashboard") }

                Column(
                    modifier = Modifier
                        .width(70.dp)
                        .fillMaxHeight()
                        .background(XdrSurface)
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    XdrSidebarIcon(Icons.Default.Dashboard, selectedTab == "Dashboard") { selectedTab = "Dashboard" }
                    XdrSidebarIcon(Icons.Default.Security, selectedTab == "Security") { 
                        selectedTab = "Security"
                        android.widget.Toast.makeText(context, "Security module launching...", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    XdrSidebarIcon(Icons.Default.Assessment, selectedTab == "Assessment") { 
                        selectedTab = "Assessment"
                        android.widget.Toast.makeText(context, "Risk Assessment scanning...", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    XdrSidebarIcon(Icons.Default.Settings, selectedTab == "Settings") { 
                        selectedTab = "Settings"
                        android.widget.Toast.makeText(context, "XDR Settings opened", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    XdrSidebarIcon(Icons.Default.ExitToApp, false) { onBack() }
                }
                
                // Dashboard Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Stats Row
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            XdrStatCard(
                                title = "Investigations",
                                value = telemetry.investigations.toString(),
                                subValue = "Active Issues",
                                trend = if (telemetry.investigations > 0) "Crit" else "Safe",
                                color = if (telemetry.investigations > 0) DangerRed else SafeBlue,
                                modifier = Modifier.weight(1f)
                            )
                            XdrStatCard(
                                title = "Exposed Entities",
                                value = telemetry.exposedEntities.toString(),
                                subValue = "Risky Apps",
                                trend = "Active",
                                color = WarningOrange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // System Resources Row
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            XdrStatCard(
                                title = "Memory (RAM)",
                                value = "${telemetry.totalRamMb - telemetry.freeRamMb} MB",
                                subValue = "Total: ${telemetry.totalRamMb} MB",
                                trend = "${((telemetry.totalRamMb - telemetry.freeRamMb).toFloat() / maxOf(1L, telemetry.totalRamMb) * 100).toInt()}%",
                                color = SafeBlue,
                                modifier = Modifier.weight(1f)
                            )
                            XdrStatCard(
                                title = "Network Tx/Rx",
                                value = "${String.format("%.1f", telemetry.networkTxMb)} MB / ${String.format("%.1f", telemetry.networkRxMb)} MB",
                                subValue = "Since boot",
                                trend = "Tracking",
                                color = SafeBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // Central Network Graph (Interactive)
                    item {
                         XdrNetworkGraph(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .height(300.dp)
                                 .clip(RoundedCornerShape(16.dp))
                                 .background(XdrSurface)
                                 .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                         )
                    }
                    
                    // Investigation List
                    item {
                        Text(
                            text = "Incident Report (Last 24h)",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(listOf(
                        XdrIncident("Brute Force Attack", "Network Layer", "Critical", DangerRed),
                        XdrIncident("Anomalous Login", "Cloud Service", "High", WarningOrange),
                        XdrIncident("Malware Payload", "Endpoint: Workstation-04", "Critical", DangerRed),
                        XdrIncident("Port Scanning", "External IP", "Medium", SafeBlue)
                    )) { incident ->
                        IncidentItem(incident)
                    }
                }
            }
        }
    }
}

@Composable
fun IncidentItem(incident: XdrIncident) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(XdrSurface, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(incident.title, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(incident.source, color = TextSecondary, fontSize = 12.sp)
        }
        Text(incident.severity, color = incident.color, fontWeight = FontWeight.Bold)
    }
}

// ...

data class XdrIncident(val title: String, val source: String, val severity: String, val color: Color)
