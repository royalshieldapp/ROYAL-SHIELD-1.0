package com.royalshield.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex

/**
 * Paywall overlay that blocks premium features for FREE users
 * Shows upgrade options to SOLO or DUO plans
 * 
 * Usage: Wrap premium content with PaywallGuard component
 */
@Composable
fun PaywallGuard(
    isPremium: Boolean,
    featureName: String,
    content: @Composable () -> Unit,
    onUpgradeClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred content for FREE users
        if (!isPremium) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
            ) {
                content()
            }
        } else {
            content()
        }

        // Paywall overlay
        if (!isPremium) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .zIndex(10f)
                    .clickable { }, // Prevent clicks from passing through
                contentAlignment = Alignment.Center
            ) {
                PaywallCard(
                    featureName = featureName,
                    onUpgradeClick = onUpgradeClick
                )
            }
        }
    }
}

/**
 * Paywall dialog for premium features
 */
@Composable
fun PaywallDialog(
    featureName: String,
    onDismiss: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        PaywallCard(
            featureName = featureName,
            onUpgradeClick = onUpgradeClick,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun PaywallCard(
    featureName: String,
    onUpgradeClick: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    // Animated gold gradient
    val infiniteTransition = rememberInfiniteTransition(label = "paywall_gradient")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_shift"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1A1A2E)
    ) {
        Box {
            // Animated background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.1f),
                                Color(0xFFFF6B00).copy(alpha = 0.1f),
                                Color(0xFFFFD700).copy(alpha = 0.1f)
                            ),
                            startX = gradientShift,
                            endX = gradientShift + 500f
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Lock Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Title
                Text(
                    text = "Premium Feature",
                    color = Color(0xFFFFD700),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                // Feature locked message
                Text(
                    text = "$featureName is only available for premium users",
                    color = Color(0xFFE0E0E0),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Benefits list
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Upgrade to unlock:",
                        color = Color(0xFFB0B0B0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    PremiumBenefit("⚡ Deep Security Scans")
                    PremiumBenefit("🗺️ Full Cyber Threat Map")
                    PremiumBenefit("🔒 Advanced Protection Tools")
                    PremiumBenefit("👥 Duo Shield (2-for-1) with referrals")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Upgrade button
                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UPGRADE NOW",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Dismiss button (if available)
                if (onDismiss != null) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Maybe Later",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumBenefit(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFFFD700))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Inline paywall banner (less intrusive)
 */
@Composable
fun PaywallBanner(
    featureName: String,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1A2E),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Color(0xFFFFD700).copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Premium Feature",
                        color = Color(0xFFFFD700),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = featureName,
                        color = Color(0xFFE0E0E0),
                        fontSize = 14.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onUpgradeClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFFD700)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFFFFD700)
                )
            ) {
                Text("UPGRADE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
