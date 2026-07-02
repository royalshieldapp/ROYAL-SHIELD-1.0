package com.royalshield.app.ui.dashboard.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.dashboard.models.ThreatEvent
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarChart(
    threats: List<ThreatEvent>,
    selectedThreatId: String?,
    onThreatClick: (ThreatEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val gridColor = Color.White.copy(alpha = 0.1f)
    val sweepColor = Color(0xFF6200EA).copy(alpha = 0.3f)
    val crossHairColor = Color.White.copy(alpha = 0.2f)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(threats) {
                    detectTapGestures { tapOffset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = minOf(size.width, size.height) / 2f
                        
                        // Hit testing
                        threats.forEach { threat ->
                            val r = threat.radius * maxRadius
                            val a = Math.toRadians(threat.angleDeg.toDouble())
                            val x = center.x + r * cos(a)
                            val y = center.y + r * sin(a)
                            
                            val dist = (tapOffset - Offset(x.toFloat(), y.toFloat())).getDistance()
                            if (dist < 30f) {
                                onThreatClick(threat)
                                return@detectTapGestures
                            }
                        }
                    }
                }
        ) {
            val center = center
            val maxRadius = minOf(size.width, size.height) / 2
            
            // Draw Grid
            val steps = 4
            for (i in 1..steps) {
                drawCircle(
                    color = gridColor,
                    radius = maxRadius * (i / steps.toFloat()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Draw Crosshair
            drawLine(
                color = crossHairColor,
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = crossHairColor,
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )

            // Draw Pulse at Center
            drawCircle(
                color = Color(0xFF6200EA).copy(alpha = pulseAlpha),
                radius = (maxRadius * 0.2f) * pulseScale,
                center = center
            )

            // Draw Sweep
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.8f to Color.Transparent,
                    1.0f to sweepColor,
                    center = center
                ),
                startAngle = sweepAngle - 90f,
                sweepAngle = 60f,
                useCenter = true,
                style = androidx.compose.ui.graphics.drawscope.Fill
            )
            
            // Draw Threats
            threats.forEach { threat ->
                val r = threat.radius * maxRadius
                val a = Math.toRadians(threat.angleDeg.toDouble())
                
                val x = center.x + (r * cos(a)).toFloat()
                val y = center.y + (r * sin(a)).toFloat()
                
                // USE COLOR DIRECTLY
                val color = threat.severity.color
                val isSelected = threat.id == selectedThreatId
                
                val radiusBase = if (isSelected) 12.dp.toPx() else 6.dp.toPx()
                
                drawCircle(
                    color = color.copy(alpha = 0.8f),
                    radius = radiusBase,
                    center = Offset(x, y)
                )
                
                if (isSelected) {
                    drawCircle(
                        color = color.copy(alpha = 0.3f),
                        radius = radiusBase * 1.5f,
                        center = Offset(x, y)
                    )
                }
            }
        }
        
        if (selectedThreatId != null) {
            val selectedThreat = threats.find { it.id == selectedThreatId }
            if (selectedThreat != null) {
                ThreatTooltip(
                    threat = selectedThreat,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun ThreatTooltip(threat: ThreatEvent, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .background(Color(0xFF1E1E24).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = threat.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Severity: ${threat.severity.label}",
                color = threat.severity.color, // Direct usage
                fontSize = 12.sp
            )
        }
    }
}
