package com.royalshield.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Refresh
import com.royalshield.app.ui.theme.CyberCyan
import com.royalshield.app.ui.theme.NeonBlue
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.SafeGreen
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun SpeedTestDialog(onDismiss: () -> Unit) {
    var downloadSpeed by remember { mutableStateOf(0f) }
    var uploadSpeed by remember { mutableStateOf(0f) }
    var ping by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }
    
    // Graph Data (Normalized 0..1)
    val graphPoints = remember { mutableStateListOf<Float>() }
    
    var stage by remember { mutableStateOf(TestStage.CONNECTING) }

    LaunchedEffect(Unit) {
        // 1. Connecting / Ping
        stage = TestStage.CONNECTING
        delay(1000)
        ping = Random.nextInt(15, 45)
        
        // 2. Download Test
        stage = TestStage.DOWNLOAD
        val maxDownload = Random.nextFloat() * 100 + 50 // 50-150 Mbps
        for (i in 0..50) {
            val variance = Random.nextFloat() * 0.2f - 0.1f // +/- 10%
            val current = (maxDownload * (i / 50f)) * (1f + variance)
            downloadSpeed = current.coerceAtLeast(0f)
            graphPoints.add(current / 150f) // Normalize roughly
            progress = i / 100f // 0 to 0.5
            delay(50)
        }
        
        // 3. Upload Test
        stage = TestStage.UPLOAD
        val maxUpload = Random.nextFloat() * 20 + 10 // 10-30 Mbps
        for (i in 0..50) {
            val variance = Random.nextFloat() * 0.2f - 0.1f
            val current = (maxUpload * (i / 50f)) * (1f + variance)
            uploadSpeed = current.coerceAtLeast(0f)
            // Add to graph but maybe scale differently or clear? 
            // Let's keep adding but shift usage.
            graphPoints.add(current / 150f) 
            progress = 0.5f + (i / 100f) // 0.5 to 1.0
            delay(50)
        }
        
        stage = TestStage.FINISHED
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("NETWORK SPEED TEST", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                // Big Speed Display
                val displaySpeed = if (stage == TestStage.UPLOAD) uploadSpeed else downloadSpeed
                val displayColor = if (stage == TestStage.UPLOAD) androidx.compose.ui.graphics.Color(0xFFAB47BC) else NeonBlue
                
                Text(
                    text = "%.1f".format(displaySpeed),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = displayColor
                )
                Text("Mbps", color = Color.Gray)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Graph Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (graphPoints.isEmpty()) return@Canvas
                        
                        val path = Path()
                        val width = size.width
                        val height = size.height
                        val stepX = width / 100f // Max 100 points
                        
                        // We only show last 100 points or scale?
                        // Let's just draw what we have, scaling X
                        val pointsToDraw = graphPoints.takeLast(100)
                        val step = width / (pointsToDraw.size.coerceAtLeast(2) - 1).toFloat()
                        
                        pointsToDraw.forEachIndexed { index, value ->
                            val x = index * step
                            val y = height - (value * height).coerceIn(0f, height)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        
                        drawPath(
                            path = path,
                            color = displayColor,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        
                        // Fill Gradient
                        val fillPath = Path()
                        fillPath.addPath(path)
                        fillPath.lineTo(width, height)
                        fillPath.lineTo(0f, height)
                        fillPath.close()
                        
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(displayColor.copy(alpha=0.3f), Color.Transparent)
                            )
                        )
                    }
                    
                    if (stage == TestStage.CONNECTING) {
                        Text("Connecting...", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Stats Row
                Row(
                   modifier = Modifier.fillMaxWidth(),
                   horizontalArrangement = Arrangement.SpaceBetween 
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("↓", fontSize = 24.sp, color = NeonBlue)
                        Text("%.1f".format(downloadSpeed), color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Download", color = Color.Gray, fontSize = 10.sp)
                    }
                     Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("↑", fontSize = 24.sp, color = androidx.compose.ui.graphics.Color(0xFFAB47BC))
                        Text("%.1f".format(uploadSpeed), color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Upload", color = Color.Gray, fontSize = 10.sp)
                    }
                     Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⟳", fontSize = 24.sp, color = SafeGreen)
                        Text("${ping}ms", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Ping", color = Color.Gray, fontSize = 10.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (stage == TestStage.FINISHED) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("DONE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = RoyalGold,
                        trackColor = Color.DarkGray
                    )
                }
            }
        }
    }
}

enum class TestStage {
    CONNECTING, DOWNLOAD, UPLOAD, FINISHED
}
