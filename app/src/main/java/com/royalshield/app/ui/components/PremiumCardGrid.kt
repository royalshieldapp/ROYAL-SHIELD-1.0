package com.royalshield.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Data Class representing a Futuristic Premium Security Card item.
 */
data class CardItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val imageRes: Int,
    val themeColor: Color,
    val glowColor: Color,
    val onClick: () -> Unit
)

/**
 * Reusable premium glassmorphism card with dynamic 3D tilt interaction and glows.
 */
@Composable
fun RoyalShieldCard(
    item: CardItem,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 3D Parallax Tilt States
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }
    var cardScale by remember { mutableStateOf(1f) }
    var cardWidth by remember { mutableStateOf(1f) }
    var cardHeight by remember { mutableStateOf(1f) }

    val animatedTiltX by animateFloatAsState(
        targetValue = tiltX,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 250f),
        label = "tiltX"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = tiltY,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 250f),
        label = "tiltY"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else cardScale,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "scale"
    )

    // Outer Glow Effect configurations
    val glowColorArgb = item.glowColor.toArgb()
    val glowRadius = if (isPressed) 24.dp else 12.dp

    Box(
        modifier = modifier
            .height(125.dp)
            .graphicsLayer {
                rotationX = animatedTiltX
                rotationY = animatedTiltY
                scaleX = animatedScale
                scaleY = animatedScale
                cameraDistance = 10f * density
            }
            .onGloballyPositioned { coordinates ->
                cardWidth = coordinates.size.width.toFloat()
                cardHeight = coordinates.size.height.toFloat()
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val centerX = cardWidth / 2f
                        val centerY = cardHeight / 2f
                        val maxTiltAngle = 18f
                        tiltX = -((offset.y - centerY) / centerY).coerceIn(-1f, 1f) * maxTiltAngle
                        tiltY = ((offset.x - centerX) / centerX).coerceIn(-1f, 1f) * maxTiltAngle
                        cardScale = 0.96f
                        try {
                            tryAwaitRelease()
                        } finally {
                            tiltX = 0f
                            tiltY = 0f
                            cardScale = 1f
                        }
                    }
                )
            }
            .drawBehind {
                val radiusPx = glowRadius.toPx()
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        color = glowColorArgb
                        setShadowLayer(
                            radiusPx,
                            0f,
                            0f,
                            glowColorArgb
                        )
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        20.dp.toPx(),
                        20.dp.toPx(),
                        paint
                    )
                }
            }
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        item.themeColor.copy(alpha = 0.9f),
                        item.themeColor.copy(alpha = 0.1f),
                        item.themeColor.copy(alpha = 0.9f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = item.onClick
            )
    ) {
        // Async Image Background (Coil)
        AsyncImage(
            model = item.imageRes,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Glassmorphic Gradient Overlay (Matte depth styling)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Title and Subtitle Overlay details
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = item.title.uppercase(),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            if (item.subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Responsive 2-column grid component displaying premium futuristic cards.
 */
@Composable
fun PremiumCardGrid(
    items: List<CardItem>,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(items) { item ->
            RoyalShieldCard(item = item, modifier = Modifier.fillMaxWidth())
        }
    }
}
