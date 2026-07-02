package com.royalshield.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LoadingDotsRing(
    modifier: Modifier = Modifier,
    color: Color = Color.Green,
    dotCount: Int = 24,
    dotRadius: Dp = 4.dp,
    isAnimating: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "DotsRing")
    
    // Rotation animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    // Opacity pulse
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = (size.minDimension - dotRadius.toPx()) / 2
        val currentRotation = if (isAnimating) rotation else 0f

        for (i in 0 until dotCount) {
            // Calculate angle for each dot
            val angle = (i * (360f / dotCount)) + currentRotation
            val angleRad = Math.toRadians(angle.toDouble())

            // Dot position
            val cx = center.x + radius * cos(angleRad).toFloat()
            val cy = center.y + radius * sin(angleRad).toFloat()

            // Dynamic size and alpha based on position in "wave" if desired, 
            // or just simple pulse for the whole ring as requested "effect".
            // The image shows glowing dots.
            
            // Let's make dots trail or pulse
            val dotAlpha = if (isAnimating) alpha else 1f
            
            drawCircle(
                color = color.copy(alpha = dotAlpha),
                radius = dotRadius.toPx(),
                center = Offset(cx, cy)
            )
            
            // Add a "glow" to the dot
            drawCircle(
                color = color.copy(alpha = dotAlpha * 0.3f),
                radius = dotRadius.toPx() * 2f,
                center = Offset(cx, cy)
            )
        }
    }
}
