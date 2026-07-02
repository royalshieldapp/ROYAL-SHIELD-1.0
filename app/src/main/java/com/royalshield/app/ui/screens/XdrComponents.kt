package com.royalshield.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.royalshield.app.ui.components.PremiumGlassCard
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun XdrHeader(onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text("XDR Security Center", color = Color.White)
    }
}

@Composable
fun XdrSidebarIcon(
    icon: ImageVector, 
    isSelected: Boolean = false, 
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(icon, null, tint = if (isSelected) Color.Cyan else Color.Gray)
    }
}

@Composable
fun XdrStatCard(
    title: String, 
    value: String, 
    color: Color, 
    subValue: String = "", 
    trend: String = "", 
    modifier: Modifier = Modifier
) {
    PremiumGlassCard(modifier = modifier) {
         Column(modifier = Modifier.padding(8.dp)) {
             Text(title, color = Color.Gray)
             Text(value, color = color)
             if(subValue.isNotEmpty()) Text(subValue, color = Color.Gray)
         }
    }
}

@Composable
fun XdrNetworkGraph(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "NetworkGraph")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "rotation"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 3
            
            // Draw connections
            rotate(rotation) {
                val nodes = 8
                val points = List(nodes) { i ->
                    val angle = (i * 360f / nodes).toDouble()
                    Offset(
                        (center.x + radius * cos(Math.toRadians(angle))).toFloat(),
                        (center.y + radius * sin(Math.toRadians(angle))).toFloat()
                    )
                }
                
                points.forEachIndexed { i, p1 ->
                    // Lines to neighbors
                    val next = points[(i + 1) % nodes]
                    val cross = points[(i + 3) % nodes]
                    
                    drawLine(
                        color = Color.Cyan.copy(alpha = 0.2f),
                        start = p1,
                        end = next,
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.Cyan.copy(alpha = 0.1f),
                        start = p1,
                        end = cross,
                        strokeWidth = 1f
                    )
                    
                    // Node dot
                    drawCircle(
                        color = Color.Cyan,
                        radius = 4.dp.toPx(),
                        center = p1
                    )
                    
                    // Pulse effect on nodes
                    drawCircle(
                        color = Color.Cyan.copy(alpha = 0.3f),
                        radius = 8.dp.toPx(),
                        center = p1,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            
            // Central Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Cyan.copy(alpha = 0.2f), Color.Transparent)
                ),
                radius = radius * 1.5f,
                center = center
            )
            
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = center
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SECURE NETWORK MESH", color = Color.Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("ACTIVE NODES: 8", color = Color.Gray, fontSize = 9.sp)
        }
    }
}
