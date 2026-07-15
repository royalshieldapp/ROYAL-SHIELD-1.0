package com.royalshield.app.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.R
import com.royalshield.app.managers.BillingManager
import com.royalshield.app.vpn.VpnConfigurationException
import com.royalshield.app.vpn.VpnManager
import com.royalshield.app.vpn.VpnProfileRepository
import com.royalshield.app.vpn.VpnServerProfile
import com.royalshield.app.vpn.VpnState
import kotlinx.coroutines.launch

data class VpnServer(val name: String, val countryCode: String, val flag: String, val configId: String)

private val AurumBlack = Color(0xFF050300)
private val AurumPanel = Color(0xFF0E0903)
private val AurumPanelSoft = Color(0xFF171006)
private val AurumStroke = Color(0xFF5D3F10)
private val AurumGold = Color(0xFFFFC43B)
private val AurumGoldDeep = Color(0xFFB8861D)
private val AurumGoldSoft = Color(0xFFE8C47A)
private val AurumMuted = Color(0xFFB49A76)
private val AurumWhite = Color(0xFFFFF8E6)
private val AurumDanger = Color(0xFFFF6157)
private val AurumGreen = Color(0xFF7AF6B2)

private fun VpnServerProfile.toVpnServer(): VpnServer {
    val code = countryCode.ifBlank { id.take(2).uppercase() }
    return VpnServer(
        name = name,
        countryCode = code,
        flag = code,
        configId = id
    )
}

