package com.royalshield.app.ui.screens

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.royalshield.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.managers.BillingManager
import com.royalshield.app.ui.theme.RoyalGold
import androidx.compose.ui.graphics.luminance
import com.royalshield.app.ui.viewmodels.PaywallUiState
import com.royalshield.app.ui.viewmodels.PaywallViewModel
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.CornerRadius

@Composable
fun PremiumEliteScreen(
    onContinue: () -> Unit,
    onPurchaseSuccess: () -> Unit,
    billingManager: BillingManager
) {
    val context = LocalContext.current
    
    val viewModel: PaywallViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PaywallViewModel(billingManager) as T
            }
        }
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val selectedProductId by viewModel.selectedProductId.collectAsState()
    val purchasedPack by viewModel.purchasedPack.collectAsState()

    // Navigate on successful purchase
    LaunchedEffect(uiState) {
        if (uiState is PaywallUiState.Success) {
            onPurchaseSuccess()
        }
    }

    com.royalshield.app.ui.components.RoyalGradientBackground {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Golden Particle Wallpaper Background
            Image(
                painter = painterResource(id = R.drawable.bg_subscription_gold),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Dark overlay for better contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                Spacer(modifier = Modifier.height(32.dp))

                if (com.royalshield.app.managers.PreferencesManager.isDiscountApplied()) {
                    Surface(
                        color = RoyalGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .border(1.dp, RoyalGold, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "🎁 10% QR Discount Applied!",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = RoyalGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                when (val state = uiState) {
                    is PaywallUiState.Loading, is PaywallUiState.Purchasing -> {
                        CircularProgressIndicator(color = RoyalGold)
                    }
                    is PaywallUiState.Error -> {
                        Text("Error: ${state.message}", color = Color.Red, modifier = Modifier.padding(16.dp))
                        Button(onClick = { /* Retry */ }) { Text("Retry") }
                    }
                    is PaywallUiState.ProductsReady -> {
                        val starter = state.starter
                        val gold = state.gold
                        val ultimate = state.ultimate

                        // STARTER - WHITE THEME
                        PlanCard(
                            title = "STARTER",
                            price = starter?.price ?: "$9.99",
                            features = listOf("Basic Protection", "File Scanner", "Ad Blocker"),
                            iconRes = R.drawable.starter_emblem_screen_subscripcion,
                            isSelected = selectedProductId == BillingManager.PRODUCT_STARTER, 
                            themeColor = Color.White, // White for starter
                            onClick = { viewModel.selectProduct(BillingManager.PRODUCT_STARTER) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // GOLD - GOLD THEME
                        PlanCard(
                            title = "GOLD BUNDLE",
                            price = gold?.price ?: "$19.99",
                            features = listOf("All Starter Features", "Smart Automation", "Gold App Theme", "Priority Support"),
                            iconRes = R.drawable.emblem_golden_screen_subscription,
                            isPopular = true,
                            isSelected = selectedProductId == BillingManager.PRODUCT_GOLD,
                            themeColor = RoyalGold, // Gold
                            onClick = { viewModel.selectProduct(BillingManager.PRODUCT_GOLD) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // ULTIMATE - RED THEME
                        PlanCard(
                            title = "ULTIMATE PACK",
                            price = ultimate?.price ?: "$39.49",
                            features = listOf("Lifetime Everything", "AI Hub Unlimited", "Elite Badge", "Future Updates"),
                            iconRes = R.drawable.emblem_ultimate_red_screen_subscription,
                            isBestValue = true,
                            isSelected = selectedProductId == BillingManager.PRODUCT_ULTIMATE,
                            themeColor = Color(0xFFFF3B30), // Red for ultimate
                            glow = true,
                            onClick = { viewModel.selectProduct(BillingManager.PRODUCT_ULTIMATE) }
                        )
                    }
                    else -> {}
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Text(
                    text = "Restore Purchases",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { viewModel.restorePurchases() }
                        .padding(16.dp)
                )
                
                 Row(
                    modifier = Modifier
                        .clickable { onContinue() }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                 ) {
                     Text(
                        text = "Continue with Limited Features",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward, 
                        contentDescription = "arrow_doble",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                 }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 16.dp)
            ) {
                val isEnabled = selectedProductId != null
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .clickable(enabled = isEnabled) {
                            val activity = (context as? android.app.Activity) 
                                ?: ((context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity)
                            if (activity != null) {
                                viewModel.purchaseSelected(activity)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.gold_pill_button),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                    Text(
                        text = "UNLOCK NOW",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    features: List<String>,
    iconRes: Int? = null,
    isSelected: Boolean = false,
    isPopular: Boolean = false,
    isBestValue: Boolean = false,
    themeColor: Color = RoyalGold, // Default to Gold, but overridable (e.g. Bronze)
    glow: Boolean = false,
    onClick: () -> Unit
) {
    // --- Theme & Style ---
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val cardBg = if (isLight) Color.White else Color(0xFF1E1E1E)
    
    // Apply Theme Color
    val titleColor = if (isLight && themeColor == Color.White) Color.Black else themeColor
    val priceColor = titleColor
    val featureColor = if (isLight) Color.DarkGray else Color.White.copy(alpha = 0.9f)
    
    // --- Animated Border Effect ---
    // Create a rotating gradient for the "transition effect around"
    val infiniteTransition = rememberInfiniteTransition(label = "border_transition")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )
    
    // Dynamic Border Color
    // Slow transition pulsing the theme color instead of a flashing rotating neon.
    val borderBrush = if (isSelected || glow || isPopular || isBestValue) {
        SolidColor(themeColor.copy(alpha = alphaAnim))
    } else {
        SolidColor(themeColor.copy(alpha = 0.5f))
    }

    // Animated scale for card selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )
    
    // Animated shadow
    val shadowElevation by animateDpAsState(
        targetValue = if (isSelected) 16.dp else if (glow) 10.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "shadowElevation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .scale(scale)
            .shadow(shadowElevation, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            // Animated Border (Slow Transition)
            .drawBehind {
                drawRoundRect(
                    brush = borderBrush,
                    size = size,
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            // Inner content padding
            .padding(2.dp) 
            .background(cardBg, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Icon
            if (iconRes != null) {
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "iconScale"
                )
                
                val isBronze = title.contains("STARTER", ignoreCase = true) || title.contains("BRONZE", ignoreCase = true)
                val isUltimate = title.contains("ULTIMATE", ignoreCase = true)
                val isGold = title.contains("GOLD", ignoreCase = true)
                
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(
                            when {
                                isBronze -> 110.dp
                                isUltimate -> 120.dp
                                isGold -> 100.dp
                                else -> 80.dp
                            }
                        ) 
                        .padding(bottom = 8.dp)
                        .scale(iconScale),
                    contentScale = ContentScale.Fit
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(title, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Lifetime One-time", color = Color.Gray, fontSize = 10.sp)
                }
                
                // Animated Badges
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(
                        visible = isPopular,
                        enter = fadeIn(animationSpec = tween(400)) + scaleIn(
                            initialScale = 0.8f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    ) {
                        Badge(text = "MOST POPULAR", color = themeColor, textColor = Color.Black)
                    }
                    AnimatedVisibility(
                        visible = isBestValue,
                        enter = fadeIn(animationSpec = tween(400)) + scaleIn(
                            initialScale = 0.8f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    ) {
                        Badge(text = "BEST VALUE", color = Color(0xFFE91E63), textColor = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Animated Features List
            features.take(3).forEachIndexed { index, feature ->
                var visible by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay((index * 100).toLong())
                    visible = true
                }
                
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(400)) + 
                           expandVertically(animationSpec = tween(400))
                ) {
                    Row(
                        modifier = Modifier.padding(bottom = 4.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DoneAll,
                            null,
                            tint = if(isSelected) RoyalGold else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(feature, color = featureColor, fontSize = 13.sp)
                    }
                }
            }
            
            if (features.size > 3) {
                 Text(
                     "+ ${features.size - 3} more...",
                     color = themeColor.copy(alpha=0.7f),
                     fontSize = 11.sp,
                     modifier = Modifier.padding(start=24.dp)
                 )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Animated Price
            val priceScale by animateFloatAsState(
                targetValue = if (isSelected) 1.1f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "priceScale"
            )
            
            Text(
                price,
                color = priceColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.scale(priceScale)
            )
        }
        
        // Selection Indicator (Radio Button style)
        RadioButton(
            selected = isSelected,
            onClick = null, 
            colors = RadioButtonDefaults.colors(selectedColor = themeColor, unselectedColor = Color.Gray),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = if(isPopular || isBestValue) 28.dp else 12.dp, end = 12.dp)
        )
    }
}

@Composable
fun Badge(text: String, color: Color, textColor: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
