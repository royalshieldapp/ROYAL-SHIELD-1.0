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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.graphics.drawable.toBitmap
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
    var isLoading by remember { mutableStateOf(true) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var currentAppName by remember { mutableStateOf("Initializing privacy scan...") }

    LaunchedEffect(Unit) {
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
                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Premium pulsing/rotating scanning effect
                        Box(
                            modifier = Modifier.size(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "scanner")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "rotation"
                            )
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 0.85f,
                                targetValue = 1.05f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseScale"
                            )
                            
                            // Outer pulsing glow
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .scale(pulseScale)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                RoyalGold.copy(alpha = 0.2f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = RoundedCornerShape(100.dp)
                                    )
                            )
                            
                            // Rotating radar sweep
                            Canvas(modifier = Modifier.size(160.dp)) {
                                drawCircle(
                                    color = RoyalGold.copy(alpha = 0.1f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            RoyalGold.copy(alpha = 0.8f),
                                            RoyalGold.copy(alpha = 0.0f)
                                        )
                                    ),
                                    startAngle = rotation,
                                    sweepAngle = 90f,
                                    useCenter = true
                                )
                            }
                            
                            // Center Icon & Text
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = RoyalGold,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${scanProgress.toInt()}%",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Golden Segmented Progress Bar (matches Security Scan style)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222222))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(scanProgress / 100f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF8B6914),
                                                RoyalGold,
                                                Color(0xFFFFD700),
                                                RoyalGold
                                            )
                                        )
                                    )
                            )
                            
                            // Segment dividers
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                repeat(12) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .fillMaxHeight()
                                            .background(Color(0xFF0A0A0A))
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Status info text
                        Text(
                            text = currentAppName,
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Analyzing app permissions for privacy risks...",
                            color = Color.Gray,
                            fontSize = 12.sp
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
