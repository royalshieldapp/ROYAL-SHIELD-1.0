package com.royalshield.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.components.RoyalGradientBackground
import com.royalshield.app.ui.theme.CyberCyan
import com.royalshield.app.ui.theme.RoyalGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

fun getIconForName(name: String): ImageVector? {
    return when (name.lowercase().trim()) {
        // Main nodes
        "device core" -> Icons.Default.Memory
        "files scanner" -> Icons.Default.Search
        "permission monitor" -> Icons.Default.Visibility
        "vpn shield" -> Icons.Default.Public
        "app lock" -> Icons.Default.Lock
        "data vault" -> Icons.Default.Storage
        "threat engine" -> Icons.Default.BugReport
        "fix center" -> Icons.Default.Build

        // Sub-nodes of Device Core
        "cpu temp guard" -> Icons.Default.Thermostat
        "syslog sentinel" -> Icons.Default.Terminal
        "integrity auditor" -> Icons.AutoMirrored.Filled.FactCheck
        "kernel shield" -> Icons.Default.AdminPanelSettings

        // Sub-nodes of Files Scanner
        "signatures db" -> Icons.Default.Folder
        "realtime watcher" -> Icons.Default.Visibility
        "quarantine zone" -> Icons.Default.Block
        "hash calculator" -> Icons.Default.Calculate

        // Sub-nodes of Permission Monitor
        "cam guard" -> Icons.Default.PhotoCamera
        "mic sentinel" -> Icons.Default.Mic
        "gps tracker" -> Icons.Default.GpsFixed
        "overlay blocker" -> Icons.Default.LayersClear

        // Sub-nodes of VPN Shield
        "tunnel encryptor" -> Icons.Default.VpnKey
        "dns leak guard" -> Icons.Default.Dns
        "kill switch" -> Icons.Default.PowerSettingsNew
        "port scanner" -> Icons.Default.Router

        // Sub-nodes of App Lock
        "biometric gate" -> Icons.Default.Fingerprint
        "pin auth portal" -> Icons.Default.Password
        "task monitor" -> Icons.Default.Dashboard
        "process sandbox" -> Icons.Default.ViewInAr

        // Sub-nodes of Data Vault
        "sql cipher" -> Icons.Default.EnhancedEncryption
        "keyring manager" -> Icons.Default.Key
        "backup guard" -> Icons.Default.Backup
        "read auditor" -> Icons.AutoMirrored.Filled.MenuBook

        // Sub-nodes of Threat Engine
        "heuristics ai" -> Icons.Default.Psychology
        "network analyzer" -> Icons.Default.NetworkCheck
        "process watcher" -> Icons.AutoMirrored.Filled.TrendingUp
        "risk classifier" -> Icons.Default.Category

        // Sub-nodes of Fix Center
        "patch coordinator" -> Icons.Default.SystemUpdate
        "perm revoker" -> Icons.Default.Gavel
        "vpn reconnector" -> Icons.Default.Sync
        "cache optimizer" -> Icons.Default.DeleteSweep

        else -> null
    }
}

data class SubNode(
    val name: String,
    val description: String,
    var status: String = "NOMINAL"
)

data class EngineNode(
    val name: String,
    val abbreviation: String,
    val angle: Double, // Position on circle
    val description: String,
    val subNodes: List<SubNode>,
    var status: NodeStatus = NodeStatus.NOMINAL
)

enum class NodeStatus { NOMINAL, WARNING, ACTIVE_SCAN }

