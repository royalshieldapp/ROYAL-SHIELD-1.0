package com.royalshield.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.res.painterResource
import com.royalshield.app.managers.PrivacyAdvisorManager
import com.royalshield.app.managers.PrivacyApp
import com.royalshield.app.managers.RiskLevel
import com.royalshield.app.ui.components.RoyalGradientBackground
import com.royalshield.app.ui.theme.NeonRed
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.Royal_shieldTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrivacyAdvisorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Royal_shieldTheme {
                PrivacyAdvisorScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyAdvisorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<PrivacyApp>>(emptyList()) }
    var hasStartedScan by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var currentAppName by remember { mutableStateOf("Ready to start privacy scan...") }

    LaunchedEffect(hasStartedScan) {
        if (!hasStartedScan) return@LaunchedEffect
        isLoading = true
        scanProgress = 0f
        currentAppName = "Initializing privacy scan..."
        withContext(Dispatchers.IO) {
            apps = PrivacyAdvisorManager.scanApps(context) { progress, appName ->
                withContext(Dispatchers.Main) {
                    scanProgress = progress
                    currentAppName = appName
                }
            }
            isLoading = false
        }
    }

    val typography = MaterialTheme.typography

    RoyalGradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Privacy Advisor", 
                            color = Color.White, 
                            style = typography.headlineSmall, // Orbitron from Type.kt
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (!hasStartedScan) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGold.copy(alpha = 0.28f))
                        ) {
                            PrivacyAdvisorScanVisual(
                                progress = 0f,
                                statusText = "Ready for privacy inspection",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { hasStartedScan = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Scan",
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                } else if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PrivacyAdvisorScanVisual(
                            progress = scanProgress,
                            statusText = currentAppName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(560.dp)
                        )
                    }
                } else {
                    LazyColumn(

                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            // Header Summary
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGold.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Security, 
                                        null, 
                                        tint = RoyalGold, 
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Column {
                                        Text(
                                            "${apps.size} Apps Analyzed", 
                                            color = Color.White, 
                                            style = typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${apps.count { it.riskLevel == RiskLevel.HIGH }} High Risk Detected",
                                            color = NeonRed,
                                            style = typography.bodySmall,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                "APP PERMISSIONS AUDIT",
                                color = RoyalGold,
                                style = typography.labelSmall,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(apps) { app ->
                            PrivacyAppCard(app)
                        }
                        
                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyAdvisorScanVisual(
    progress: Float,
    statusText: String,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 100f)
    val infiniteTransition = rememberInfiniteTransition(label = "privacy_gold_visual")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "privacyRingRotation"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "privacyGlow"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF070708))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val points = listOf(
                Offset(size.width * 0.12f, size.height * 0.15f),
                Offset(size.width * 0.28f, size.height * 0.24f),
                Offset(size.width * 0.22f, size.height * 0.37f),
                Offset(size.width * 0.06f, size.height * 0.50f),
                Offset(size.width * 0.74f, size.height * 0.08f),
                Offset(size.width * 0.88f, size.height * 0.20f),
                Offset(size.width * 0.80f, size.height * 0.36f),
                Offset(size.width * 0.94f, size.height * 0.52f),
                Offset(size.width * 0.62f, size.height * 0.68f),
                Offset(size.width * 0.45f, size.height * 0.58f),
                Offset(size.width * 0.30f, size.height * 0.74f),
                Offset(size.width * 0.78f, size.height * 0.86f),
                Offset(size.width * 0.92f, size.height * 0.78f)
            )
            val connections = listOf(
                0 to 1, 1 to 2, 2 to 3, 4 to 5, 5 to 6, 6 to 7,
                8 to 9, 9 to 10, 11 to 12, 4 to 6, 8 to 11
            )
            connections.forEach { (start, end) ->
                drawLine(
                    color = RoyalGold.copy(alpha = 0.12f),
                    start = points[start],
                    end = points[end],
                    strokeWidth = 1.dp.toPx()
                )
            }
            points.forEachIndexed { index, point ->
                drawCircle(
                    color = RoyalGold.copy(alpha = if (index % 3 == 0) 0.92f else 0.42f),
                    radius = if (index % 3 == 0) 3.3.dp.toPx() else 2.dp.toPx(),
                    center = point
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(210.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                RoyalGold.copy(alpha = glowAlpha),
                                RoyalGold.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.48f
                        )
                    )
                    drawCircle(
                        color = RoyalGold.copy(alpha = 0.75f),
                        radius = size.minDimension * 0.43f,
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                    drawCircle(
                        color = RoyalGold.copy(alpha = 0.38f),
                        radius = size.minDimension * 0.34f,
                        style = Stroke(
                            width = 1.8.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(14f, 14f),
                                -rotation
                            )
                        )
                    )
                    drawArc(
                        color = RoyalGold,
                        startAngle = rotation,
                        sweepAngle = 96f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = RoyalGold,
                    modifier = Modifier.size(92.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 48.dp, y = 42.dp)
                        .size(52.dp)
                        .background(Color(0xFFFFD400), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "R O Y A L   S H I E L D",
                color = Color(0xFFFFD400),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "C H E C K I N G   F I R E W A L L . . .",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((clampedProgress / 100f).coerceAtLeast(0.08f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFFEFA3), Color(0xFFFFD400), RoyalGold)
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "SCANNING SYSTEM: ${clampedProgress.toInt()}%",
                color = Color(0xFFFFD400),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = statusText,
                color = Color.White.copy(alpha = 0.64f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PrivacyAppCard(app: PrivacyApp) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                app.icon?.let {
                    Image(
                        bitmap = it.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                } ?: Icon(Icons.Default.Warning, null, tint = Color.Gray, modifier = Modifier.size(48.dp))

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(app.appName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(app.packageName, color = Color.Gray, fontSize = 10.sp)
                }

                // Risk Badge
                val (riskColor, riskText) = when (app.riskLevel) {
                    RiskLevel.HIGH -> NeonRed to "HIGH RISK"
                    RiskLevel.MEDIUM -> RoyalGold to "MEDIUM"
                    RiskLevel.LOW -> Color.Green to "SAFE"
                }

                Surface(
                    color = riskColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = riskText,
                        color = riskColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            Text("Permissions:", color = Color.Gray, fontSize = 12.sp)
            Row(modifier = Modifier.wrapContentSize()) {
                 app.permissions.take(3).forEach { perm ->
                     Text("• $perm  ", color = Color.LightGray, fontSize = 11.sp)
                 }
                 if(app.permissions.size > 3) Text("+${app.permissions.size - 3} more", color = RoyalGold, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalGold),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("MANAGE", fontSize = 12.sp)
                }
            }
        }
    }
}
