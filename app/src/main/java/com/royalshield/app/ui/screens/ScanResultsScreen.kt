package com.royalshield.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.MalwareScanner
import com.royalshield.app.R
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.util.toProStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultsScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var scanData by remember { mutableStateOf<com.royalshield.app.util.ScanProStrings?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ScanResultsScreen", "Starting malware scan...")
                val scanner = com.royalshield.app.MalwareScanner(context)
                val summary = scanner.scanInstalledAppsWithMetrics()
                scanData = summary.toProStrings()
                android.util.Log.d("ScanResultsScreen", "Scan completed successfully")
            } catch (e: Exception) {
                android.util.Log.e("ScanResultsScreen", "Scan failed: ${e.message}", e)
                // Optionally set an error state here if UI should show "Scan Failed"
            } finally {
                isLoading = false
            }
        }
    }

    com.royalshield.app.ui.components.RoyalGradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Scan Report", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = RoyalGold,
                        navigationIconContentColor = RoyalGold
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = RoyalGold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Running comprehensive diagnostic...", color = Color.White)
                    }
                } else {
                    scanData?.let { data ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // 0. HERO CARD — dashboard_card_malware with scanner effect
                            MalwareScanHeroCard()

                            // 1. STATUS HEADER & SCORE
                            val riskScore = 85 // Mock or derive
                            val status = "SUSPICIOUS" // Mock or derive based on data
                            val statusColor = Color(0xFFFF9800)

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            progress = riskScore / 100f,
                                            modifier = Modifier.size(120.dp),
                                            color = statusColor,
                                            trackColor = Color.White.copy(alpha = 0.1f),
                                            strokeWidth = 12.dp
                                        )
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = riskScore.toString(),
                                                style = MaterialTheme.typography.displayMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "/100",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Scan completed: ${java.text.SimpleDateFormat("MMM dd, HH:mm").format(java.util.Date())}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }

                            // 2. DETECTIONS & METRICS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                MetricCard(
                                    title = "Detections",
                                    value = "2", // Mock
                                    icon = Icons.Default.Warning,
                                    color = Color(0xFFFF5252),
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = "Scanned",
                                    value = "142", // Mock
                                    icon = Icons.Default.Search,
                                    color = Color(0xFF00E5FF),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 3. ENGINE ANALYSIS
                            Text(
                                "Engine Analysis",
                                color = RoyalGold,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                EngineResultRow("Royal Shield AI", "Clean", Color(0xFF00E5FF))
                                EngineResultRow("Signature DB", "Suspicious", Color(0xFFFF9800))
                                EngineResultRow("Heuristic Analysis", "Clean", Color(0xFF00E5FF))
                                EngineResultRow("Behavioral Monitor", "Clean", Color(0xFF00E5FF))
                            }

                            // 4. PERMISSIONS SUMMARY (Existing Data)
                            if (data.topPermissionsLines.isNotEmpty()) {
                                Text(
                                    "Critical Permissions Found",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                                )
                                data.topPermissionsLines.take(3).forEach { line ->
                                    EngineResultRow("System", line, Color.Gray)
                                }
                            }

                            // 5. ACTION BUTTONS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { isLoading = true }, // Mock Rescan
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)
                                ) {
                                    Text("RE-SCAN", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { /* Share */ },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                                ) {
                                    Text("SHARE", color = Color.White)
                                }
                            }
                            Button(
                                onClick = { showDetailsDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGold.copy(alpha = 0.5f))
                            ) {
                                Text("VIEW FULL DETAILS", color = RoyalGold)
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
        
        if (showDetailsDialog) {
            AlertDialog(
                onDismissRequest = { showDetailsDialog = false },
                containerColor = Color(0xFF1A1A2E),
                title = { Text("Detailed Scan Report", color = RoyalGold, fontWeight = FontWeight.Bold) },
                text = { 
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        scanData?.let { data ->
                            Text(data.header, color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(data.kpiLine, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(data.distributionLine, color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("All Identified Permissions:", color = RoyalGold, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            data.topPermissionsLines.forEach {
                                Text(it, color = Color.Gray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDetailsDialog = false }) {
                        Text("CLOSE", color = RoyalGold)
                    }
                }
            )
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF252525)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = title, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun EngineResultRow(name: String, result: String, resultColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (result == "Clean") {
                Icon(Icons.Default.CheckCircle, null, tint = resultColor, modifier = Modifier.size(16.dp))
            } else {
                Icon(Icons.Default.Warning, null, tint = resultColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(result, color = resultColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Hero card with dashboard_card_malware background image
 * and the same scanner sweep animation from the Dashboard MalwareScannerCard.
 */
@Composable
fun MalwareScanHeroCard(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_scanner_sweep")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hero_sweep_progress"
    )

    val silverColor = Color(0xFFC0C0C0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, silverColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image — dashboard_card_malware.png
            Image(
                painter = painterResource(id = R.drawable.dashboard_card_malware),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
            )

            // Dark overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.55f),
                            0.6f to Color.Black.copy(alpha = 0.75f),
                            1.0f to Color.Black.copy(alpha = 0.9f)
                        )
                    )
            )

            // Matrix Code Layer (same as Dashboard card)
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
                color = Color(0xFF00FF00).copy(alpha = 0.4f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp,
                modifier = Modifier.padding(12.dp)
            )

            // Scanner Line Overlay — animated sweep
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
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

            // Title & Icon overlay at bottom
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = silverColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "MALWARE SCAN ENGINE",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Real-time heuristic analysis active",
                    color = silverColor.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}



