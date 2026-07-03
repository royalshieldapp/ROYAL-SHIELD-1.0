package com.royalshield.app.ui.components

import com.royalshield.app.R

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.theme.*

/**
 * A futuristic ROUND button with a gold metallic ring and inner glow.
 * Inspired by the cyber-buttons package.
 */
@Composable
fun CyberButtonRound(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: Any? = null,
    contentDescription: String? = null,
    size: Dp = 64.dp,
    isActive: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.gold_circle_frame),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier.padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            when (icon) {
                is ImageVector -> Icon(icon, contentDescription, tint = RoyalGold, modifier = Modifier.size(size * 0.4f))
                is Int -> Icon(painterResource(id = icon), contentDescription, tint = Color.Unspecified, modifier = Modifier.size(size * 0.4f))
            }
        }
    }
}

/**
 * A futuristic RECTANGULAR button with angled tech accents and gold tabs.
 */
@Composable
fun CyberButtonRect(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    icon: ImageVector? = null,
    color: Color = RoyalGold
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.gold_button_bg), 
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = if(color == RoyalGold) Color.White else color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = text.uppercase(),
                color = if(color == RoyalGold) Color.White else color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * A segmented progress bar inspired by the holographic gold design.
 */
@Composable
fun CyberProgressBar(
    progress: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
    color: Color = RoyalGold,
    segmentCount: Int = 20
) {
    Canvas(modifier = modifier.height(20.dp).fillMaxWidth()) {
        val w = size.width
        val h = size.height
        val spacing = 4.dp.toPx()
        val segmentW = (w - (segmentCount - 1) * spacing) / segmentCount
        
        for (i in 0 until segmentCount) {
            val isFilled = (i.toFloat() / segmentCount) < progress
            val startX = i * (segmentW + spacing)
            
            // Draw slanted parallelogram
            val segmentPath = Path().apply {
                moveTo(startX + 4.dp.toPx(), 0f)
                lineTo(startX + segmentW + 4.dp.toPx(), 0f)
                lineTo(startX + segmentW, h)
                lineTo(startX, h)
                close()
            }
            
            drawPath(
                path = segmentPath,
                color = if (isFilled) color else color.copy(alpha = 0.15f)
            )
            
            if (isFilled) {
                // Add inner glow to filled segments
                drawPath(
                    path = segmentPath,
                    brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), color)),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}

/**
 * The full "Cyber Status Strip" with a percentage circle and segmented progress.
 */
@Composable
fun CyberStatusStrip(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = RoyalGold
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(25.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(25.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Percentage Circle
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(2.dp, Brush.sweepGradient(listOf(color, Color.White, color)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${(progress * 100).toInt()}%",
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label.uppercase(),
                color = color.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            CyberProgressBar(progress = progress, color = color)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
    }
}
