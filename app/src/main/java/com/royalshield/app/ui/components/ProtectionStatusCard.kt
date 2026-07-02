package com.royalshield.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable

/**
 * Protection status levels
 */
enum class ProtectionStatus {
    SAFE,      // Green neon
    THREAT,    // Red neon
    PREMIUM,   // Gold neon
    WARNING    // Orange neon
}

/**
 * Reusable card component with animated neon border effect
 * Used across Dashboard, Security, and other screens
 */
@Composable
fun ProtectionStatusCard(
    status: ProtectionStatus,
    title: String,
    subtitle: String? = null,
    description: String? = null,
    modifier: Modifier = Modifier,
    customIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    val (borderColor, backgroundColor, iconColor) = when (status) {
        ProtectionStatus.SAFE -> Triple(
            Color(0xFF00FF94),
            Color(0xFF00FF94).copy(alpha = 0.1f),
            Color(0xFF00FF94)
        )
        ProtectionStatus.THREAT -> Triple(
            Color(0xFFFF3131),
            Color(0xFFFF3131).copy(alpha = 0.1f),
            Color(0xFFFF3131)
        )
        ProtectionStatus.PREMIUM -> Triple(
            Color(0xFFFFD700),
            Color(0xFFFFD700).copy(alpha = 0.1f),
            Color(0xFFFFD700)
        )
        ProtectionStatus.WARNING -> Triple(
            Color(0xFFFF8C00),
            Color(0xFFFF8C00).copy(alpha = 0.1f),
            Color(0xFFFF8C00)
        )
    }

    // Animated pulsing glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "neon_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val glowWidth by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_width"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickableWithRipple(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF121216),
        border = BorderStroke(glowWidth.dp, borderColor.copy(alpha = glowAlpha)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with glow background
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(backgroundColor, CircleShape)
                    .border(1.5.dp, borderColor.copy(alpha = glowAlpha), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = customIcon ?: when (status) {
                        ProtectionStatus.SAFE -> Icons.Default.CheckCircle
                        ProtectionStatus.THREAT -> Icons.Default.Warning
                        ProtectionStatus.PREMIUM -> Icons.Default.Shield
                        ProtectionStatus.WARNING -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = title,
                    color = borderColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )

                if (description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        color = borderColor.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

/**
 * Simplified compact version for smaller spaces
 */
@Composable
fun ProtectionStatusBadge(
    status: ProtectionStatus,
    text: String,
    modifier: Modifier = Modifier
) {
    val borderColor = when (status) {
        ProtectionStatus.SAFE -> Color(0xFF00FF94)
        ProtectionStatus.THREAT -> Color(0xFFFF3131)
        ProtectionStatus.PREMIUM -> Color(0xFFFFD700)
        ProtectionStatus.WARNING -> Color(0xFFFF8C00)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_glow"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = borderColor.copy(alpha = 0.15f),
        border = BorderStroke(2.dp, borderColor.copy(alpha = glowAlpha))
    ) {
        Text(
            text = text,
            color = borderColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            letterSpacing = 1.sp
        )
    }
}

/**
 * Helper extension for clickable with ripple effect
 */

@Composable
private fun Modifier.clickableWithRipple(onClick: () -> Unit): Modifier {
    return this.clickable(
        onClick = onClick,
        indication = ripple(
            color = Color.White.copy(alpha = 0.2f)
        ),
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    )
}
