package com.royalshield.app.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.R
import com.royalshield.app.vpn.VpnManager
import com.royalshield.app.vpn.VpnState
import com.royalshield.app.vpn.VpnStats
import com.royalshield.app.vpn.VpnProfileRepository
import com.royalshield.app.vpn.VpnConfigurationException
import com.royalshield.app.vpn.VpnServerProfile
import kotlinx.coroutines.launch

data class VpnServer(val name: String, val countryCode: String, val flag: String, val configId: String)

private fun VpnServerProfile.toVpnServer(): VpnServer {
    val code = countryCode.ifBlank { id.take(2).uppercase() }
    return VpnServer(
        name = name,
        countryCode = code,
        flag = code,
        configId = id
    )
}

val staticServers = listOf(
    VpnServer("US East (New York)", "US", "US", "US_EAST"),
    VpnServer("US West (Los Angeles)", "US", "US", "US_WEST"),
    VpnServer("UK London", "UK", "UK", "UK_LON"),
    VpnServer("Germany Frankfurt", "DE", "DE", "DE_FRA"),
    VpnServer("Japan Tokyo", "JP", "JP", "JP_TOK")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnScreen(
    onBack: () -> Unit = {},
    billingManager: com.royalshield.app.managers.BillingManager? = null,
    onNavigateToPremium: () -> Unit = {}
) {
    val context = LocalContext.current
    val vpnManager = remember { VpnManager.getInstance(context) }
    val vpnState by vpnManager.connectionState.collectAsState()
    val vpnStats by vpnManager.bytesTransferred.collectAsState()
    
    // Check subscription status
    val hasPremiumAccess = billingManager?.hasPremiumAccess?.collectAsState()?.value ?: false
    val currentProduct = billingManager?.currentProduct?.collectAsState()?.value
    
    val hasVpnAccess = hasPremiumAccess && (
        currentProduct == "gold_pro_monthly" || 
        currentProduct == "royal_business_monthly" || 
        currentProduct == "royal_enterprise_monthly" || 
        currentProduct == "royal_ultimate_lifetime"
    )
    
    val vpnProfileRepository = remember { VpnProfileRepository() }
    val scope = rememberCoroutineScope()
    var isFetchingConfig by remember { mutableStateOf(false) }
    var vpnNotice by remember { mutableStateOf<String?>(null) }
    var backendAvailable by remember { mutableStateOf<Boolean?>(null) }
    var serverOptions by remember { mutableStateOf(staticServers) }

    var selectedServer by remember { mutableStateOf(staticServers.first()) }
    var showServerSelector by remember { mutableStateOf(false) }

    fun handleVpnFailure(error: Throwable) {
        val message = error.message ?: "VPN request failed"
        vpnNotice = message
        if (error is VpnConfigurationException && error.code.contains("NOT_CONFIGURED")) {
            vpnManager.setMissingConfig()
        } else {
            vpnManager.setError(message)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                isFetchingConfig = true
                val res = vpnProfileRepository.getVpnConfig(selectedServer.configId)
                isFetchingConfig = false
                res.onSuccess { config ->
                    vpnManager.connect(config)
                }.onFailure { error ->
                    handleVpnFailure(error)
                }
            }
        }
    }

    fun requestVpnConnection() {
        scope.launch {
            isFetchingConfig = true
            val res = vpnProfileRepository.getVpnConfig(selectedServer.configId)
            isFetchingConfig = false
            res.onSuccess { config ->
                vpnNotice = null
                val intent = vpnManager.connect(config)
                if (intent != null) permissionLauncher.launch(intent)
            }.onFailure { error ->
                handleVpnFailure(error)
            }
        }
    }

    LaunchedEffect(hasVpnAccess) {
        if (!hasVpnAccess) return@LaunchedEffect

        isFetchingConfig = true
        vpnProfileRepository.getVpnStatus()
            .onSuccess { status ->
                backendAvailable = status.available
                vpnNotice = if (status.available) null else status.message
                if (!status.available) vpnManager.setMissingConfig()
            }
            .onFailure { error ->
                backendAvailable = false
                handleVpnFailure(error)
            }

        vpnProfileRepository.getVpnServers()
            .onSuccess { servers ->
                val mappedServers = servers.map { it.toVpnServer() }
                if (mappedServers.isNotEmpty()) {
                    serverOptions = mappedServers
                    if (serverOptions.none { it.configId == selectedServer.configId }) {
                        selectedServer = mappedServers.first()
                    }
                }
            }
        isFetchingConfig = false
    }
    
    // Colors
    val neonGreen = Color(0xFF00FF00)
    val redWarning = Color(0xFFFF3B30)
    val surfaceDark = Color(0xFF1C1C1E)
    val backgroundDark = Color(0xFF000000)
    val gold = Color(0xFFFFD700)
    
    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (vpnState is VpnState.Connected || vpnState is VpnState.Connecting) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (vpnState is VpnState.Connected) 0f else 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("VPN PANEL", color = Color.White, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundDark
                )
            )
        },
        containerColor = backgroundDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!hasVpnAccess) {
                VpnPaywall(onNavigateToPremium, gold, surfaceDark)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Server Selector
                    ServerSelector(
                        selectedServer = selectedServer,
                        enabled = backendAvailable == true && (vpnState is VpnState.Disconnected || vpnState is VpnState.Error),
                        onClick = { showServerSelector = true },
                        surfaceColor = surfaceDark,
                        textColor = Color.White
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Connection Status Text
                    val statusText = when(vpnState) {
                        is VpnState.Disconnected -> "DISCONNECTED"
                        is VpnState.Connecting -> "CONNECTING..."
                        is VpnState.Connected -> "CONNECTED"
                        is VpnState.Disconnecting -> "DISCONNECTING..."
                        is VpnState.Error -> "CONNECTION ERROR"
                        is VpnState.PermissionRequired -> "PERMISSION REQUIRED"
                        is VpnState.MissingConfig -> "VPN NOT CONFIGURED"
                        is VpnState.PremiumRequired -> "PREMIUM REQUIRED"
                    }
                    val statusColor = when(vpnState) {
                        is VpnState.Connected -> neonGreen
                        is VpnState.Error -> redWarning
                        is VpnState.Connecting, is VpnState.Disconnecting -> gold
                        else -> Color.Gray
                    }
                    
                    Text(
                        text = statusText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        letterSpacing = 2.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Main Connection Button
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clickable(
                                enabled = backendAvailable == true &&
                                    vpnState !is VpnState.Connecting &&
                                    vpnState !is VpnState.Disconnecting &&
                                    !isFetchingConfig
                            ) {
                                when(vpnState) {
                                    is VpnState.Disconnected, is VpnState.Error, is VpnState.MissingConfig -> requestVpnConnection()
                                    is VpnState.Connected -> vpnManager.disconnect()
                                    is VpnState.PermissionRequired -> {
                                        permissionLauncher.launch((vpnState as VpnState.PermissionRequired).intent)
                                    }
                                    else -> {}
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulse rings
                        if (vpnState is VpnState.Connected || vpnState is VpnState.Connecting) {
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .scale(pulseScale)
                                    .border(2.dp, statusColor.copy(alpha = pulseAlpha), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .scale(pulseScale)
                                    .border(1.dp, statusColor.copy(alpha = pulseAlpha / 2), CircleShape)
                            )
                        }
                        
                        // Main Button Surface
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(surfaceDark.copy(alpha = 0.8f), backgroundDark)
                                    )
                                )
                                .border(3.dp, statusColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Toggle VPN",
                                modifier = Modifier.size(64.dp),
                                tint = statusColor
                            )
                        }
                    }

                    vpnNotice?.let { notice ->
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = notice,
                            color = if (vpnState is VpnState.MissingConfig) gold else redWarning,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Stats section (Ping, Up, Down)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val pingStr = if (vpnState is VpnState.Connected) "32 ms" else "--"
                        StatItem(icon = Icons.Default.NetworkCheck, label = "Ping", value = pingStr, color = neonGreen)
                        
                        val upStr = if (vpnState is VpnState.Connected) formatBytes(vpnStats.bytesSent) else "--"
                        StatItem(icon = Icons.Default.KeyboardArrowUp, label = "Upload", value = upStr, color = gold)
                        
                        val downStr = if (vpnState is VpnState.Connected) formatBytes(vpnStats.bytesReceived) else "--"
                        StatItem(icon = Icons.Default.KeyboardArrowDown, label = "Download", value = downStr, color = neonGreen)
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        
        // Server Selection Bottom Sheet
        if (showServerSelector) {
            ModalBottomSheet(
                onDismissRequest = { showServerSelector = false },
                containerColor = surfaceDark
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Select Server",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    serverOptions.forEach { server ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedServer = server
                                    showServerSelector = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(server.flag, fontSize = 24.sp, modifier = Modifier.padding(end = 16.dp))
                            Text(server.name, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (server.configId == selectedServer.configId) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = neonGreen)
                            }
                        }
                        Divider(color = Color.DarkGray)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ServerSelector(
    selectedServer: VpnServer,
    enabled: Boolean,
    onClick: () -> Unit,
    surfaceColor: Color,
    textColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selectedServer.flag, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Current Server", fontSize = 12.sp, color = Color.Gray)
                Text(selectedServer.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Change Server", tint = Color.Gray)
        }
    }
}

@Composable
fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun VpnPaywall(onNavigateToPremium: () -> Unit, gold: Color, surface: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            gold.copy(alpha = 0.22f),
                            surface,
                            Color.Black
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            gold.copy(alpha = 0.95f),
                            gold.copy(alpha = 0.35f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.emblem_golden_screen_subscription),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("VPN Premium", fontSize = 28.sp, fontWeight = FontWeight.Black, color = gold)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Exclusive access for Pro Defender subscribers and above",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Unlock VPN with:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                InfoItem("Pro Defender", "$9.99/month")
                InfoItem("Business Suit", "$29.99/month")
                InfoItem("Enterprise Core", "$99.99/month")
                InfoItem("Ultimate Pack", "$39.49 (Lifetime)")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .clickable { onNavigateToPremium() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.gold_pill_button),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            Text("UPGRADE PLAN", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

