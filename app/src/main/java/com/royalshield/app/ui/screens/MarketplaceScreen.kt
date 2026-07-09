package com.royalshield.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.royalshield.app.ui.theme.*

// Mock data structures
data class RewardItem(
    val id: String,
    val title: String,
    val description: String,
    val price: Int,
    val category: String,
    val isFeatured: Boolean = false,
    val isLimited: Boolean = false,
    val icon: ImageVector,
    val owned: Boolean = false
)

val mockItems = listOf(
    RewardItem("1", "Black Gold Elite Theme", "Exclusive black and gold theme for Royal Shield. Experience the premium look and feel.", 1500, "Themes", true, false, Icons.Default.Security, true),
    RewardItem("2", "7 Days Premium", "Unlock all premium features for 7 days.", 3500, "Premium", true, false, Icons.Default.Star, false),
    RewardItem("3", "XP Boost x2 (24h)", "Double your XP gains for the next 24 hours.", 800, "XP Boosts", true, false, Icons.Default.KeyboardDoubleArrowUp, false),
    RewardItem("4", "Golden Week Special", "Up to 20% off on Premium items!", 2000, "Premium", false, true, Icons.Default.WorkspacePremium, false)
)

data class PointPack(val title: String, val points: Int, val price: String, val bonus: String? = null)
val packList = listOf(
    PointPack("STARTER PACK", 1000, "$0.99"),
    PointPack("DEFENDER PACK", 3200, "$2.99", "+200 BONUS"),
    PointPack("ELITE PACK", 8000, "$6.99", "+800 BONUS"),
    PointPack("ROYAL PACK", 16000, "$12.99", "+2,000 BONUS"),
    PointPack("ULTIMATE PACK", 35000, "$24.99", "+5,000 BONUS")
)

data class EarnAction(val title: String, val description: String, val points: Int, val isClaimable: Boolean = false)
val dailyActions = listOf(
    EarnAction("Daily Login", "Login to the app", 10, true),
    EarnAction("Complete Malware Scan", "Run a full malware scan", 25, true),
    EarnAction("Activate VPN", "Activate VPN for the first time", 50, false)
)
val onceActions = listOf(
    EarnAction("Complete Profile", "Fill all profile information", 100, false),
    EarnAction("Refer a User", "Invite a friend to join", 250, false),
    EarnAction("Referred User Subscribes", "When your referral subscribes", 1000, false),
    EarnAction("Use SOS Test Mode", "Run SOS test mode", 30, false),
    EarnAction("Check Security Score", "View your security score", 20, false)
)

enum class StoreRoute { HOME, DETAILS, BUY, EARN, REWARDS }

