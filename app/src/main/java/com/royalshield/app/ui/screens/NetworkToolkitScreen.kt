package com.royalshield.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.features.networktools.NetworkToolkitViewModel
import com.royalshield.app.ui.components.SpeedTestDialog
import com.royalshield.app.ui.theme.RoyalGold

private data class NetworkTool(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val action: NetworkAction
)

private enum class NetworkAction { HEALTH, SPEED, DNS, DHCP, PING, TRACE, DNS_BENCHMARK, WIFI, PORTS, ROUTER, CAMERAS }

@Composable
fun NetworkToolkitScreen(onBack: () -> Unit) {
    val vm: NetworkToolkitViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    var inputAction by remember { mutableStateOf<NetworkAction?>(null) }
    var confirmationAction by remember { mutableStateOf<NetworkAction?>(null) }
    var pendingHost by remember { mutableStateOf("") }
    var showSpeedTest by remember { mutableStateOf(false) }

    val wifiPermissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }.toTypedArray()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (wifiPermissions.all { grants[it] == true || ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            vm.runWifiScan()
        } else {
            vm.permissionDenied()
        }
    }

    fun startWifiScan() {
        if (wifiPermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            vm.runWifiScan()
        } else {
            permissionLauncher.launch(wifiPermissions)
        }
    }

    val connectivity = listOf(
        NetworkTool("Run health check", "Verify active network, validation, transport, and metering", Icons.Default.HealthAndSafety, NetworkAction.HEALTH),
        NetworkTool("Run speed test", "Measure real backend download, upload, and latency", Icons.Default.Speed, NetworkAction.SPEED),
        NetworkTool("DNS Benchmark", "Compare Cloudflare, Google, and AdGuard DNS-over-HTTPS", Icons.Default.Dns, NetworkAction.DNS_BENCHMARK),
        NetworkTool("Ping a target", "Measure TCP round-trip on ports 443 and 80", Icons.Default.NetworkPing, NetworkAction.PING),
        NetworkTool("Start traceroute", "Trace up to 15 network hops when supported by Android", Icons.Default.Route, NetworkAction.TRACE),
        NetworkTool("DNS Lookup", "Resolve IPv4 and IPv6 addresses using the active network", Icons.Default.Search, NetworkAction.DNS),
        NetworkTool("Network configuration", "View local addresses, gateway, DNS, and interface", Icons.Default.Router, NetworkAction.DHCP),
        NetworkTool("Wi-Fi Scanner", "List nearby SSIDs, signal strength, and security", Icons.Default.WifiFind, NetworkAction.WIFI)
    )
    val security = listOf(
        NetworkTool("Find open ports", "Scan common TCP ports on a target you own", Icons.Default.LockOpen, NetworkAction.PORTS),
        NetworkTool("Audit my router", "Check the local gateway for unsafe administrative services", Icons.Default.Router, NetworkAction.ROUTER),
        NetworkTool("Find possible cameras", "Search your local /24 network for typical camera services", Icons.Default.Videocam, NetworkAction.CAMERAS)
    )

    Box(Modifier.fillMaxSize().background(Color(0xFF080A0F))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Text("NETWORK TOOLKIT", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            ToolSection("VERIFY YOUR INTERNET CONNECTIVITY", connectivity, state.isRunning) { action ->
                when (action) {
                    NetworkAction.HEALTH -> vm.runHealthCheck()
                    NetworkAction.SPEED -> showSpeedTest = true
                    NetworkAction.DNS, NetworkAction.PING, NetworkAction.TRACE, NetworkAction.DNS_BENCHMARK -> inputAction = action
                    NetworkAction.DHCP -> vm.readDhcpInfo()
                    NetworkAction.WIFI -> startWifiScan()
                    else -> Unit
                }
            }
            Spacer(Modifier.height(18.dp))
            ToolSection("IMPROVE YOUR NETWORK SECURITY", security, state.isRunning) { action ->
                when (action) {
                    NetworkAction.PORTS -> inputAction = action
                    NetworkAction.ROUTER, NetworkAction.CAMERAS -> confirmationAction = action
                    else -> Unit
                }
            }
        }

        if (state.isRunning) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = RoyalGold)
                    Spacer(Modifier.height(14.dp))
                    Text("Running secure diagnostic…", color = Color.White)
                }
            }
        }
    }

    if (showSpeedTest) SpeedTestDialog { showSpeedTest = false }

    inputAction?.let { action ->
        val title = when (action) {
            NetworkAction.DNS -> "DNS Lookup"
            NetworkAction.PING -> "TCP Ping"
            NetworkAction.TRACE -> "Traceroute"
            NetworkAction.DNS_BENCHMARK -> "DNS Benchmark"
            NetworkAction.PORTS -> "Authorized Port Scan"
            else -> "Network Target"
        }
        HostInputDialog(title, if (action == NetworkAction.PORTS) "CONTINUE" else "RUN", onDismiss = { inputAction = null }) { host ->
            inputAction = null
            if (action == NetworkAction.PORTS) {
                pendingHost = host
                confirmationAction = NetworkAction.PORTS
            } else {
                when (action) {
                    NetworkAction.DNS -> vm.runDnsLookup(host)
                    NetworkAction.PING -> vm.runPing(host)
                    NetworkAction.TRACE -> vm.runTraceroute(host)
                    NetworkAction.DNS_BENCHMARK -> vm.runDnsBenchmark(host)
                    else -> Unit
                }
            }
        }
    }

    confirmationAction?.let { action ->
        val (title, message) = when (action) {
            NetworkAction.PORTS -> "Confirm authorization" to "I own or have explicit permission to scan $pendingHost. The scan is limited to common TCP ports."
            NetworkAction.ROUTER -> "Audit your router" to "Run a non-exploit configuration audit against this Wi-Fi network's local gateway?"
            NetworkAction.CAMERAS -> "Scan your local network" to "Confirm that you own or administer this network. Royal Shield will check local addresses for typical camera ports without accessing video."
            else -> "Confirm" to "Continue with this diagnostic?"
        }
        AlertDialog(
            onDismissRequest = { confirmationAction = null },
            containerColor = Color(0xFF11151D),
            title = { Text(title, color = RoyalGold) },
            text = { Text(message, color = Color.White) },
            dismissButton = { TextButton(onClick = { confirmationAction = null }) { Text("CANCEL") } },
            confirmButton = {
                TextButton(onClick = {
                    confirmationAction = null
                    when (action) {
                        NetworkAction.PORTS -> vm.runPortScan(pendingHost)
                        NetworkAction.ROUTER -> vm.runRouterAudit()
                        NetworkAction.CAMERAS -> vm.runCameraDiscovery()
                        else -> Unit
                    }
                }) { Text("I CONFIRM", color = RoyalGold) }
            }
        )
    }

    state.result?.let { result ->
        AlertDialog(
            onDismissRequest = vm::clearResult,
            containerColor = Color(0xFF11151D),
            title = { Text(result.title, color = if (result.success) RoyalGold else Color(0xFFFF6B6B)) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) { result.lines.forEach { Text(it, color = Color.White) } }
            },
            confirmButton = { TextButton(onClick = vm::clearResult) { Text("CLOSE", color = RoyalGold) } }
        )
    }
}

