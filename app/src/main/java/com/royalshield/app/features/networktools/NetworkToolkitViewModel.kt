package com.royalshield.app.features.networktools

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

data class NetworkToolResult(val title: String, val lines: List<String>, val success: Boolean)

private object NetworkDiagnosticsRepository {
    fun healthCheck(context: Context): NetworkToolResult {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork
            ?: return NetworkToolResult("Health Check", listOf("No active network"), false)
        val capabilities = manager.getNetworkCapabilities(network)
            ?: return NetworkToolResult("Health Check", listOf("Network capabilities unavailable"), false)
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val transport = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Other"
        }
        return NetworkToolResult(
            "Health Check",
            listOf("Transport: $transport", "Internet capability: ${if (hasInternet) "Yes" else "No"}", "Validated connection: ${if (validated) "Yes" else "No"}"),
            validated,
        )
    }

    fun dhcpInfo(context: Context): NetworkToolResult {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifi.dhcpInfo ?: return NetworkToolResult("DHCP Info", listOf("DHCP information unavailable"), false)
        fun ipv4(value: Int): String = listOf(value and 0xff, value shr 8 and 0xff, value shr 16 and 0xff, value shr 24 and 0xff).joinToString(".")
        return NetworkToolResult(
            "DHCP Info",
            listOf("Device IP: ${ipv4(info.ipAddress)}", "Gateway: ${ipv4(info.gateway)}", "DNS 1: ${ipv4(info.dns1)}", "DNS 2: ${ipv4(info.dns2)}"),
            info.ipAddress != 0,
        )
    }

    suspend fun dnsLookup(host: String): NetworkToolResult = withContext(Dispatchers.IO) {
        val cleanHost = validatedHost(host)
        val addresses = InetAddress.getAllByName(cleanHost).map { it.hostAddress ?: "Unknown" }.distinct()
        NetworkToolResult("DNS Lookup", listOf("Host: $cleanHost") + addresses.map { "Address: $it" }, addresses.isNotEmpty())
    }

    suspend fun ping(host: String): NetworkToolResult = withContext(Dispatchers.IO) {
        val cleanHost = validatedHost(host)
        val samples = mutableListOf<Long>()
        repeat(4) {
            runCatching {
                measureTimeMillis {
                    Socket().use { socket -> socket.connect(InetSocketAddress(cleanHost, 443), 3_000) }
                }
            }.getOrNull()?.let(samples::add)
        }
        if (samples.isEmpty()) {
            NetworkToolResult("TCP Ping", listOf("Host: $cleanHost", "No HTTPS response received"), false)
        } else {
            NetworkToolResult(
                "TCP Ping",
                listOf("Host: $cleanHost", "Port: 443", "Average: ${samples.average().toInt()} ms", "Successful attempts: ${samples.size}/4"),
                true,
            )
        }
    }

    private fun validatedHost(host: String): String {
        val cleanHost = host.trim().removePrefix("https://").removePrefix("http://").substringBefore('/')
        require(cleanHost.matches(Regex("^[A-Za-z0-9.-]{1,253}$"))) { "Enter a valid hostname" }
        return cleanHost
    }
}

data class NetworkToolkitUiState(val isRunning: Boolean = false, val result: NetworkToolResult? = null)

class NetworkToolkitViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(NetworkToolkitUiState())
    val uiState: StateFlow<NetworkToolkitUiState> = _uiState.asStateFlow()

    fun runHealthCheck() = runLocal { NetworkDiagnosticsRepository.healthCheck(getApplication()) }
    fun readDhcpInfo() = runLocal { NetworkDiagnosticsRepository.dhcpInfo(getApplication()) }

    fun runDnsLookup(host: String) {
        runRemote("DNS Lookup") { NetworkDiagnosticsRepository.dnsLookup(host) }
    }

    fun runPing(host: String) {
        runRemote("TCP Ping") { NetworkDiagnosticsRepository.ping(host) }
    }

    private fun runRemote(title: String, block: suspend () -> NetworkToolResult) {
        viewModelScope.launch {
            _uiState.value = NetworkToolkitUiState(isRunning = true)
            _uiState.value = runCatching { block() }
                .fold(
                    onSuccess = { NetworkToolkitUiState(result = it) },
                    onFailure = { NetworkToolkitUiState(result = NetworkToolResult(title, listOf(it.message ?: "Diagnostic failed"), false)) },
                )
        }
    }

    fun clearResult() { _uiState.value = NetworkToolkitUiState() }

    private fun runLocal(block: () -> NetworkToolResult) {
        viewModelScope.launch {
            _uiState.value = NetworkToolkitUiState(isRunning = true)
            _uiState.value = NetworkToolkitUiState(result = block())
        }
    }
}
