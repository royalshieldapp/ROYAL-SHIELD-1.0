package com.royalshield.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        color = Color.White.copy(alpha = 0.05f),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BorderColor,
    backgroundColor: Color = Color.White.copy(alpha = 0.05f),
    backgroundBrush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundMod = if (backgroundBrush != null) {
        Modifier.background(backgroundBrush)
    } else {
        Modifier.background(backgroundColor)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(backgroundMod)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
fun NeonRgbCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black.copy(alpha = 0.6f),
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rgb_border")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Transparent) 
    ) {
         // The Border Layer (Animated)
         Box(
             modifier = Modifier
                 .matchParentSize()
                 .drawBehind {
                     rotate(angle) {
                         drawRect(
                             brush = Brush.sweepGradient(
                                 colors = listOf(
                                     Color.Red, Color.Magenta, Color.Blue, 
                                     Color.Cyan, Color.Green, Color.Yellow, Color.Red
                                 )
                             )
                         )
                     }
                 }
         )
         
         // The Content Layer (Padded to reveal border)
         Box(
             modifier = Modifier
                 .matchParentSize()
                 .padding(2.dp) // Border Width
                 .clip(RoundedCornerShape(22.dp)) 
                 .background(backgroundColor)
         ) {
             Column(
                 modifier = Modifier.padding(20.dp),
                 content = content
             )
         }
    }
}

@Composable
fun PulsatingSOSButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(160.dp)
    ) {
        // Outer pulse rings
        Box(
            modifier = Modifier
                .size(140.dp * pulseScale)
                .drawBehind {
                    drawCircle(
                        color = NeonRed.copy(alpha = pulseAlpha),
                        radius = size.minDimension / 2
                    )
                }
        )
        
        // Main Button
        Surface(
            modifier = Modifier
                .size(120.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(50),
            color = NeonRed,
            shadowElevation = 12.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "SOS",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.3f),
                            blurRadius = 8f
                        )
                    )
                )
            }
        }
    }
}

@Composable
fun RoyalGradientBackground(
    containerColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(containerColor) // Adaptive Background
    ) {
        // Subtle top gradient for depth (like a studio light)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .drawBehind {
                     drawRect(
                         brush = Brush.verticalGradient(
                             colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
                         )
                     )
                }
        )
        content()
    }
}

@Composable
fun CyberDashboardHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "ROYAL SHIELD",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = RoyalGold,
                letterSpacing = 2.sp
            )
            Text(
                "ADVANCED SECURITY PROTOCOL",
                style = MaterialTheme.typography.labelSmall,
                color = BorderColor, // Was GlassBorder
                letterSpacing = 1.sp
            )
        }
        
        // Status Badge
        Surface(
            shape = RoundedCornerShape(50),
            color = SafeGreen.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).background(SafeGreen, RoundedCornerShape(50)))
                Text(
                    "LIVE",
                    color = SafeGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    PremiumGlassCard(
        modifier = modifier.height(110.dp)
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RoyalGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        /*
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.button_gold_bg),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        */
        Box(modifier = Modifier.fillMaxSize().background(RoyalGold, RoundedCornerShape(12.dp)))
        
        if (isLoading) {
            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
        } else {
            Text(
                text = text,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