private val staticServers = listOf(
    VpnServer("Zurich", "CH", "CH", "ch-zurich"),
    VpnServer("New York", "US", "US", "us-east"),
    VpnServer("London", "UK", "UK", "uk-london"),
    VpnServer("Frankfurt", "DE", "DE", "de-frankfurt"),
    VpnServer("Tokyo", "JP", "JP", "jp-tokyo")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnScreen(
    onBack: () -> Unit = {},
    billingManager: BillingManager? = null,
    onNavigateToPremium: () -> Unit = {}
) {
    val context = LocalContext.current
    val vpnManager = remember { VpnManager.getInstance(context) }
    val vpnState by vpnManager.connectionState.collectAsState()
    val vpnStats by vpnManager.bytesTransferred.collectAsState()

    val hasPremiumAccess = billingManager?.hasPremiumAccess?.collectAsState()?.value ?: false
    val currentProduct = billingManager?.currentProduct?.collectAsState()?.value
    val hasVpnAccess = hasPremiumAccess && (
        currentProduct == BillingManager.PRODUCT_GOLD ||
            currentProduct == BillingManager.PRODUCT_ULTIMATE
        )

    val vpnProfileRepository = remember { VpnProfileRepository() }
    val scope = rememberCoroutineScope()
    var isFetchingConfig by remember { mutableStateOf(false) }
    var vpnNotice by remember { mutableStateOf<String?>(null) }
    var backendAvailable by remember { mutableStateOf<Boolean?>(null) }
    var serverOptions by remember { mutableStateOf(staticServers) }
    var selectedServer by remember { mutableStateOf(staticServers.first()) }
    var showServerSelector by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(VpnTab.WorldMap) }

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

    Surface(color = AurumBlack, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF201103), AurumBlack, Color.Black),
                        center = Offset(900f, 220f),
                        radius = 1100f
                    )
                )
        ) {
            if (!hasVpnAccess) {
                AurumVpnPaywall(onNavigateToPremium)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 23.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AurumHeader(onBack = onBack)
                    Spacer(modifier = Modifier.height(26.dp))
                    AurumSegmentedTabs(selectedTab = selectedTab, onSelected = { selectedTab = it })
                    Spacer(modifier = Modifier.height(30.dp))
                    if (selectedTab == VpnTab.WorldMap) {
                        AurumWorldMapCard(selectedServer = selectedServer)
                    } else {
                        AurumServerListCard(
                            serverOptions = serverOptions,
                            selectedServer = selectedServer,
                            onSelect = { selectedServer = it }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    AurumSelectedServerCard(
                        selectedServer = selectedServer,
                        vpnState = vpnState,
                        enabled = backendAvailable == true,
                        onClick = { showServerSelector = true }
                    )
                    Spacer(modifier = Modifier.height(42.dp))
                    AurumPowerButton(
                        vpnState = vpnState,
                        enabled = backendAvailable == true && !isFetchingConfig,
                        onClick = {
                            when (vpnState) {
                                is VpnState.Disconnected,
                                is VpnState.Error,
                                is VpnState.MissingConfig -> requestVpnConnection()
                                is VpnState.Connected -> vpnManager.disconnect()
                                is VpnState.PermissionRequired -> {
                                    permissionLauncher.launch((vpnState as VpnState.PermissionRequired).intent)
                                }
                                else -> Unit
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(25.dp))
                    Text(
                        text = if (vpnState is VpnState.Connected) "SECURE TUNNEL ACTIVE" else "TAP TO CONNECT",
                        color = AurumMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 5.sp
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = vpnStateLabel(vpnState),
                        color = vpnStateColor(vpnState),
                        fontSize = 27.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    vpnNotice?.let { notice ->
                        Spacer(modifier = Modifier.height(13.dp))
                        Text(
                            text = notice,
                            color = if (vpnState is VpnState.MissingConfig) AurumGoldSoft else AurumDanger,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(34.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AurumMetricCard(
                            label = "DOWN",
                            value = if (vpnState is VpnState.Connected) formatBytes(vpnStats.bytesReceived) else "0 B",
                            modifier = Modifier.weight(1f)
                        )
                        AurumMetricCard(
                            label = "UP",
                            value = if (vpnState is VpnState.Connected) formatBytes(vpnStats.bytesSent) else "0 B",
                            modifier = Modifier.weight(1f)
                        )
                        AurumMetricCard(
                            label = "PING",
                            value = if (vpnState is VpnState.Connected) "28 ms" else "--",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(96.dp))
                }
            }

            if (showServerSelector) {
                ModalBottomSheet(
                    onDismissRequest = { showServerSelector = false },
                    containerColor = AurumPanel,
                    contentColor = AurumWhite
                ) {
                    Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
                        Text(
                            "Select Server",
                            color = AurumGoldSoft,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        serverOptions.forEach { server ->
                            AurumServerRow(
                                server = server,
                                selected = server.configId == selectedServer.configId,
                                onClick = {
                                    selectedServer = server
                                    showServerSelector = false
                                }
                            )
                            Divider(color = AurumStroke.copy(alpha = 0.45f))
                        }
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }
            }
        }
    }
}

private enum class VpnTab { WorldMap, ServerList }

@Composable
private fun AurumHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFFE796), AurumGoldDeep))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = AurumBlack, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Aurum VPN",
                color = AurumGoldSoft,
                fontSize = 26.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
            Text(
                "PREMIUM - TIER I",
                color = AurumMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 5.sp
            )
        }
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .border(1.dp, AurumStroke.copy(alpha = 0.85f), CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Back", tint = AurumGold, modifier = Modifier.size(25.dp))
        }
    }
}