@Composable
private fun ToolSection(title: String, tools: List<NetworkTool>, isRunning: Boolean, onAction: (NetworkAction) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(Color(0xFF151B24), RoundedCornerShape(18.dp))
            .border(1.dp, RoyalGold.copy(alpha = .22f), RoundedCornerShape(18.dp)).padding(16.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
        Spacer(Modifier.height(14.dp))
        tools.forEach { tool ->
            Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF252D39), RoundedCornerShape(10.dp))
                        .clickable(enabled = !isRunning) { onAction(tool.action) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(tool.icon, null, tint = RoyalGold, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(tool.title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                }
                Text(tool.description, color = Color.White.copy(alpha = .62f), fontSize = 12.sp, modifier = Modifier.padding(start = 14.dp, top = 7.dp))
            }
        }
    }
}

@Composable
private fun HostInputDialog(title: String, actionLabel: String, onDismiss: () -> Unit, onRun: (String) -> Unit) {
    var host by remember(title) { mutableStateOf("example.com") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF11151D),
        title = { Text(title, color = RoyalGold) },
        text = {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it.take(253) },
                label = { Text("Hostname or IPv4") },
                supportingText = { Text("Do not enter a URL path or credentials") },
                singleLine = true
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
        confirmButton = { TextButton(onClick = { onRun(host.trim()) }, enabled = host.isNotBlank()) { Text(actionLabel, color = RoyalGold) } }
    )
}
