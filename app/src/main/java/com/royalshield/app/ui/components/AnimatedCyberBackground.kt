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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Cyber Colors
private val DeepCyberBlue = Color(0xFF030b18) // Very dark blue/black
private val CyberNeonBlue = Color(0xFF00D4FF)
private val CyberGold = Color(0xFFFFD700)
private val CyberPurple = Color(0xFF9D00FF)

@Composable
fun AnimatedCyberBackground(
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    
    // State to hold Prisms
    var prisms by remember {
        mutableStateOf(generatePrisms(15))
    }

    // Animation Loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // ~60 FPS
            
            prisms = prisms.map { prism ->
                // Update position
                var newX = prism.x + prism.vx
                var newY = prism.y + prism.vy
                var newRotation = prism.rotation + prism.vRotation
                
                // Wrap around logic (0-1000 coordinate space)
                if (newX < -100) newX = 1100f
                if (newX > 1100) newX = -100f
                if (newY < -100) newY = 2100f
                if (newY > 2100) newY = -100f

                prism.copy(x = newX, y = newY, rotation = newRotation)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DeepCyberBlue,
                        Color(0xFF000000)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            prisms.forEach { prism ->
                val x = (prism.x / 1000f) * width
                val y = (prism.y / 2000f) * height
                val sizePx = prism.size * density

                // Create a Triangle Path
                val path = Path().apply {
                    val halfSize = sizePx / 2
                    moveTo(0f, -halfSize) // Top
                    lineTo(halfSize, halfSize) // Bottom Right
                    lineTo(-halfSize, halfSize) // Bottom Left
                    close()
                }

                val color = when(prism.type) {
                    0 -> CyberNeonBlue
                    1 -> CyberGold
                    else -> CyberPurple
                }

                translate(left = x, top = y) {
                    rotate(degrees = prism.rotation) {
                        // Draw glow/stroke
                        drawPath(
                            path = path,
                            color = color.copy(alpha = 0.3f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        // Fill with low alpha
                        drawPath(
                            path = path,
                            color = color.copy(alpha = 0.05f)
                        )
                    }
                }
            }
        }
    }
}

data class Prism(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val vRotation: Float,
    val size: Float,
    val type: Int // 0: Blue, 1: Gold, 2: Purple
)

fun generatePrisms(count: Int): List<Prism> {
    return List(count) {
        Prism(
            x = Random.nextFloat() * 1000f,
            y = Random.nextFloat() * 2000f,
            vx = (Random.nextFloat() - 0.5f) * 0.8f,
            vy = (Random.nextFloat() - 0.5f) * 0.8f,
            rotation = Random.nextFloat() * 360f,
            vRotation = (Random.nextFloat() - 0.5f) * 0.5f,
            size = Random.nextFloat() * 40f + 20f, // 20-60 size
            type = Random.nextInt(3)
        )
    }
}
