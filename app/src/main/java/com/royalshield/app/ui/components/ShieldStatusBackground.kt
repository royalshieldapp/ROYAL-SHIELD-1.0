package com.royalshield.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ShieldStatusBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "hologram")
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(modifier = modifier.background(Color(0xFF0A0A14))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Draw hexagonal grid
            val hexSize = 40.dp.toPx()
            val hexWidth = hexSize * 1.732f
            val hexHeight = hexSize * 2f
            
            for (y in -1..(canvasHeight / (hexHeight * 0.75f)).toInt() + 1) {
                for (x in -1..(canvasWidth / hexWidth).toInt() + 1) {
                    val offsetX = if (y % 2 == 0) 0f else hexWidth / 2f
                    val centerX = x * hexWidth + offsetX
                    val centerY = y * hexHeight * 0.75f
                    
                    val path = Path().apply {
                        for (i in 0..5) {
                            val angle = Math.toRadians(60.0 * i - 30.0)
                            val px = centerX + hexSize * cos(angle).toFloat()
                            val py = centerY + hexSize * sin(angle).toFloat()
                            if (i == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        close()
                    }
                    
                    drawPath(
                        path = path,
                        color = Color(0xFF00E5FF).copy(alpha = 0.05f * pulse),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            
            // Draw glowing scan line
            val scanLinePos = (pulse * canvasHeight)
            drawLine(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xFF00E5FF).copy(alpha = 0.5f), Color.Transparent),
                    startY = scanLinePos - 20.dp.toPx(),
                    endY = scanLinePos + 20.dp.toPx()
                ),
                start = Offset(0f, scanLinePos),
                end = Offset(canvasWidth, scanLinePos),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
