package com.royalshield.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.royalshield.app.ui.theme.RoyalGold

/**
 * Gold Premium Frame Card
 * A UI component for the AntiGravity Design System.
 * 
 * Features:
 * - Double metallic gold border
 * - Soft bevel effect
 * - Large rounded corners
 * - Diffuse shadow
 * - Matte black interior with subtle gradient
 * - Interactive states (Pressed, Active, Disabled)
 */
@Composable
fun RoyalFrameCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // --- State Animations ---
    val borderAlpha by animateColorAsState(
        targetValue = if (!enabled) Color.Gray.copy(alpha = 0.5f) 
                      else if (isSelected || isPressed) RoyalGold 
                      else RoyalGold.copy(alpha = 0.7f),
        label = "BorderAlpha"
    )
    
    val glowElevation by animateDpAsState(
        targetValue = if (!enabled) 0.dp else if (isSelected || isPressed) 12.dp else 4.dp,
        label = "GlowElevation"
    )

    val containerGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2A2A2A), // Lighter matte black top
            Color(0xFF151515)  // Darker matte black bottom
        )
    )

    // --- Container ---
    Box(
        modifier = modifier
            .shadow(
                elevation = glowElevation,
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(containerGradient)
            // Outer Gold Border (Soft)
            .border(
                width = 1.dp,
                color = borderAlpha.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            )
            // Inner gap padding
            .padding(1.dp)
            // Inner Highlight Border (Sharp)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        RoyalGold.copy(alpha = 0.9f),
                        RoyalGold.copy(alpha = 0.2f), 
                        RoyalGold.copy(alpha = 0.9f)
                    )
                ),
                shape = RoundedCornerShape(23.dp)
            )
            .then(
                if (enabled && onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null, // Custom visual feedback handled by state
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(contentPadding) // Inner content padding
    ) {
        // Disabled Overlay
        if (!enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
        }
        
        content()
    }
}
