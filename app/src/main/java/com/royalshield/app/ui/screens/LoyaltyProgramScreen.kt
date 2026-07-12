package com.royalshield.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.features.loyalty.LoyaltyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyProgramScreen(
    onBack: () -> Unit,
    vm: LoyaltyViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    
    val gold = Color(0xFFFFD700)
    val deepGold = Color(0xFFB8860B)
    val obsidian = Color(0xFF0A0A0A)
    val surfaceColor = Color(0xFF1A1A1A)
    
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "ROYAL LOYALTY", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = gold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = gold)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.loadLoyaltyStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = gold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = obsidian,
                    titleContentColor = gold
                )
            )
        },
        containerColor = obsidian
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Points Progress Card
                LoyaltyProgressCard(state.points, gold, deepGold, surfaceColor)

                // Tier Info
                TierSection(state.tier, gold, deepGold)

                // Detailed Benefits Section
                DetailedBenefitsSection(gold)

                // Reward Shop
                RewardShopSection(gold, vm)

                // Referral Section
                ReferralCard(gold, obsidian)

                Spacer(modifier = Modifier.height(20.dp))
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = gold)
                }
            }

            state.error?.let { err ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = err,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoyaltyProgressCard(points: Int, gold: Color, deepGold: Color, surface: Color) {
    val nextTier = when {
        points < 300 -> "Silver"
        points < 800 -> "Gold"
        points < 1500 -> "Elite"
        else -> null
    }
    val nextTierPoints = when {
        points < 300 -> 300
        points < 800 -> 800
        points < 1500 -> 1500
        else -> 1500
    }
    val progress = (points.toFloat() / nextTierPoints).coerceIn(0f, 1f)
    val remaining = (nextTierPoints - points).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Your Balance",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "$points Pts",
                color = gold,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = gold,
                trackColor = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (nextTier != null && remaining > 0) {
                Text(
                    "$remaining pts left for $nextTier Tier",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            } else {
                Text(
                    "Maximum Tier Achieved!",
                    color = gold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TierSection(currentTier: String, gold: Color, deepGold: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Your Insignias",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        var selectedBadge by remember { mutableStateOf<Pair<Int, String>?>(null) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TierBadge(R.drawable.img_cybershield_1_loyalty_program, "Level 1", currentTier == "Starter" || currentTier == "Bronze", gold) { 
                selectedBadge = Pair(R.drawable.img_cybershield_1_loyalty_program, "Level 1: Basic protection, standard security scans, and essential features.") 
            }
            TierBadge(R.drawable.badge_level_2, "Level 2", currentTier == "Pro" || currentTier == "Silver", gold) { 
                selectedBadge = Pair(R.drawable.badge_level_2, "Level 2: Advanced malware protection, encrypted VPN access, and faster response.") 
            }
            TierBadge(R.drawable.img_cyber_shield_2_loyalty_program, "Level 3", currentTier == "Gold", gold) { 
                selectedBadge = Pair(R.drawable.img_cyber_shield_2_loyalty_program, "Level 3: Golden themes, VIP Priority AI analysis, and full telemetry.") 
            }
            TierBadge(R.drawable.img_cybershield_3_loyalty_program, "Level 4", currentTier == "Elite" || currentTier == "Platinum", gold) { 
                selectedBadge = Pair(R.drawable.img_cybershield_3_loyalty_program, "Level 4: Ultimate Lifetime Access. Unlimited AI Hub, XDR Elite features, and exclusive insignias.") 
            }
        }
        
        selectedBadge?.let { badge ->
            AlertDialog(
                onDismissRequest = { selectedBadge = null },
                containerColor = Color(0xFF1A1A1A),
                title = { 
                    Text(badge.second.substringBefore(":"), color = gold, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) 
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = badge.first),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp).padding(bottom = 16.dp)
                        )
                        Text(
                            text = badge.second.substringAfter(":").trim(), 
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedBadge = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("CLOSE", color = gold, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Detailed Reward Note
        Surface(
            color = gold.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, null, tint = gold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "You are currently a $currentTier member. Enjoy exclusive themes and priority AI analysis.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun DetailedBenefitsSection(gold: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Exclusive Benefits",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        BenefitDetailItem(
            title = "Priority AI Processing",
            desc = "Your analysis requests are prioritized in our servers.",
            icon = Icons.Default.Bolt,
            color = gold
        )
        BenefitDetailItem(
            title = "Zero-Day Protection",
            desc = "Early access to the latest Threat Radar signatures.",
            icon = Icons.Default.Security,
            color = gold
        )
        BenefitDetailItem(
            title = "Premium Themes",
            desc = "Unlock the Golden and Matrix UI styles completely.",
            icon = Icons.Default.Palette,
            color = gold
        )
    }
}

@Composable
fun BenefitDetailItem(title: String, desc: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun TierBadge(iconRes: Int, name: String, active: Boolean, gold: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (active) Color(0xFF1E1E1E) else Color.DarkGray.copy(alpha = 0.3f))
                .border(
                    width = 1.dp,
                    color = if (active) gold.copy(alpha=0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center

        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                contentDescription = name,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            name,
            color = if (active) gold else Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BenefitsGrid(gold: Color) {
    val benefits = listOf(
        "Priority Support",
        "Cloud Storage+",
        "Early Beta Access",
        "Referral Multiplier"
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        benefits.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { benefit ->
                    BenefitItem(benefit, gold, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun BenefitItem(text: String, gold: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.DarkGray)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = gold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ReferralCard(gold: Color, background: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = gold)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Invite & Earn",
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                "Get 100 Gold Points for every friend who joins with your link.",
                color = Color.Black.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(
                onClick = { /* Share logic */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(50)
            ) {
                Text("Share invite link", color = gold)
            }
        }
    }
}

data class LoyaltyReward(
    val name: String,
    val description: String,
    val cost: Int,
    val icon: ImageVector
)

@Composable
fun RewardShopSection(gold: Color, vm: LoyaltyViewModel) {
    val state by vm.uiState.collectAsState()
    var redemptionStatus by remember { mutableStateOf<String?>(null) }
    var showRedemptionResult by remember { mutableStateOf(false) }
    
    val rewards = listOf(
        LoyaltyReward("1 Month Premium VPN", "Unlimited high-speed secure tunnel access.", 200, Icons.Default.VpnLock),
        LoyaltyReward("Matrix Custom Theme", "Unlock the glowing matrix style UI completely.", 500, Icons.Default.Palette),
        LoyaltyReward("Priority AI Processing (24h)", "Your analysis requests skip the queue.", 800, Icons.Default.Bolt),
        LoyaltyReward("VIP Custom Avatar", "Exclusive digital agent animation presets.", 1500, Icons.Default.Face)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Reward Shop",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rewards.forEach { reward ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(gold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(reward.icon, null, tint = gold, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = reward.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = reward.description,
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        
                        Button(
                            onClick = {
                                vm.redeemReward(reward.cost, reward.name) { success, message ->
                                    redemptionStatus = message
                                    showRedemptionResult = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.points >= reward.cost) gold else Color.DarkGray
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "${reward.cost} pts",
                                color = if (state.points >= reward.cost) Color.Black else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRedemptionResult) {
        AlertDialog(
            onDismissRequest = { showRedemptionResult = false },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text(
                    text = "REDEEM REWARD",
                    color = gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = redemptionStatus ?: "",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showRedemptionResult = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK", color = gold, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

