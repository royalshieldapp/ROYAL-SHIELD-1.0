package com.royalshield.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.theme.CyberCyan
import com.royalshield.app.ui.theme.NeonBlue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CyberLoadingIndicator(
    modifier: Modifier = Modifier,
    text: String = "Loading...",
    size: androidx.compose.ui.unit.Dp = 200.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_scan")

    // Rotation for outer thick arc
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )

    // Reverse rotation for inner dotted ring
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rotation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2, size.toPx() / 2)
            val radius = size.toPx() / 2

            // 1. Outer track background (faint)
            drawCircle(
                color = Color.Black,
                radius = radius - 10f,
                center = center,
                style = Stroke(width = 20f)
            )

            // 2. Outer glowing arc
            val sweepAngle = 140f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        CyberCyan.copy(alpha = 0f),
                        CyberCyan.copy(alpha = 0.5f),
                        CyberCyan,
                        Color.White
                    ),
                    center = center
                ),
                startAngle = outerRotation,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(10f, 10f),
                size = Size(size.toPx() - 20f, size.toPx() - 20f),
                style = Stroke(width = 20f, cap = StrokeCap.Round)
            )

            // 3. Middle transparent ring
            drawCircle(
                color = NeonBlue.copy(alpha = 0.15f),
                radius = radius - 40f,
                center = center,
                style = Stroke(width = 2f)
            )

            // 4. Inner dotted ring
            val innerRadius = radius - 60f
            val dotCount = 40
            val angleStep = 360f / dotCount

            for (i in 0 until dotCount) {
                // Determine if this segment should be bright based on rotation
                val currentAngle = (i * angleStep + innerRotation) % 360
                val alpha = if (currentAngle in 0f..90f) 1f else 0.3f
                val color = CyberCyan.copy(alpha = alpha)

                val angleRad = Math.toRadians((i * angleStep + innerRotation).toDouble())
                val startX = center.x + (innerRadius - 5f) * cos(angleRad).toFloat()
                val startY = center.y + (innerRadius - 5f) * sin(angleRad).toFloat()
                val endX = center.x + (innerRadius + 5f) * cos(angleRad).toFloat()
                val endY = center.y + (innerRadius + 5f) * sin(angleRad).toFloat()

                drawLine(
                    color = color,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 6f,
                    cap = StrokeCap.Square
                )
            }
        }

        // Center Text
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 1.sp
        )
    }
}
