package com.royalshield.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun MatrixRainBackground(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00FF00),
    fontSize: Float = 42f, // text size in pixels
    columnWidthDp: Int = 18 // width of each column in dp
) {
    val density = LocalDensity.current
    val columnWidthPx = with(density) { columnWidthDp.dp.toPx() }
    
    // Characters used in Matrix rain
    val chars = remember {
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$#@%&"
    }
    
    // State of columns
    var columnsInitialized by remember { mutableStateOf(false) }
    var columns by remember { mutableStateOf(emptyList<MatrixColumn>()) }
    
    // Animation loop using LaunchedEffect
    LaunchedEffect(Unit) {
        while (true) {
            delay(40) // ~25 FPS is perfect for retro/stealth terminal feel and lightweight rendering
            if (columnsInitialized) {
                columns = columns.map { col ->
                    val newY = col.y + col.speed
                    if (newY > col.screenHeight + (col.trailLength * fontSize)) {
                        // Reset to top
                        col.copy(
                            y = -Random.nextFloat() * 300f - 100f,
                            speed = Random.nextFloat() * 15f + 10f,
                            trailLength = Random.nextInt(8, 20),
                            chars = List(col.trailLength) { chars[Random.nextInt(chars.length)] }
                        )
                    } else {
                        // Flicker some chars in the column
                        val updatedChars = col.chars.map { c ->
                            if (Random.nextFloat() < 0.1f) chars[Random.nextInt(chars.length)] else c
                        }
                        col.copy(y = newY, chars = updatedChars)
                    }
                }
            }
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val width = size.width
        val height = size.height
        
        if (!columnsInitialized && width > 0 && height > 0) {
            val totalColumns = (width / columnWidthPx).toInt() + 1
            columns = List(totalColumns) { i ->
                val trailLen = Random.nextInt(8, 20)
                MatrixColumn(
                    x = i * columnWidthPx,
                    y = Random.nextFloat() * height - height, // start at random heights
                    speed = Random.nextFloat() * 15f + 10f,
                    screenHeight = height,
                    trailLength = trailLen,
                    chars = List(trailLen) { chars[Random.nextInt(chars.length)] }
                )
            }
            columnsInitialized = true
        }
        
        if (columnsInitialized) {
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    isAntiAlias = true
                    textSize = fontSize
                    typeface = Typeface.MONOSPACE
                    textAlign = Paint.Align.CENTER
                }
                
                columns.forEach { col ->
                    // Draw each character in the column's trail
                    for (i in 0 until col.trailLength) {
                        val charY = col.y - (i * fontSize)
                        if (charY in -fontSize..height + fontSize) {
                            // Calculate fade/opacity for the trail (head is brightest, tail fades to zero)
                            val alpha = 1f - (i.toFloat() / col.trailLength)
                            val isHead = i == 0
                            
                            // Head is white or bright light green, trail is green
                            if (isHead) {
                                paint.color = android.graphics.Color.WHITE
                                paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                            } else {
                                // Dynamic color matching parameter
                                paint.color = color.toArgb()
                                paint.alpha = (alpha * 180).toInt().coerceIn(0, 255)
                            }
                            
                            val charStr = col.chars.getOrElse(i) { '0' }.toString()
                            canvas.nativeCanvas.drawText(charStr, col.x, charY, paint)
                        }
                    }
                }
            }
        }
    }
}

data class MatrixColumn(
    val x: Float,
    val y: Float,
    val speed: Float,
    val screenHeight: Float,
    val trailLength: Int,
    val chars: List<Char>
)