@Composable
fun MarketplaceScreen(onBack: () -> Unit) {
    var currentRoute by remember { mutableStateOf(StoreRoute.HOME) }
    var selectedItem by remember { mutableStateOf<RewardItem?>(null) }
    var userBalance by remember { mutableIntStateOf(850) }

    // Dialogs
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SpaceDeep
    ) {
        Crossfade(targetState = currentRoute, label = "StoreNavigation") { route ->
            when (route) {
                StoreRoute.HOME -> MarketplaceHome(
                    balance = userBalance,
                    onBuyPoints = { currentRoute = StoreRoute.BUY },
                    onEarnPoints = { currentRoute = StoreRoute.EARN },
                    onItemClick = {
                        selectedItem = it
                        currentRoute = StoreRoute.DETAILS
                    },
                    onViewRewards = { currentRoute = StoreRoute.REWARDS }
                )
                StoreRoute.DETAILS -> selectedItem?.let { item ->
                    ItemDetails(
                        item = item,
                        onBack = { currentRoute = StoreRoute.HOME },
                        onRedeem = { showConfirmDialog = true }
                    )
                }
                StoreRoute.BUY -> BuyPointsScreen(onBack = { currentRoute = StoreRoute.HOME })
                StoreRoute.EARN -> EarnPointsScreen(onBack = { currentRoute = StoreRoute.HOME })
                StoreRoute.REWARDS -> MyRewardsScreen(onBack = { currentRoute = StoreRoute.HOME })
            }
        }
    }

    if (showConfirmDialog && selectedItem != null) {
        ConfirmRedemptionDialog(
            item = selectedItem!!,
            onCancel = { showConfirmDialog = false },
            onConfirm = {
                showConfirmDialog = false
                userBalance -= selectedItem!!.price
                showSuccessDialog = true
            }
        )
    }

    if (showSuccessDialog) {
        RewardUnlockedDialog(
            onViewRewards = {
                showSuccessDialog = false
                currentRoute = StoreRoute.REWARDS
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceHome(
    balance: Int,
    onBuyPoints: () -> Unit,
    onEarnPoints: () -> Unit,
    onItemClick: (RewardItem) -> Unit,
    onViewRewards: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("ROYAL MARKETPLACE", color = Color.White, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceDeep),
            navigationIcon = {
                IconButton(onClick = { /* Handle global nav menu if needed */ }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = RoyalGold)
                }
            },
            actions = {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = Color.White)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp) // Space for bottom bar
        ) {
            // Balance Card
            item {
                BalanceCard(balance, onBuyPoints, onEarnPoints)
            }

            // Featured Rewards
            item {
                SectionHeader("FEATURED REWARDS", "View all")
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(mockItems.filter { it.isFeatured }) { item ->
                        RewardCard(item = item, onClick = { onItemClick(item) })
                    }
                }
            }

            // Categories
            item {
                SectionHeader("CATEGORIES", "View all")
                val categories = listOf(
                    "Themes" to Icons.Default.Palette,
                    "Wallpapers" to Icons.Default.Wallpaper,
                    "Badges" to Icons.Default.Verified,
                    "Premium" to Icons.Default.WorkspacePremium,
                    "XP Boosts" to Icons.Default.KeyboardDoubleArrowUp,
                    "Storage" to Icons.Default.Cloud,
                    "Icons" to Icons.Default.Apps
                )

                // 4 columns grid for categories
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val chunked = categories.chunked(4)
                    chunked.forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowItems.forEach { (name, icon) ->
                                CategoryItem(name, icon, modifier = Modifier.weight(1f))
                            }
                            if (rowItems.size < 4) {
                                repeat(4 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Limited Offers
            item {
                SectionHeader("LIMITED OFFERS", "View all")
                mockItems.filter { it.isLimited }.forEach { item ->
                    LimitedOfferCard(item = item, onClick = { onItemClick(item) })
                }
            }

            // My Active Rewards
            item {
                SectionHeader("MY ACTIVE REWARDS", "View all", onViewRewards)
                ActiveRewardCard("XP Boost x2", "18h 45m remaining", Icons.Default.KeyboardDoubleArrowUp)
            }
        }
    }
}

@Composable
fun BalanceCard(balance: Int, onBuyPoints: () -> Unit, onEarnPoints: () -> Unit) {
    Box(modifier = Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(
        Brush.horizontalGradient(listOf(Color(0xFF221100), Color(0xFF110A00)))
    ).border(1.dp, RoyalGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("YOUR BALANCE", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(balance.toString(), color = RoyalGold, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Text("Royal Points", color = Color.White, fontSize = 14.sp)
                }

                // Big shield icon
                Icon(Icons.Default.Security, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(80.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onBuyPoints, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = SpaceDeep), shape = RoundedCornerShape(8.dp)) {
                    Text("BUY POINTS", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onEarnPoints, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalGold), border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGold), shape = RoundedCornerShape(8.dp)) {
                    Text("EARN POINTS", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, actionText: String, onAction: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(actionText, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.clickable { onAction() })
    }
}

@Composable
fun RewardCard(item: RewardItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(item.icon, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(48.dp).padding(bottom = 12.dp))
        Text(item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.height(32.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Toll, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("%,d".format(item.price), color = RoyalGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CategoryItem(name: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.clickable { }) {
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = name, tint = RoyalGold, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
fun LimitedOfferCard(item: RewardItem, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1500)).border(1.dp, RoyalGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).clickable { onClick() }) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = RoyalGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(item.description, color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("02 D", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(" : ", color = Color.White, fontSize = 12.sp)
                    Text("18 H", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(" : ", color = Color.White, fontSize = 12.sp)
                    Text("34 M", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Icon(item.icon, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(60.dp))
        }
    }
}

@Composable
fun ActiveRewardCard(title: String, subtitle: String, icon: ImageVector) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

// ---------------------------------------------------------
// Sub Screens
// ---------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetails(item: RewardItem, onBack: () -> Unit, onRedeem: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("ITEM DETAILS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tint = Color.White, contentDescription = "Back") }
            },
            actions = {
                IconButton(onClick = { }) { Icon(Icons.Default.FavoriteBorder, tint = Color.White, contentDescription = "Favorite") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceDeep)
        )

        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            // Big Preview
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1A1500)).border(1.dp, RoyalGold, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(100.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(color = Color(0xFF331A4D), shape = RoundedCornerShape(4.dp)) {
                    Text("THEME", color = Color(0xFFD9B3FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(item.description, color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp)

            Spacer(modifier = Modifier.height(24.dp))

            ItemPropertyRow("Price", {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Toll, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("%,d".format(item.price), color = RoyalGold, fontWeight = FontWeight.Bold)
                }
            })
            ItemPropertyRow("Category", { Text(item.category, color = Color.White) })
            ItemPropertyRow("Duration", { Text("Permanent", color = Color.White) })
            ItemPropertyRow("Status", { Text("Available", color = SafeGreen) })

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalGold), border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGold), shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PREVIEW", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = onRedeem, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = SpaceDeep), shape = RoundedCornerShape(8.dp)) {
                Text("REDEEM NOW", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ItemPropertyRow(label: String, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        content()
    }
    HorizontalDivider(color = Color(0xFF333333))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyPointsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("BUY POINTS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tint = Color.White, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceDeep)
        )

        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Royal Points are used to unlock premium features and rewards.", color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Toll, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(48.dp))
                }
            }

            items(packList) { pack ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)).padding(16.dp).clickable { }, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pack.title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("%,d".format(pack.points), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Royal Points", color = Color.Gray, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(color = RoyalGold, shape = RoundedCornerShape(4.dp)) {
                            Text(pack.price, color = SpaceDeep, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                        if (pack.bonus != null) {
                            Text(pack.bonus, color = SafeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("All purchases are secure and verified\nthrough Google Play.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarnPointsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("EARN POINTS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tint = Color.White, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceDeep)
        )

        var selectedTab by remember { mutableIntStateOf(0) }

        TabRow(selectedTabIndex = selectedTab, containerColor = SpaceDeep, contentColor = RoyalGold, indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = RoyalGold
            )
        }) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("DAILY ACTIONS", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("ONCE ACTIONS", fontWeight = FontWeight.Bold) })
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Complete actions and earn Royal Points every day.", color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(48.dp))
                }
            }

            val actions = if (selectedTab == 0) dailyActions else onceActions
            items(actions) { action ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(action.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(action.description, color = Color.Gray, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Toll, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(action.points.toString(), color = RoyalGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    if (action.isClaimable) {
                        Surface(color = RoyalGold, shape = RoundedCornerShape(4.dp), modifier = Modifier.clickable {}) {
                            Text("CLAIM", color = SpaceDeep, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    } else {
                        Surface(color = Color.Transparent, shape = RoundedCornerShape(4.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray), modifier = Modifier.clickable {}) {
                            Text("GO", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFF222222), modifier = Modifier.padding(bottom = 12.dp, start = 36.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRewardsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("MY REWARDS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tint = Color.White, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceDeep)
        )

        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("OWNED", "ACTIVE", "EXPIRED", "LOCKED")

        ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = SpaceDeep, contentColor = RoyalGold, edgePadding = 16.dp, indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = RoyalGold
            )
        }) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title, fontWeight = FontWeight.Bold) })
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(mockItems.filter { it.owned }) { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)).padding(16.dp).clickable { }, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF2A2A2A)).border(1.dp, RoyalGold, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(item.icon, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(item.category, color = Color.Gray, fontSize = 12.sp)
                        Text("Owned", color = SafeGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
        }
    }
}

// ---------------------------------------------------------
// Dialogs
// ---------------------------------------------------------

@Composable
fun ConfirmRedemptionDialog(item: RewardItem, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1A1A1A), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CONFIRM REDEMPTION", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1500)).border(1.dp, RoyalGold, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(64.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Default.Toll, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("%,d".format(item.price), color = RoyalGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(" Royal Points", color = Color.Gray, fontSize = 14.sp)
                }

                Text("You are about to redeem this reward using Royal Points. This action cannot be reversed.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray), shape = RoundedCornerShape(8.dp)) {
                        Text("CANCEL")
                    }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = SpaceDeep), shape = RoundedCornerShape(8.dp)) {
                        Text("REDEEM", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RewardUnlockedDialog(onViewRewards: () -> Unit) {
    Dialog(onDismissRequest = onViewRewards, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1A1A1A), border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen)) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(SafeGreen.copy(alpha = 0.2f)).border(2.dp, SafeGreen, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("REWARD UNLOCKED!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Your reward has been added to your Royal Shield account.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onViewRewards, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = SpaceDeep), shape = RoundedCornerShape(8.dp)) {
                    Text("VIEW MY REWARDS", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
