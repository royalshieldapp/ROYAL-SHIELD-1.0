package com.royalshield.app.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.core.*
import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb

// ... (other imports)


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.managers.BillingManager
import com.royalshield.app.ui.viewmodels.PaywallUiState
import com.royalshield.app.ui.viewmodels.PaywallViewModel
import com.royalshield.app.ui.theme.Royal_shieldTheme
import com.android.billingclient.api.ProductDetails
import com.royalshield.app.R
import com.royalshield.app.billing.BillingRepository
import com.royalshield.app.subscription.SubscriptionManager
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.Icons

data class UiSubscriptionTier(
    val title: String,
    val price: String,
    val perks: List<String>,
    val accent: Color,
    val badgeRes: Int,
    val productDetails: ProductDetails? = null
)

@Composable
fun SubscriptionScreen(
    modifier: Modifier = Modifier, 
    isPremium: Boolean, 
    billingManager: BillingManager,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // Unified ViewModel approach
    val viewModel: PaywallViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PaywallViewModel(billingManager) as T
            }
        }
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val purchasedPack by viewModel.purchasedPack.collectAsState()

    val goldColor = Color(0xFFFFC107)
    val redColor = Color(0xFFFF3B30)
    
    var showLimitedOffer by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is PaywallUiState.Success) {
            Toast.makeText(context, (uiState as PaywallUiState.Success).message, Toast.LENGTH_LONG).show()
        } else if (uiState is PaywallUiState.Error) {
            Toast.makeText(context, (uiState as PaywallUiState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

    // Map UI Tiers to actual products from the ViewModel
    val products = (uiState as? PaywallUiState.ProductsReady)
    
    val demoTiers = listOf(
        UiSubscriptionTier(
            title = products?.starter?.title ?: "Starter Shield",
            price = products?.starter?.price ?: "USD 4.99 / mo",
            perks = listOf("Basic threat scan", "Safe Browsing", "1 Device"),
            accent = Color.White,
            badgeRes = R.drawable.starter_emblem_screen_subscripcion,
            productDetails = products?.starter?.originalDetails // This holds the real Google Play details
        ),
        UiSubscriptionTier(
            title = products?.gold?.title ?: "Gold Bundle",
            price = products?.gold?.price ?: "USD 29.99 / mo",
            perks = listOf("Priority Support", "Cloud Reports", "10 Devices + Admin Panel"),
            accent = goldColor,
            badgeRes = R.drawable.emblem_golden_screen_subscription,
            productDetails = products?.gold?.originalDetails
        ),
        UiSubscriptionTier(
            title = products?.ultimate?.title ?: "Ultimate Shield",
            price = products?.ultimate?.price ?: "USD 99.99 / mo",
            perks = listOf("Dedicated Analyst", "API Access", "Unlimited Devices", "Forensic Audit"),
            accent = redColor,
            badgeRes = R.drawable.emblem_ultimate_red_screen_subscription,
            productDetails = products?.ultimate?.originalDetails
        )
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background Image with Globe
        Image(
            painter = painterResource(id = R.drawable.bg_subscription_globe),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Dark Overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, 
                    modifier = Modifier.weight(1f).padding(end = 48.dp) // Offset the back button for centering
                ) {
                    Text(
                        text = "SELECT YOUR SHIELD",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Upgrade to unlock full potential",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            if (com.royalshield.app.managers.PreferencesManager.isDiscountApplied()) {
                Surface(
                    color = goldColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .border(1.dp, goldColor, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "🎁 10% QR Discount Applied!",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = goldColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (isPremium) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Premium Features Active - ${purchasedPack ?: "Elite User"}", 
                        color = goldColor, 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                demoTiers.forEach { tier ->
                    SubscriptionTierCard(
                        tier = tier,
                        onSubscribe = {
                            // Find the correct product ID to select in ViewModel
                            val productId = when(tier.title) {
                                products?.starter?.title -> BillingManager.PRODUCT_STARTER
                                products?.gold?.title -> BillingManager.PRODUCT_GOLD
                                products?.ultimate?.title -> BillingManager.PRODUCT_ULTIMATE
                                else -> {
                                    // Fallback mapping if products aren't ready yet or names mismatch
                                    if (tier.title.contains("Starter")) BillingManager.PRODUCT_STARTER
                                    else if (tier.title.contains("Gold")) BillingManager.PRODUCT_GOLD
                                    else BillingManager.PRODUCT_ULTIMATE
                                }
                            }
                            
                            // Improved activity context retrieval
                            val activity = context.getActivity()
                            
                            if (activity != null) {
                                // Select and purchase
                                viewModel.selectProduct(productId)
                                viewModel.purchaseSelected(activity)
                            } else {
                                Toast.makeText(context, "Error: Activity context not found", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (showLimitedOffer) {
            PromoDialog(
                title = "Loyalty Promo",
                description = "Upgrade to any lifetime plan today and get priority support.",
                confirmLabel = "Got it",
                onConfirm = { showLimitedOffer = false },
                onDismiss = { showLimitedOffer = false }
            )
        }
    }
}

// Helper to find Activity in context wrappers
private fun android.content.Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.getActivity()
    else -> null
}

@Composable
fun SubscriptionTierCard(tier: UiSubscriptionTier, onSubscribe: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp), // Space for badge overhang
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131313).copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(8.dp)
            // Removed neon border as requested by user
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tier.title.uppercase(),
                    color = tier.accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = tier.price, 
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                
                tier.perks.forEach {
                    Text(text = "• $it", color = Color(0xFFCCCCCC), fontSize = 13.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Redesigned Button - Clean background, no overflow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(tier.accent.copy(alpha = 0.9f), tier.accent.copy(alpha = 0.7f))
                            )
                        )
                        .clickable { onSubscribe() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "UNLOCK NOW",
                        color = Color.Black,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    )
                }
            }
        }
        
        // Badge (Insignia) - Centered Top
        val isStarter = tier.title.contains("Starter", ignoreCase = true)
        val isUltimate = tier.title.contains("Ultimate", ignoreCase = true)
        val isGold = tier.title.contains("Gold", ignoreCase = true)

        val badgeSize: androidx.compose.ui.unit.Dp = when {
            isStarter -> 150.dp
            isUltimate -> 160.dp
            isGold -> 140.dp
            else -> 120.dp
        }

        Image(
            painter = painterResource(id = tier.badgeRes),
            contentDescription = "Badge",
            modifier = Modifier
                .size(badgeSize)
                .offset(y = (-50).dp) 
        )
    }
}

@Composable
fun PromoDialog(
    title: String,
    description: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold, color = Color(0xFFFFC107)) },
        text = { Text(text = description, color = Color.White) },
        containerColor = Color(0xFF1E1E1E),
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(text = confirmLabel, color = Color(0xFFFFC107)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Close", color = Color.Gray) }
        }
    )
}