@Composable
private fun AurumSegmentedTabs(selectedTab: VpnTab, onSelected: (VpnTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF0A0602))
            .border(1.dp, AurumStroke.copy(alpha = 0.62f), RoundedCornerShape(32.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AurumTabButton(
            text = "WORLD MAP",
            selected = selectedTab == VpnTab.WorldMap,
            onClick = { onSelected(VpnTab.WorldMap) },
            modifier = Modifier.weight(1f)
        )
        AurumTabButton(
            text = "SERVER LIST",
            selected = selectedTab == VpnTab.ServerList,
            onClick = { onSelected(VpnTab.ServerList) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AurumTabButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(27.dp))
            .background(
                if (selected) Brush.linearGradient(listOf(Color(0xFFFFE895), Color(0xFFD4A236)))
                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.Black else AurumMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun AurumWorldMapCard(selectedServer: VpnServer) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(308.dp)
            .clip(RoundedCornerShape(31.dp))
            .background(Color(0xFF080501))
            .border(1.dp, AurumStroke.copy(alpha = 0.86f), RoundedCornerShape(31.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(Color(0xFF100A04).copy(alpha = 0.45f))

            val continent = Color(0xFF251B11).copy(alpha = 0.72f)
            drawOval(continent, topLeft = Offset(w * 0.12f, h * 0.18f), size = Size(w * 0.29f, h * 0.22f))
            drawOval(continent, topLeft = Offset(w * 0.30f, h * 0.35f), size = Size(w * 0.15f, h * 0.34f))
            drawOval(continent, topLeft = Offset(w * 0.49f, h * 0.22f), size = Size(w * 0.14f, h * 0.17f))
            drawOval(continent, topLeft = Offset(w * 0.47f, h * 0.38f), size = Size(w * 0.17f, h * 0.36f))
            drawOval(continent, topLeft = Offset(w * 0.63f, h * 0.21f), size = Size(w * 0.28f, h * 0.24f))
            drawOval(continent, topLeft = Offset(w * 0.73f, h * 0.47f), size = Size(w * 0.12f, h * 0.18f))
            drawOval(continent, topLeft = Offset(w * 0.82f, h * 0.62f), size = Size(w * 0.08f, h * 0.10f))

            val routeColor = AurumGold.copy(alpha = 0.13f)
            drawLine(routeColor, Offset(w * 0.52f, h * 0.29f), Offset(w * 0.36f, h * 0.26f), 1.3f)
            drawLine(routeColor, Offset(w * 0.52f, h * 0.29f), Offset(w * 0.68f, h * 0.34f), 1.3f)
            drawLine(routeColor, Offset(w * 0.52f, h * 0.29f), Offset(w * 0.84f, h * 0.31f), 1.3f)

            val nodes = listOf(
                Offset(w * 0.52f, h * 0.29f), Offset(w * 0.34f, h * 0.26f), Offset(w * 0.20f, h * 0.33f),
                Offset(w * 0.38f, h * 0.72f), Offset(w * 0.60f, h * 0.74f), Offset(w * 0.68f, h * 0.40f),
                Offset(w * 0.84f, h * 0.31f), Offset(w * 0.80f, h * 0.52f), Offset(w * 0.90f, h * 0.79f),
                Offset(w * 0.73f, h * 0.45f), Offset(w * 0.88f, h * 0.32f)
            )
            nodes.forEachIndexed { index, point ->
                drawCircle(AurumGold.copy(alpha = 0.14f), radius = if (index == 0) 14f else 10f, center = point)
                drawCircle(if (index == 0) AurumWhite else AurumGold, radius = if (index == 0) 5.8f else 4.1f, center = point)
            }
        }
        AurumLegend(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 8.dp)
        )
        Text(
            selectedServer.name.uppercase(),
            color = AurumGoldSoft.copy(alpha = 0.42f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 13.dp, end = 13.dp)
        )
    }
}

@Composable
private fun AurumLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, AurumStroke.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        AurumLegendItem(AurumWhite, "SELECTED")
        AurumLegendItem(AurumGoldSoft, "ELITE")
        AurumLegendItem(AurumGoldDeep, "STANDARD")
    }
}

@Composable
private fun AurumLegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(7.dp))
        Text(text, color = AurumMuted, fontSize = 12.sp, letterSpacing = 2.sp)
    }
}

@Composable
private fun AurumSelectedServerCard(
    selectedServer: VpnServer,
    vpnState: VpnState,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(122.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(AurumPanel)
            .border(1.dp, AurumStroke.copy(alpha = 0.9f), RoundedCornerShape(23.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 25.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1B130C)),
            contentAlignment = Alignment.Center
        ) {
            Text(selectedServer.countryCode, color = AurumWhite, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                selectedServer.name,
                color = AurumWhite,
                fontSize = 25.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                "${selectedServer.countryCode} - ${if (enabled) "Elite node" else "Node locked"}",
                color = AurumMuted,
                fontSize = 15.sp,
                maxLines = 1
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (vpnState is VpnState.Connected) "28ms" else "--",
                color = AurumGold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text("TAP TO CHANGE", color = AurumMuted, fontSize = 12.sp, letterSpacing = 3.sp)
        }
    }
}

