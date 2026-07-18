package com.royalshield.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.royalshield.app.ui.theme.RoyalGold
import kotlin.math.sin

@Composable
internal fun GoldenSecurityWave(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "gold_security_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gold_security_wave_phase"
    )

    Canvas(modifier = modifier) {
        val wave = Path()
        val centerY = size.height * 0.57f
        val amplitude = size.height * 0.13f
        val steps = 90

        for (step in 0..steps) {
            val progress = step / steps.toFloat()
            val x = progress * size.width
            val radians = (progress * Math.PI * 2.4 + phase * Math.PI * 2).toFloat()
            val y = centerY + sin(radians) * amplitude
            if (step == 0) wave.moveTo(x, y) else wave.lineTo(x, y)
        }

        drawPath(
            path = wave,
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, RoyalGold.copy(alpha = 0.38f), RoyalGold, RoyalGold.copy(alpha = 0.38f), Color.Transparent)
            ),
            style = Stroke(width = 1.6.dp.toPx())
        )

        for (step in 0..steps step 3) {
            val progress = step / steps.toFloat()
            val x = progress * size.width
            val radians = (progress * Math.PI * 2.4 + phase * Math.PI * 2).toFloat()
            val y = centerY + sin(radians) * amplitude
            val distanceFromCenter = 1f - kotlin.math.abs(progress - 0.5f) * 2f
            drawCircle(
                color = RoyalGold.copy(alpha = (0.10f + distanceFromCenter * 0.35f).coerceIn(0f, 0.45f)),
                radius = (0.8.dp + (step % 4).dp).toPx(),
                center = Offset(x, y)
            )
        }
    }
}
