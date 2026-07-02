package com.royalshield.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.managers.CampaignManager
import com.royalshield.app.models.Campaign
import kotlinx.coroutines.launch
import android.widget.Toast

/**
 * Animated banner for "First 50 Users" and other limited campaigns
 * Displays remaining slots, countdown timer, and claim button
 */
@Composable
fun CampaignBanner(
    campaign: Campaign,
    onClaimed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val campaignManager = remember { CampaignManager(context) }
    
    var isClaiming by remember { mutableStateOf(false) }
    var hasClaimed by remember { mutableStateOf(false) }

    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "banner_gradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_offset"
    )

    // Pulsing border
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.2f),
                        Color(0xFFFF6B00).copy(alpha = 0.2f),
                        Color(0xFFFFD700).copy(alpha = 0.2f)
                    ),
                    startX = gradientOffset,
                    endX = gradientOffset + 500f
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = borderAlpha),
                        Color(0xFFFF6B00).copy(alpha = borderAlpha)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (campaign.type) {
                        com.royalshield.app.models.CampaignType.FIRST_50 -> "🔥 FIRST 50 USERS ONLY"
                        else -> "⚡ LIMITED TIME OFFER"
                    },
                    color = Color(0xFFFFD700),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Benefit description
            Text(
                text = buildBenefitText(campaign),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            // Remaining slots indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Only ",
                    color = Color(0xFFE0E0E0),
                    fontSize = 14.sp
                )
                Text(
                    text = "${campaign.remainingSlots}",
                    color = Color(0xFFFF3131),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = " slots left!",
                    color = Color(0xFFE0E0E0),
                    fontSize = 14.sp
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { (campaign.remainingSlots.toFloat() / 50f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFFF3131),
                trackColor = Color(0xFF3A3A3A),
            )

            // Claim button
            if (!hasClaimed) {
                Button(
                    onClick = {
                        if (!isClaiming) {
                            isClaiming = true
                            scope.launch {
                                val result = campaignManager.claimCampaign(campaign.id)
                                isClaiming = false
                                
                                if (result.isSuccess) {
                                    hasClaimed = true
                                    Toast.makeText(
                                        context,
                                        "✅ Premium unlocked! Enjoy your ${campaign.benefit.premiumDays} free days",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onClaimed()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "❌ ${result.exceptionOrNull()?.message ?: "Error claiming campaign"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isClaiming
                ) {
                    if (isClaiming) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CLAIM PREMIUM NOW",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            } else {
                // Success state
                Surface(
                    color = Color(0xFF00FF94).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        Color(0xFF00FF94)
                    )
                ) {
                    Text(
                        text = "✅ CLAIMED! Check your premium access",
                        color = Color(0xFF00FF94),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact banner for smaller spaces
 */
@Composable
fun CompactCampaignBanner(
    campaign: Campaign,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFD700).copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Color(0xFFFFD700)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "First 50 Promo",
                        color = Color(0xFFFFD700),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${campaign.remainingSlots} slots left",
                        color = Color(0xFFE0E0E0),
                        fontSize = 12.sp
                    )
                }
            }
            
            Text(
                text = "CLAIM →",
                color = Color(0xFFFFD700),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun buildBenefitText(campaign: Campaign): String {
    return when {
        campaign.benefit.premiumDays > 0 -> 
            "Get ${campaign.benefit.premiumDays} days of PREMIUM access absolutely FREE!"
        campaign.benefit.unlockPremiumTemp -> 
            "Unlock exclusive PREMIUM features"
        else -> 
            "Special promotional offer - claim now!"
    }
}