@Composable
private fun AurumPowerButton(vpnState: VpnState, enabled: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "vpn-power")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = if (vpnState is VpnState.Connected || vpnState is VpnState.Connecting) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "power-pulse"
    )
    val ringColor = vpnStateColor(vpnState)

    Box(
        modifier = Modifier
            .size(216.dp)
            .scale(pulse)
            .clip(CircleShape)
            .clickable(enabled = enabled && vpnState !is VpnState.Connecting && vpnState !is VpnState.Disconnecting, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            drawCircle(Color(0xFF1A1108), radius = size.minDimension / 2f)
            drawCircle(AurumBlack, radius = size.minDimension * 0.43f)
            drawCircle(
                color = ringColor.copy(alpha = if (vpnState is VpnState.Connected) 0.45f else 0.20f),
                radius = size.minDimension * 0.47f,
                style = Stroke(width = 3.5f)
            )
            drawArc(
                color = ringColor,
                startAngle = -220f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(center.x - 33f, center.y - 33f),
                size = Size(66f, 66f),
                style = Stroke(width = 5.5f, cap = StrokeCap.Round)
            )
        }
        Icon(
            Icons.Default.PowerSettingsNew,
            contentDescription = "Toggle VPN",
            tint = ringColor,
            modifier = Modifier.size(60.dp)
        )
    }
}

@Composable
private fun AurumMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(101.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AurumPanel)
            .border(1.dp, AurumStroke.copy(alpha = 0.62f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AurumMuted, fontSize = 13.sp, letterSpacing = 4.sp, maxLines = 1)
        Text(value, color = AurumWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun AurumServerListCard(
    serverOptions: List<VpnServer>,
    selectedServer: VpnServer,
    onSelect: (VpnServer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(31.dp))
            .background(AurumPanel)
            .border(1.dp, AurumStroke.copy(alpha = 0.86f), RoundedCornerShape(31.dp))
            .padding(18.dp)
    ) {
        serverOptions.forEach { server ->
            AurumServerRow(
                server = server,
                selected = server.configId == selectedServer.configId,
                onClick = { onSelect(server) }
            )
            if (server != serverOptions.last()) Divider(color = AurumStroke.copy(alpha = 0.35f))
        }
    }
}

@Composable
private fun AurumServerRow(server: VpnServer, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1B130C)),
            contentAlignment = Alignment.Center
        ) {
            Text(server.countryCode, color = AurumWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(server.name, color = AurumWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${server.countryCode} - Elite node", color = AurumMuted, fontSize = 12.sp, maxLines = 1)
        }
        if (selected) Icon(Icons.Default.Check, contentDescription = "Selected", tint = AurumGold)
    }
}

@Composable
private fun AurumVpnPaywall(onNavigateToPremium: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(118.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFFE796), AurumGoldDeep))),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.emblem_golden_screen_subscription),
                contentDescription = null,
                modifier = Modifier.size(82.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.height(25.dp))
        Text("Aurum VPN", color = AurumGoldSoft, fontSize = 32.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        Text(
            "Exclusive secure tunnel for premium members.",
            color = AurumMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(31.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFFE895), Color(0xFFD4A236))))
                .clickable(onClick = onNavigateToPremium),
            contentAlignment = Alignment.Center
        ) {
            Text("UPGRADE PLAN", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
    }
}

private fun vpnStateLabel(vpnState: VpnState): String {
    return when (vpnState) {
        is VpnState.Connected -> "Connected"
        is VpnState.Connecting -> "Connecting"
        is VpnState.Disconnecting -> "Disconnecting"
        is VpnState.Error -> "Connection error"
        is VpnState.MissingConfig -> "Not configured"
        is VpnState.PermissionRequired -> "Permission required"
        is VpnState.PremiumRequired -> "Premium required"
        is VpnState.Disconnected -> "Not connected"
    }
}

private fun vpnStateColor(vpnState: VpnState): Color {
    return when (vpnState) {
        is VpnState.Connected -> AurumGreen
        is VpnState.Connecting,
        is VpnState.Disconnecting,
        is VpnState.PermissionRequired -> AurumGold
        is VpnState.Error -> AurumDanger
        else -> AurumGold
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
