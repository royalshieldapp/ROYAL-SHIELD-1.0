package com.royalshield.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.royalshield.app.ui.theme.Royal_shieldTheme
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay

data class Promotion(
    val id: String,
    val title: String,
    val description: String,
    val discountPct: Int? = null,
    val code: String? = null,
    val expiresAt: String? = null,
    val featured: Boolean = false
)

@Composable
fun PremiumPromoScreen(
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var promotions by remember { mutableStateOf<List<Promotion>>(emptyList()) }

    LaunchedEffect(Unit) {
        // Simulate loading check
        delay(1500) // Splash delay
        if (!isPremium) {
            // Mock promotions
            promotions = listOf(
                Promotion(
                    id = "promo-black",
                    title = "Premium -40% for 3 months",
                    description = "Access to advanced automations and historical cyber-map.",
                    discountPct = 40,
                    code = "BLACK40",
                    featured = true
                ),
                Promotion(
                    id = "promo-annual",
                    title = "2 months free on Annual Plan",
                    description = "Save compared to monthly subscription.",
                    code = "ANNUAL24"
                )
            )
            showDialog = true
        } else {
            onDismiss()
        }
    }

    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Badge(containerColor = Color(0xFFFFD700).copy(alpha = 0.2f)) {
                                Text(
                                    "ANTIGRAVITY PREMIUM",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.5f))
                            }
                        }

                        Text(
                            text = "Unlock Total Power",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "Automations, Real-time Cyberattack Map and advanced security.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        // Featured Promo
                        val featured = promotions.find { it.featured } ?: promotions.firstOrNull()
                        if (featured != null) {
                            PromoItem(featured, isFeatured = true, onSubscribe)
                        }

                        // Other Promos
                        val others = promotions.filter { it != featured }
                        if (others.isNotEmpty()) {
                            Text(
                                "Other exclusive offers:",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.align(Alignment.Start)
                            )
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(others) { promo ->
                                    PromoItem(promo, isFeatured = false, onSubscribe)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = onDismiss) {
                            Text("Continue with free version", color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PromoItem(promo: Promotion, isFeatured: Boolean, onClick: () -> Unit) {
    val borderColor = if (isFeatured) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f)
    val containerColor = if (isFeatured) Color(0xFFFFD700).copy(alpha = 0.05f) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = promo.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (promo.discountPct != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = Color(0xFF4CAF50)) {
                            Text("-${promo.discountPct}%", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = promo.description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                if (promo.code != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Code: ${promo.code}",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewPremiumPromoScreen() {
    Royal_shieldTheme {
        PremiumPromoScreen(
            isPremium = false,
            onDismiss = {},
            onSubscribe = {}
        )
    }
}
