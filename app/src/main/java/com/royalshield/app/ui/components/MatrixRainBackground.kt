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
import kotlin.random.Random

class MatrixColumn(
    val x: Float,
    var y: Float,
    var speed: Float,
    var screenHeight: Float,
    var trailLength: Int,
    var chars: CharArray
)

@Composable
fun MatrixRainBackground(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00FF00),
    fontSize: Float = 42f, // text size in pixels
    columnWidthDp: Int = 18 // width of each column in dp
) {
    val density = LocalDensity.current
    val columnWidthPx = remember(density, columnWidthDp) { with(density) { columnWidthDp.dp.toPx() } }

    val charsStr = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$#@%&"
    val columns = remember { mutableListOf<MatrixColumn>() }
    var columnsInitialized by remember { mutableStateOf(false) }

    // Ticks every frame to drive the canvas drawing
    val time by produceState(initialValue = 0L) {
        while (true) {
            withFrameMillis { value = it }
        }
    }

    // Avoid recomposition loop by storing last update time in a primitive array
    val lastUpdateTime = remember { longArrayOf(0L) }

    val textPaint = remember(fontSize) {
        Paint().apply {
            isAntiAlias = true
            textSize = fontSize
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
    }

    val baseColorArgb = color.toArgb()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Read time to ensure recomposition of draw phase ONLY
        val currentTime = time
        val width = size.width
        val height = size.height

        val delta = currentTime - lastUpdateTime[0]
        val shouldUpdatePhysics = delta > 40L
        if (shouldUpdatePhysics || lastUpdateTime[0] == 0L) {
            lastUpdateTime[0] = currentTime
        }

        if (!columnsInitialized && width > 0 && height > 0) {
            val totalColumns = (width / columnWidthPx).toInt() + 1
            columns.clear()
            for (i in 0 until totalColumns) {
                val trailLen = Random.nextInt(8, 20)
                columns.add(
                    MatrixColumn(
                        x = i * columnWidthPx,
                        y = Random.nextFloat() * height - height, // start at random heights
                        speed = Random.nextFloat() * 15f + 10f,
                        screenHeight = height,
                        trailLength = trailLen,
                        chars = CharArray(trailLen) { charsStr[Random.nextInt(charsStr.length)] }
                    )
                )
            }
            columnsInitialized = true
        }

        if (columnsInitialized) {
            drawIntoCanvas { canvas ->
                columns.forEach { col ->
                    // Update physics only every ~40ms to keep the retro feel
                    if (shouldUpdatePhysics) {
                        col.y += col.speed
                        if (col.y > height + (col.trailLength * fontSize)) {
                            // Reset to top
                            col.y = -Random.nextFloat() * 300f - 100f
                            col.speed = Random.nextFloat() * 15f + 10f
                            col.trailLength = Random.nextInt(8, 20)
                            col.chars = CharArray(col.trailLength) { charsStr[Random.nextInt(charsStr.length)] }
                        } else {
                            // Flicker some chars in the column
                            for (i in 0 until col.trailLength) {
                                if (Random.nextFloat() < 0.1f) {
                                    col.chars[i] = charsStr[Random.nextInt(charsStr.length)]
                                }
                            }
                        }
                    }

                    // Draw each character in the column's trail
                    for (i in 0 until col.trailLength) {
                        val charY = col.y - (i * fontSize)
                        if (charY in -fontSize..height + fontSize) {
                            // Calculate fade/opacity for the trail (head is brightest, tail fades to zero)
                            val alpha = 1f - (i.toFloat() / col.trailLength)
                            val isHead = i == 0

                            // Head is white or bright light color, trail is base color
                            if (isHead) {
                                textPaint.color = android.graphics.Color.WHITE
                                textPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                            } else {
                                textPaint.color = baseColorArgb
                                textPaint.alpha = (alpha * 180).toInt().coerceIn(0, 255)
                            }

                            val charStr = if (i < col.chars.size) col.chars[i].toString() else "0"
                            canvas.nativeCanvas.drawText(charStr, col.x, charY, textPaint)
                        }
                    }
                }
            }
        }
    }
}
