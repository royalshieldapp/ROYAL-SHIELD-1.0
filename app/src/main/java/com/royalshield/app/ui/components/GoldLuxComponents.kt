package com.royalshield.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.theme.RoyalGold

/**
 * A ultra-premium frame for "Gold Lux" text and components.
 * Matches the 'Pill' shape from visual reference.
 */
@Composable
fun GoldLuxFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gold_lux_shine")
    val shineOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shineOffset"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF8B6B23), // Lighter center
                        Color(0xFF4E3B11)  // Darker edges
                    )
                )
            )
            .drawBehind {
                // Outer gold metallic border effect
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFD4AF37),
                            Color(0xFFFFECB3),
                            Color(0xFF8B6B23),
                            Color(0xFFD4AF37)
                        ),
                        start = Offset(shineOffset * size.width, 0f),
                        end = Offset(shineOffset * size.width + size.width / 2, size.height)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(100f, 100f),
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Subtle inner shadow/depth
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(100f, 100f),
                    style = Stroke(width = 1.dp.toPx()),
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx())
                )
            }
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Text optimized for Gold Lux aesthetics
 */
@Composable
fun GoldLuxText(
    text: String,
    fontSize: Int = 14,
    fontWeight: FontWeight = FontWeight.Bold,
    letterSpacing: Float = 1.5f
) {
    Text(
        text = text.uppercase(),
        style = TextStyle(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFECB3),
                    Color(0xFFFFD700),
                    Color(0xFFB8860B)
                )
            ),
            fontSize = fontSize.sp,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing.sp
        )
    )
}
