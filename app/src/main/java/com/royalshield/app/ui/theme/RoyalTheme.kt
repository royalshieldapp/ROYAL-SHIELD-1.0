package com.royalshield.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object RoyalTheme {
    val BackgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF5C6AC4), Color(0xFF7B42F6))
    )
    val Surface = Color(0xFFFFFFFF).copy(alpha = 0.10f)
    val SurfaceHighlight = Color(0xFFFFFFFF).copy(alpha = 0.15f)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFE2E8F0)
    val AccentRed = Color(0xFFE53E3E)
    val AccentRedHover = Color(0xFFC53030)
    val SuccessGreen = Color(0xFF48BB78)
    
    val CardShape = RoundedCornerShape(24.dp)
    val ButtonShape = RoundedCornerShape(12.dp)
    val InputShape = RoundedCornerShape(16.dp)
}
