package com.royalshield.app.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.models.ProductInfo
import com.royalshield.app.NeonGoldTheme
import com.royalshield.app.managers.BillingManager
import kotlinx.coroutines.launch

/**
 * Security Screen Paywall - $0.99
 * Shown before entering Security Dashboard if user doesn't have access
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityPaywallScreen(
    billingManager: BillingManager,
    onPurchaseSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var c = context
        while (c is android.content.ContextWrapper) {
            if (c is Activity) break
            c = c.baseContext
        }
        c as? Activity
    }
    val scope = rememberCoroutineScope()
    
    var productDetails by remember { mutableStateOf<ProductInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    val purchaseState by billingManager.purchaseState.collectAsState()
    
    // Load product details
    LaunchedEffect(Unit) {
        scope.launch {
            val products = billingManager.queryProducts()
            productDetails = products?.firstOrNull()
            isLoading = false
        }
    }
    
    // Handle purchase state changes
    LaunchedEffect(purchaseState) {
        when (val state = purchaseState) {
            is BillingManager.PurchaseState.Success -> {
                Toast.makeText(context, "Purchase successful! Welcome to Security Center", Toast.LENGTH_LONG).show()
                onPurchaseSuccess()
            }
            is BillingManager.PurchaseState.Error -> {
                Toast.makeText(context, "Purchase failed: ${state.message}", Toast.LENGTH_LONG).show()
            }
            is BillingManager.PurchaseState.Cancelled -> {
                Toast.makeText(context, "Purchase cancelled", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0A0A), Color(0xFF1A1A1A), Color(0xFF0A0A0A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
            
            // Main content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(NeonGoldTheme.accent.copy(alpha = 0.3f), Color.Transparent)
                            ),
                            CircleShape
                        )
                        .border(3.dp, NeonGoldTheme.accent, CircleShape), // Slightly thicker for neon feel
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = NeonGoldTheme.accent,
                        modifier = Modifier.size(64.dp)
                    )
                }
                
                // Title
                Text(
                    text = "Unlock Security Center",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                // Subtitle
                Text(
                    text = "Access advanced protection features",
                    fontSize = 16.sp,
                    color = NeonGoldTheme.muted,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Features list
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(20.dp)
                        )
                        .border(1.5.dp, NeonGoldTheme.accent.copy(alpha=0.4f), RoundedCornerShape(20.dp)) // Neon Gold Border
                        .padding(24.dp)
                ) {
                    FeatureItem(
                        icon = Icons.Default.Search,
                        title = "URL Scanner",
                        description = "Check links for threats in real-time"
                    )
                    FeatureItem(
                        icon = Icons.Default.UploadFile,
                        title = "File Scanner",
                        description = "Detect malware in documents and downloads"
                    )
                    FeatureItem(
                        icon = Icons.Default.Lock,
                        title = "App Analysis",
                        description = "Scan installed apps for vulnerabilities"
                    )
                    FeatureItem(
                        icon = Icons.Default.Warning,
                        title = "Threat Map",
                        description = "View global cyber threats in real-time"
                    )
                }
            }
            
            // Price and purchase button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = NeonGoldTheme.accent)
                } else {
                    // Price display
                    val priceText = productDetails?.price ?: "$0.99"
                    
                    Text(
                        text = priceText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGoldTheme.accent
                    )
                    
                    Text(
                        text = "One-time purchase • Lifetime access",
                        fontSize = 14.sp,
                        color = NeonGoldTheme.muted
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Purchase button (Asset Integration)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .clickable(
                                enabled = purchaseState !is BillingManager.PurchaseState.Loading
                            ) {
                                activity?.let { act ->
                                    productDetails?.let { details ->
                                        billingManager.launchPurchaseFlow(act, details)
                                    } ?: run {
                                        Toast.makeText(context, "Product not available", Toast.LENGTH_SHORT).show()
                                    }
                                } ?: run {
                                    Toast.makeText(context, "Activity context error", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.gold_pill_button),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
                        )
                        
                        if (purchaseState is BillingManager.PurchaseState.Loading) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                "UNLOCK NOW",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }
                    
                    // Restore purchases
                    TextButton(onClick = {
                        // This will trigger a check of existing purchases
                        scope.launch {
                            val hasAccess = billingManager.checkPremiumAccess()
                            if (hasAccess) {
                                onPurchaseSuccess()
                            } else {
                                Toast.makeText(context, "No purchases found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text("Restore Purchase", color = NeonGoldTheme.muted)
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = NeonGoldTheme.accent,
            modifier = Modifier.size(32.dp)
        )
        Column {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = description,
                color = NeonGoldTheme.muted,
                fontSize = 13.sp
            )
        }
    }
}