@Composable
fun SolutionEngineScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var showFlowAnimation by remember { mutableStateOf(false) }
    var activeNodeIndex by remember { mutableIntStateOf(-1) }
    var selectedNodeIndex by remember { mutableIntStateOf(0) }
    var currentLog by remember { mutableStateOf("Ready for telemetry scanning...") }
    var showLegendDialog by remember { mutableStateOf(false) }
    var showSubFlowDialog by remember { mutableStateOf(false) }
    var is3DMode by remember { mutableStateOf(false) }
    
    // Nodes state representation
    val nodes = remember {
        mutableStateListOf(
            EngineNode(
                "Device Core", "DC", 0.0, "Root CPU diagnostics & hardware monitor",
                listOf(
                    SubNode("CPU Temp Guard", "Monitors processor thermal status"),
                    SubNode("Syslog Sentinel", "Analyzes system event logger"),
                    SubNode("Integrity Auditor", "Verifies OS partition hash check"),
                    SubNode("Kernel Shield", "Protects core memory registers")
                ),
                NodeStatus.NOMINAL
            ),
            EngineNode(
                "Files Scanner", "FS", 45.0, "Scans internal storage for malicious APKs & files",
                listOf(
                    SubNode("Signatures DB", "Local virus definition table"),
                    SubNode("Realtime Watcher", "Monitors background file creations"),
                    SubNode("Quarantine Zone", "Isolates infected directories"),
                    SubNode("Hash Calculator", "Computes file SHA-256 signatures")
                ),
                NodeStatus.NOMINAL
            ),
            EngineNode(
                "Permission Monitor", "PM", 90.0, "Audits background camera, mic & location permissions",
                listOf(
                    SubNode("Cam Guard", "Detects unauthorized background camera access", "WARNING"),
                    SubNode("Mic Sentinel", "Detects background microphone recording", "WARNING"),
                    SubNode("GPS Tracker", "Audits location leakage in background"),
                    SubNode("Overlay Blocker", "Detects display overlap hijacking attempts")
                ),
                NodeStatus.WARNING
            ),
            EngineNode(
                "VPN Shield", "VP", 135.0, "Analyzes active VPN tunnel state & DNS leakage",
                listOf(
                    SubNode("Tunnel Encryptor", "AES-256 data packet encryption"),
                    SubNode("DNS Leak Guard", "Verifies secure resolver routing", "WARNING"),
                    SubNode("Kill Switch", "Blocks internet if VPN connection drops"),
                    SubNode("Port Scanner", "Blocks suspicious incoming connections")
                ),
                NodeStatus.WARNING
            ),
            EngineNode(
                "App Lock", "AL", 180.0, "Ensures sensitive apps remain encrypted & locked",
                listOf(
                    SubNode("Biometric Gate", "Authenticates via fingerprint/face unlock"),
                    SubNode("PIN Auth Portal", "Secondary secure passcode screen"),
                    SubNode("Task Monitor", "Blocks app switcher exposure"),
                    SubNode("Process Sandbox", "Restricts cross-app memory reading")
                ),
                NodeStatus.NOMINAL
            ),
            EngineNode(
                "Data Vault", "DV", 225.0, "Monitors encrypted database integrity",
                listOf(
                    SubNode("SQL Cipher", "Encrypts local app databases"),
                    SubNode("Keyring Manager", "Stores cryptographic keys securely"),
                    SubNode("Backup Guard", "Validates external storage backups"),
                    SubNode("Read Auditor", "Logs read/write access logs")
                ),
                NodeStatus.NOMINAL
            ),
            EngineNode(
                "Threat Engine", "TE", 270.0, "Real-time AI behavioral heuristic processor",
                listOf(
                    SubNode("Heuristics AI", "Predicts malicious behaviors using ML models"),
                    SubNode("Network Analyzer", "Detects connection patterns of bots"),
                    SubNode("Process Watcher", "Monitors active CPU tasks for anomalies"),
                    SubNode("Risk Classifier", "Assigns danger score based on metrics")
                ),
                NodeStatus.NOMINAL
            ),
            EngineNode(
                "Fix Center", "FC", 315.0, "Central coordination module for security updates",
                listOf(
                    SubNode("Patch Coordinator", "Applies security patches automatically"),
                    SubNode("Perm Revoker", "Revokes unnecessary background permissions"),
                    SubNode("VPN Reconnector", "Restores active VPN tunnel"),
                    SubNode("Cache Optimizer", "Deletes temporary threat definitions")
                ),
                NodeStatus.NOMINAL
            )
        )
    }

    // Animation specs
    val infiniteTransition = rememberInfiniteTransition(label = "solution_engine_inf")
    val gridPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "gridPulse"
    )
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "flowPhase"
    )

    fun runSequentialScan() {
        if (isScanning) return
        isScanning = true
        showFlowAnimation = true
        scope.launch {
            for (i in nodes.indices) {
                activeNodeIndex = i
                selectedNodeIndex = i
                nodes[i].status = NodeStatus.ACTIVE_SCAN
                currentLog = "Scanning node [${nodes[i].name}] - Running neural heuristics..."
                delay(800)
                
                // Keep Warning status for Permission Monitor and VPN Shield to show Fix Center capability
                if (nodes[i].name == "Permission Monitor") {
                    nodes[i].status = NodeStatus.WARNING
                    nodes[i].subNodes[0].status = "WARNING"
                    nodes[i].subNodes[1].status = "WARNING"
                    currentLog = "Node [${nodes[i].name}] scan completed: WARNING detected (Action Required)."
                } else if (nodes[i].name == "VPN Shield") {
                    nodes[i].status = NodeStatus.WARNING
                    nodes[i].subNodes[1].status = "WARNING"
                    currentLog = "Node [${nodes[i].name}] scan completed: WARNING detected (Action Required)."
                } else {
                    nodes[i].status = NodeStatus.NOMINAL
                    nodes[i].subNodes.forEach { it.status = "NOMINAL" }
                    currentLog = "Node [${nodes[i].name}] scan completed: NOMINAL. Secure."
                }
                delay(200)
            }
            activeNodeIndex = -1
            isScanning = false
            currentLog = "System scan finished. Review flagged nodes in Fix Center."
        }
    }

    fun applyFixes() {
        scope.launch {
            currentLog = "Fix Center resolving risks... Reconfiguring sandbox policies..."
            delay(1000)
            for (i in nodes.indices) {
                if (nodes[i].status == NodeStatus.WARNING) {
                    nodes[i].status = NodeStatus.NOMINAL
                    nodes[i].subNodes.forEach { it.status = "NOMINAL" }
                }
            }
            currentLog = "All flagged issues resolved. Solution Engine confirms 100% nominal state."
        }
    }

    RoyalGradientBackground(containerColor = Color(0xFF030610)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SOLUTION ENGINE",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Visualize how Royal Shield detects risks, protects data, and guides users toward security solutions in real time.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = { showLegendDialog = true },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Help Legend",
                        tint = RoyalGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { runSequentialScan() },
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run Scan", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showFlowAnimation = !showFlowAnimation },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showFlowAnimation) RoyalGold else Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (showFlowAnimation) Color.Black else Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Protection Flow", color = if (showFlowAnimation) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { applyFixes() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BugReport, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fix Issues", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Holographic Visualization Canvas (Shield Matrix Grid)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B16)),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.2f))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Futuristic rotating cyber grid lines inside 3D wrapper
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (is3DMode) {
                                    rotationX = 55f
                                    rotationZ = 15f
                                    cameraDistance = 10f * density
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2, size.height / 2)
                            
                            // Cyber grid circular lines
                            for (r in 50..250 step 50) {
                                drawCircle(
                                    color = CyberCyan.copy(alpha = gridPulseAlpha),
                                    radius = r.dp.toPx(),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                            
                            // Diagonal guide lines
                            val lineLength = 200.dp.toPx()
                            for (angleDeg in 0..360 step 45) {
                                val angleRad = Math.toRadians(angleDeg.toDouble())
                                val endX = center.x + lineLength * cos(angleRad).toFloat()
                                val endY = center.y + lineLength * sin(angleRad).toFloat()
                                drawLine(
                                    color = CyberCyan.copy(alpha = 0.05f),
                                    start = center,
                                    end = Offset(endX, endY),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Protection Flow connection paths
                            if (showFlowAnimation) {
                                val nodesCount = nodes.size
                                for (i in 0 until nodesCount) {
                                    val currentAngleRad = Math.toRadians(nodes[i].angle)
                                    val nextAngleRad = Math.toRadians(nodes[(i + 1) % nodesCount].angle)
                                    
                                    val startX = center.x + 100.dp.toPx() * cos(currentAngleRad).toFloat()
                                    val startY = center.y + 100.dp.toPx() * sin(currentAngleRad).toFloat()
                                    val endX = center.x + 100.dp.toPx() * cos(nextAngleRad).toFloat()
                                    val endY = center.y + 100.dp.toPx() * sin(nextAngleRad).toFloat()
                                    
                                    // Animated dashed connection lines
                                    drawLine(
                                        color = if (nodes[i].status == NodeStatus.WARNING) Color(0xFFFF5252).copy(alpha = 0.7f) else CyberCyan.copy(alpha = 0.7f),
                                        start = Offset(startX, startY),
                                        end = Offset(endX, endY),
                                        strokeWidth = 2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(
                                            intervals = floatArrayOf(20f, 10f),
                                            phase = -flowPhase * 2f
                                        )
                                    )
                                }
                            }
                        }

                        // Render central shield anchor node
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .graphicsLayer {
                                    if (is3DMode) {
                                        rotationX = -55f
                                        rotationZ = -15f
                                    }
                                }
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(RoyalGold.copy(alpha = 0.3f), Color.Transparent)
                                    ), CircleShape
                                )
                                .border(1.dp, RoyalGold.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Grid4x4,
                                contentDescription = "Shield Matrix Core",
                                tint = RoyalGold,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Render nodes individually in circular alignment
                        nodes.forEachIndexed { index, node ->
                            val angleRad = Math.toRadians(node.angle)
                            val radiusPx = 100.dp

                            val xOffset = radiusPx * cos(angleRad).toFloat()
                            val yOffset = radiusPx * sin(angleRad).toFloat()

                            val borderTint = when (node.status) {
                                NodeStatus.NOMINAL -> CyberCyan.copy(alpha = 0.5f)
                                NodeStatus.WARNING -> Color(0xFFFF5252).copy(alpha = 0.8f)
                                NodeStatus.ACTIVE_SCAN -> RoyalGold
                            }

                            val glowBg = when (node.status) {
                                NodeStatus.NOMINAL -> CyberCyan.copy(alpha = 0.1f)
                                NodeStatus.WARNING -> Color(0xFFFF5252).copy(alpha = 0.15f)
                                NodeStatus.ACTIVE_SCAN -> RoyalGold.copy(alpha = 0.25f)
                            }

                            val nodeSize = if (is3DMode) 64.dp else 48.dp
                            val shape = if (is3DMode) RoundedCornerShape(12.dp) else CircleShape

                            Box(
                                modifier = Modifier
                                    .offset(x = xOffset, y = yOffset)
                                    .size(nodeSize)
                                    .scale(if (index == activeNodeIndex) 1.2f else 1.0f)
                                    .background(glowBg, shape)
                                    .border(1.5.dp, borderTint, shape)
                                    .clip(shape)
                                    .graphicsLayer {
                                        if (is3DMode) {
                                            rotationX = -55f
                                            rotationZ = -15f
                                        }
                                    }
                                    .clickable { selectedNodeIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                val iconTint = if (node.status == NodeStatus.WARNING) Color(0xFFFF5252) else Color.White
                                val icon = getIconForName(node.name)
                                
                                if (is3DMode) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        if (icon != null) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = node.name,
                                                tint = iconTint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = node.abbreviation,
                                            color = iconTint,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            node.subNodes.forEach { sub ->
                                                val dotColor = if (sub.status == "WARNING") {
                                                    Color(0xFFFF5252)
                                                } else {
                                                    CyberCyan
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .background(dotColor, CircleShape)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    if (icon != null) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = node.name,
                                            tint = iconTint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Text(
                                            text = node.abbreviation,
                                            color = iconTint,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Floating 3D Mode Toggle
                    Button(
                        onClick = { is3DMode = !is3DMode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (is3DMode) RoyalGold else Color.White.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, if (is3DMode) RoyalGold else Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "Toggle 3D",
                            tint = if (is3DMode) Color.Black else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "3D",
                            color = if (is3DMode) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Log Console (Real-time telemetry feed)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "> ",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = currentLog,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Node Inspector Panel (Information on the selected node)
            val selectedNode = nodes.getOrNull(selectedNodeIndex)
            if (selectedNode != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10131E)),
                    border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedNode.name,
                                color = RoyalGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            val statusText = when (selectedNode.status) {
                                NodeStatus.NOMINAL -> "SECURE / NOMINAL"
                                NodeStatus.WARNING -> "ATTENTION REQUIRED"
                                NodeStatus.ACTIVE_SCAN -> "SCANNING..."
                            }
                            val statusColor = when (selectedNode.status) {
                                NodeStatus.NOMINAL -> CyberCyan
                                NodeStatus.WARNING -> Color(0xFFFF5252)
                                NodeStatus.ACTIVE_SCAN -> RoyalGold
                            }
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = selectedNode.description,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Recommend Action Block (Fix Center details)
                        if (selectedNode.status == NodeStatus.WARNING) {
                            Surface(
                                color = Color(0xFFFF5252).copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedNode.name == "VPN Shield") {
                                            "Active VPN connection recommended to prevent data interception."
                                        } else {
                                            "Grant missing permissions to allow continuous background telemetry."
                                        },
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = CyberCyan.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Node is fully operational. Sandbox integrity and security scores are nominal.",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showSubFlowDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dive into ${selectedNode.name} Flow", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Legend / Abbreviation Help Dialog
            if (showLegendDialog) {
                AlertDialog(
                    onDismissRequest = { showLegendDialog = false },
                    title = {
                        Text(
                            "Solution Engine Legend",
                            color = RoyalGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Here is the explanation of all abbreviations in the Solution Engine map:",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            
                            nodes.forEach { node ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(CyberCyan.copy(alpha = 0.1f), CircleShape)
                                            .border(1.dp, CyberCyan, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val icon = getIconForName(node.name)
                                        if (icon != null) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = node.name,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = node.abbreviation,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = node.name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = node.description,
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLegendDialog = false }) {
                            Text("CLOSE", color = RoyalGold, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = Color(0xFF0F101A),
                    tonalElevation = 6.dp
                )
            }

            // Detailed Sub-Flow Dialog (Layer 2)
            if (showSubFlowDialog && selectedNode != null) {
                Dialog(
                    onDismissRequest = { showSubFlowDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF02040A)
                    ) {
                        // Sub-flow animations
                        val subTransition = rememberInfiniteTransition(label = "sub_flow_inf")
                        val subFlowPhase by subTransition.animateFloat(
                            initialValue = 0f, targetValue = 100f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ), label = "subFlowPhase"
                        )
                        val subZRotation by subTransition.animateFloat(
                            initialValue = 0f, targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(12000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ), label = "subZRotation"
                        )
                        
                        var selectedSubNodeIndex by remember { mutableIntStateOf(0) }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showSubFlowDialog = false },
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "${selectedNode.name.uppercase()} SUB-SYSTEM",
                                        color = RoyalGold,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Layer 2: Detailed internal processes and micro-telemetry nodes.",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Detailed flow chart box
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B16)),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            rotationX = 60f
                                            cameraDistance = 8f * density
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Drawing connecting lines to sub-nodes in 3D projection
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val center = Offset(size.width / 2, size.height / 2)
                                        
                                        // Draw orbiting ellipsis (representing the 3D plane)
                                        drawOval(
                                            color = CyberCyan.copy(alpha = 0.15f),
                                            topLeft = Offset(center.x - 100.dp.toPx(), center.y - 50.dp.toPx()),
                                            size = androidx.compose.ui.geometry.Size(200.dp.toPx(), 100.dp.toPx()),
                                            style = Stroke(
                                                width = 1.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                                            )
                                        )
                                        
                                        val subNodesCount = selectedNode.subNodes.size
                                        for (i in 0 until subNodesCount) {
                                            val angleDeg = (i * (360.0 / subNodesCount)) + subZRotation
                                            val angleRad = Math.toRadians(angleDeg)
                                            val endX = center.x + 100.dp.toPx() * cos(angleRad).toFloat()
                                            val endY = center.y + 100.dp.toPx() * sin(angleRad).toFloat() * 0.5f // Y compressed by tilt
                                            
                                            drawLine(
                                                color = CyberCyan.copy(alpha = 0.4f),
                                                start = center,
                                                end = Offset(endX, endY),
                                                strokeWidth = 1.5.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(
                                                    intervals = floatArrayOf(15f, 10f),
                                                    phase = -subFlowPhase * 2f
                                                )
                                            )
                                        }
                                    }
                                    
                                    // Center Parent Node with counter-tilt
                                    Box(
                                        modifier = Modifier
                                            .graphicsLayer {
                                                rotationX = -60f
                                            }
                                            .size(64.dp)
                                            .background(CyberCyan.copy(alpha = 0.15f), CircleShape)
                                            .border(1.5.dp, CyberCyan, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val icon = getIconForName(selectedNode.name)
                                        if (icon != null) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = selectedNode.name,
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        } else {
                                            Text(
                                                text = selectedNode.abbreviation,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    // Orbiting Sub-nodes with depth cues and counter-tilt
                                    selectedNode.subNodes.forEachIndexed { i, subNode ->
                                        val angleDeg = (i * (360.0 / selectedNode.subNodes.size)) + subZRotation
                                        val angleRad = Math.toRadians(angleDeg)
                                        val radius = 100.dp
                                        val cosVal = cos(angleRad).toFloat()
                                        val sinVal = sin(angleRad).toFloat()
                                        val xOffset = radius * cosVal
                                        val yOffset = radius * sinVal * 0.5f // compressed Y coordinates
                                        
                                        // Depth scaling cues
                                        val depthFactor = sinVal // goes from -1 (back) to 1 (front)
                                        val scale = 0.85f + (depthFactor + 1f) * 0.15f
                                        val alpha = 0.5f + (depthFactor + 1f) * 0.25f
                                        
                                        val isSelected = i == selectedSubNodeIndex
                                        val subBorderTint = if (isSelected) RoyalGold else Color.White.copy(alpha = 0.4f)
                                        val subBg = if (isSelected) RoyalGold.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
                                        
                                        Box(
                                            modifier = Modifier
                                                .offset(x = xOffset, y = yOffset)
                                                .graphicsLayer {
                                                    rotationX = -60f // counter-tilt to remain upright
                                                    scaleX = scale
                                                    scaleY = scale
                                                    this.alpha = alpha
                                                }
                                                .size(38.dp)
                                                .background(subBg, CircleShape)
                                                .border(1.dp, subBorderTint, CircleShape)
                                                .clip(CircleShape)
                                                .clickable { selectedSubNodeIndex = i },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val subIcon = getIconForName(subNode.name)
                                            if (subIcon != null) {
                                                Icon(
                                                    imageVector = subIcon,
                                                    contentDescription = subNode.name,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = subNode.name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase(),
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Selected Sub-node detailed inspector
                            val selectedSubNode = selectedNode.subNodes.getOrNull(selectedSubNodeIndex)
                            if (selectedSubNode != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10131E)),
                                    border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = selectedSubNode.name,
                                            color = RoyalGold,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = selectedSubNode.description,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            lineHeight = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color.Green, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "STATUS: ${selectedSubNode.status}",
                                                color = Color.Green,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Telemetry simulation log in detail
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Text(
                                    text = "Sub-flow analyzer online. Direct tunnel connection nominal. Encrypting telemetry streams...",
                                    color = Color.Green.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
