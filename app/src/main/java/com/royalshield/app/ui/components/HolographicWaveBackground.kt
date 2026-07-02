package com.royalshield.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun HolographicWaveBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "holographicWave")

    // Phase shift animation for movement
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier
            .background(Color.Black) // Deep dark background
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Draw multiple waves
            drawHolographicWave(
                phase = phase,
                amplitude = height * 0.15f,
                frequency = 1.5f,
                yOffset = height * 0.4f,
                color = Color(0xFF00E5FF).copy(alpha = 0.6f), // Cyan
                strokeWidth = 8f
            )
            
            drawHolographicWave(
                phase = phase + 1.5f,
                amplitude = height * 0.12f,
                frequency = 1.2f,
                yOffset = height * 0.5f,
                color = Color(0xFF8B5CF6).copy(alpha = 0.6f), // Purple
                strokeWidth = 6f
            )

            drawHolographicWave(
                phase = phase + 3f,
                amplitude = height * 0.18f,
                frequency = 0.8f,
                yOffset = height * 0.6f,
                color = Color(0xFFFF00FF).copy(alpha = 0.5f), // Magenta
                strokeWidth = 10f
            )
            
            // Subtle glow overlay
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.2f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.6f)
                    )
                )
            )
        }
    }
}

fun DrawScope.drawHolographicWave(
    phase: Float,
    amplitude: Float,
    frequency: Float,
    yOffset: Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path()
    val halfHeight = size.height / 2
    
    path.moveTo(0f, yOffset + amplitude * sin(phase))

    for (x in 0..size.width.toInt() step 10) {
        val xFloat = x.toFloat()
        val normalizedX = xFloat / size.width
        val y = yOffset + amplitude * sin(phase + normalizedX * 2 * PI.toFloat() * frequency)
        path.lineTo(xFloat, y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth)
    )
    
    // Fill subtle gradient below wave for "liquid" look
    val fillPath = Path()
    fillPath.addPath(path)
    fillPath.lineTo(size.width, size.height)
    fillPath.lineTo(0f, size.height)
    fillPath.close()
    
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = 0.1f),
                Color.Transparent
            ),
            startY = yOffset,
            endY = size.height
        )
    )
}
