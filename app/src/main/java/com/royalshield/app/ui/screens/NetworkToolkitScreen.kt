package com.royalshield.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.features.networktools.NetworkToolkitViewModel
import com.royalshield.app.ui.components.SpeedTestDialog
import com.royalshield.app.ui.theme.RoyalGold

private data class NetworkTool(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val action: NetworkAction,
    val enabled: Boolean = true,
)

private enum class NetworkAction { HEALTH, SPEED, DNS, DHCP, PING, TRACE, DNS_BENCHMARK, WIFI, PORTS, ROUTER, CAMERAS }

@Composable
fun NetworkToolkitScreen(onBack: () -> Unit) {
    val vm: NetworkToolkitViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    var showSpeedTest by remember { mutableStateOf(false) }
    var showDnsInput by remember { mutableStateOf(false) }
    var showPingInput by remember { mutableStateOf(false) }
    val connectivity = listOf(
        NetworkTool("Run health check", "Verify active network and internet validation", Icons.Default.HealthAndSafety, NetworkAction.HEALTH),
        NetworkTool("Run speed test", "Calculate download, upload, and latency", Icons.Default.Speed, NetworkAction.SPEED),
        NetworkTool("DNS Benchmark", "Requires a controlled resolver test service", Icons.Default.Dns, NetworkAction.DNS_BENCHMARK, false),
        NetworkTool("Ping a target", "Measure HTTPS round-trip connectivity", Icons.Default.NetworkPing, NetworkAction.PING),
        NetworkTool("Start traceroute", "Backend diagnostic worker required", Icons.Default.Route, NetworkAction.TRACE, false),
        NetworkTool("DNS Lookup", "Resolve a hostname using the active network", Icons.Default.Search, NetworkAction.DNS),
        NetworkTool("DHCP Information", "View local IP, gateway, and DNS servers", Icons.Default.Router, NetworkAction.DHCP),
        NetworkTool("Wi-Fi Scanner", "Runtime permission and real scanner pending", Icons.Default.WifiFind, NetworkAction.WIFI, false),
    )
    val security = listOf(
        NetworkTool("Find open ports", "Only available for authorized targets", Icons.Default.LockOpen, NetworkAction.PORTS, false),
        NetworkTool("Test router vulnerabilities", "Requires explicit router ownership confirmation", Icons.Default.Router, NetworkAction.ROUTER, false),
        NetworkTool("Find hidden cameras", "Requires a consent-based local network scan", Icons.Default.Videocam, NetworkAction.CAMERAS, false),
    )

    Box(Modifier.fillMaxSize().background(Color(0xFF080A0F))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Text("NETWORK TOOLKIT", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            ToolSection("VERIFY YOUR INTERNET CONNECTIVITY", connectivity) { action ->
                when (action) {
                    NetworkAction.HEALTH -> vm.runHealthCheck()
                    NetworkAction.SPEED -> showSpeedTest = true
                    NetworkAction.DNS -> showDnsInput = true
                    NetworkAction.DHCP -> vm.readDhcpInfo()
                    NetworkAction.PING -> showPingInput = true
                    else -> Unit
                }
            }
            Spacer(Modifier.height(18.dp))
            ToolSection("IMPROVE YOUR NETWORK SECURITY", security) { }
        }
    }

    if (showSpeedTest) SpeedTestDialog { showSpeedTest = false }
    if (showDnsInput) DnsInputDialog(onDismiss = { showDnsInput = false }, onRun = { showDnsInput = false; vm.runDnsLookup(it) })
    if (showPingInput) HostInputDialog("TCP Ping", "PING", onDismiss = { showPingInput = false }, onRun = { showPingInput = false; vm.runPing(it) })
    state.result?.let { result ->
        AlertDialog(
            onDismissRequest = vm::clearResult,
            containerColor = Color(0xFF11151D),
            title = { Text(result.title, color = RoyalGold) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { result.lines.forEach { Text(it, color = Color.White) } } },
            confirmButton = { TextButton(onClick = vm::clearResult) { Text("CLOSE", color = RoyalGold) } },
        )
    }
}

@Composable
private fun ToolSection(title: String, tools: List<NetworkTool>, onAction: (NetworkAction) -> Unit) {
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
                        .clickable(enabled = tool.enabled) { onAction(tool.action) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(tool.icon, null, tint = if (tool.enabled) RoyalGold else Color.Gray, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(tool.title, color = if (tool.enabled) Color.White else Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                }
                Text(tool.description, color = Color.White.copy(alpha = .62f), fontSize = 12.sp, modifier = Modifier.padding(start = 14.dp, top = 7.dp))
            }
        }
    }
}

@Composable
private fun DnsInputDialog(onDismiss: () -> Unit, onRun: (String) -> Unit) {
    HostInputDialog("DNS Lookup", "LOOK UP", onDismiss, onRun)
}

@Composable
private fun HostInputDialog(title: String, actionLabel: String, onDismiss: () -> Unit, onRun: (String) -> Unit) {
    var host by remember { mutableStateOf("example.com") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF11151D),
        title = { Text(title, color = RoyalGold) },
        text = { OutlinedTextField(value = host, onValueChange = { host = it.take(253) }, label = { Text("Hostname") }, singleLine = true) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
        confirmButton = { TextButton(onClick = { onRun(host) }, enabled = host.isNotBlank()) { Text(actionLabel, color = RoyalGold) } },
    )
}
