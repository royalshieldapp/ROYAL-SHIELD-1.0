package com.royalshield.app.features.networktools

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.system.measureTimeMillis

data class NetworkToolResult(val title: String, val lines: List<String>, val success: Boolean)

private object NetworkDiagnosticsRepository {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val commonPorts = linkedMapOf(
        20 to "FTP data", 21 to "FTP", 22 to "SSH", 23 to "Telnet", 25 to "SMTP",
        53 to "DNS", 80 to "HTTP", 110 to "POP3", 143 to "IMAP", 443 to "HTTPS",
        445 to "SMB", 554 to "RTSP", 587 to "SMTP TLS", 631 to "IPP", 993 to "IMAPS",
        995 to "POP3S", 1883 to "MQTT", 3389 to "RDP", 5000 to "Web service",
        8000 to "Camera/web", 8080 to "HTTP alternate", 8443 to "HTTPS alternate", 8899 to "Camera service"
    )

    fun healthCheck(context: Context): NetworkToolResult {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork
            ?: return NetworkToolResult("Health Check", listOf("No hay una red activa"), false)
        val capabilities = manager.getNetworkCapabilities(network)
            ?: return NetworkToolResult("Health Check", listOf("No se pudieron leer las capacidades de red"), false)
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val metered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val transport = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Celular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Otro"
        }
        return NetworkToolResult(
            "Health Check",
            listOf(
                "Transporte: $transport",
                "Capacidad de Internet: ${yesNo(hasInternet)}",
                "Conexión validada por Android: ${yesNo(validated)}",
                "Red de consumo medido: ${yesNo(metered)}"
            ),
            validated
        )
    }

    fun dhcpInfo(context: Context): NetworkToolResult {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork
            ?: return NetworkToolResult("Network Configuration", listOf("No hay una red activa"), false)
        val properties = connectivity.getLinkProperties(network)
            ?: return NetworkToolResult("Network Configuration", listOf("Configuración de red no disponible"), false)
        val addresses = properties.linkAddresses.map { it.address.hostAddress.orEmpty() }
        val gateways = properties.routes.filter { it.isDefaultRoute }.mapNotNull { it.gateway?.hostAddress }
        val dns = properties.dnsServers.mapNotNull { it.hostAddress }
        return NetworkToolResult(
            "Network Configuration",
            buildList {
                add("Interfaz: ${properties.interfaceName ?: "Desconocida"}")
                addAll(addresses.map { "IP local: $it" })
                addAll(gateways.map { "Gateway: $it" })
                addAll(dns.map { "DNS: $it" })
                if (properties.domains != null) add("Dominio: ${properties.domains}")
            },
            addresses.isNotEmpty()
        )
    }

    suspend fun dnsLookup(host: String): NetworkToolResult = withContext(Dispatchers.IO) {
        val cleanHost = validatedHost(host)
        val started = System.nanoTime()
        val addresses = InetAddress.getAllByName(cleanHost).mapNotNull { it.hostAddress }.distinct()
        val elapsed = (System.nanoTime() - started) / 1_000_000
        NetworkToolResult(
            "DNS Lookup",
            listOf("Host: $cleanHost", "Tiempo: ${elapsed.coerceAtLeast(1)} ms") + addresses.map { "Dirección: $it" },
            addresses.isNotEmpty()
        )
    }

    suspend fun ping(host: String): NetworkToolResult = withContext(Dispatchers.IO) {
        val cleanHost = validatedHost(host)
        val address = InetAddress.getByName(cleanHost)
        val ports = listOf(443, 80)
        val samples = ports.associateWith { port ->
            (1..3).mapNotNull {
                runCatching {
                    measureTimeMillis {
                        Socket().use { socket -> socket.connect(InetSocketAddress(address, port), 2_500) }
                    }
                }.getOrNull()
            }
        }
        val successful = samples.filterValues { it.isNotEmpty() }
        if (successful.isEmpty()) {
            NetworkToolResult(
                "TCP Ping",
                listOf("Host: $cleanHost", "IP: ${address.hostAddress}", "Sin respuesta TCP en puertos 443 o 80"),
                false
            )
        } else {
            val best = successful.minBy { (_, values) -> values.average() }
            NetworkToolResult(
                "TCP Ping",
                listOf(
                    "Host: $cleanHost",
                    "IP: ${address.hostAddress}",
                    "Puerto: ${best.key}",
                    "Promedio: ${best.value.average().toInt()} ms",
                    "Intentos correctos: ${best.value.size}/3"
                ),
                true
            )
        }
    }

    suspend fun traceroute(host: String): NetworkToolResult = withContext(Dispatchers.IO) {
        val cleanHost = validatedHost(host)
        val pingBinary = "/system/bin/ping"
        val lines = mutableListOf("Destino: $cleanHost")
        var reached = false
        for (ttl in 1..15) {
            val process = runCatching {
                ProcessBuilder(pingBinary, "-c", "1", "-W", "1", "-t", ttl.toString(), cleanHost)
                    .redirectErrorStream(true)
                    .start()
            }.getOrElse {
                return@withContext NetworkToolResult(
                    "Traceroute",
                    listOf("Traceroute no está disponible en este dispositivo"),
                    false
                )
            }
            val finished = process.waitFor(2, TimeUnit.SECONDS)
            if (!finished) process.destroy()
            val output = runCatching { process.inputStream.bufferedReader().readText() }.getOrDefault("")
            val hop = Regex("(?i)(?:from|From)\\s+([^\\s:]+)").find(output)?.groupValues?.get(1)
            lines += "$ttl  ${hop ?: "*"}"
            if (output.contains("bytes from", ignoreCase = true) && !output.contains("exceeded", ignoreCase = true)) {
                reached = true
                break
            }
        }
        NetworkToolResult("Traceroute", lines, reached)
    }

    suspend fun dnsBenchmark(host: String): NetworkToolResult = withContext(Dispatchers.IO) {
        val cleanHost = validatedHost(host)
        val encoded = URLEncoder.encode(cleanHost, Charsets.UTF_8.name())
        val providers = listOf(
            "Cloudflare" to "https://cloudflare-dns.com/dns-query?name=$encoded&type=A",
            "Google" to "https://dns.google/resolve?name=$encoded&type=A",
            "AdGuard" to "https://dns.adguard-dns.com/resolve?name=$encoded&type=A"
        )
        val results = providers.map { (name, url) ->
            runCatching {
                var successful = false
                val elapsed = measureTimeMillis {
                    val request = Request.Builder().url(url).header("Accept", "application/dns-json").build()
                    httpClient.newCall(request).execute().use { response -> successful = response.isSuccessful }
                }
                Triple(name, elapsed, successful)
            }.getOrElse { Triple(name, -1L, false) }
        }
        val ranked = results.sortedBy { if (it.third) it.second else Long.MAX_VALUE }
        NetworkToolResult(
            "DNS Benchmark",
            listOf("Consulta: $cleanHost") + ranked.map { (name, ms, ok) ->
                if (ok) "$name: $ms ms" else "$name: sin respuesta"
            },
            ranked.any { it.third }
        )
    }

    @SuppressLint("MissingPermission")
    suspend fun wifiScan(context: Context): NetworkToolResult {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifi.isWifiEnabled) return NetworkToolResult("Wi-Fi Scanner", listOf("Wi-Fi está desactivado"), false)
        val freshResults = withTimeoutOrNull(7_000) { awaitWifiScan(context.applicationContext, wifi) }
        val results = (freshResults ?: wifi.scanResults).distinctBy { it.BSSID }.sortedByDescending { it.level }
        val lines = results.take(25).map { result ->
            val ssid = result.SSID.ifBlank { "Red oculta" }
            val security = securityLabel(result)
            "$ssid · ${result.level} dBm · $security"
        }
        return NetworkToolResult(
            "Wi-Fi Scanner",
            if (lines.isEmpty()) listOf("No se detectaron redes. Activa ubicación e inténtalo nuevamente.") else lines,
            lines.isNotEmpty()
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun awaitWifiScan(context: Context, wifi: WifiManager): List<ScanResult> =
        suspendCancellableCoroutine { continuation ->
            var receiver: BroadcastReceiver? = null
            fun unregister() {
                receiver?.let { runCatching { context.unregisterReceiver(it) } }
                receiver = null
            }
            receiver = object : BroadcastReceiver() {
                override fun onReceive(receivingContext: Context?, intent: Intent?) {
                    unregister()
                    if (continuation.isActive) continuation.resume(wifi.scanResults)
                }
            }
            val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION") context.registerReceiver(receiver, filter)
            }
            continuation.invokeOnCancellation { unregister() }
            if (!wifi.startScan()) {
                unregister()
                if (continuation.isActive) continuation.resume(wifi.scanResults)
            }
        }

    suspend fun portScan(host: String): NetworkToolResult = withContext(Dispatchers.IO) {
        val cleanHost = validatedHost(host)
        val address = InetAddress.getByName(cleanHost)
        val open = scanPorts(address.hostAddress ?: cleanHost, commonPorts.keys.toList(), 450)
        NetworkToolResult(
            "Authorized Port Scan",
            listOf("Host: $cleanHost", "IP: ${address.hostAddress}") +
                if (open.isEmpty()) listOf("No se detectaron puertos TCP comunes abiertos")
                else open.map { "Puerto $it: ${commonPorts[it] ?: "Servicio desconocido"}" },
            true
        )
    }

    suspend fun routerAudit(context: Context): NetworkToolResult = withContext(Dispatchers.IO) {
        val gateway = gatewayAddress(context)
            ?: return@withContext NetworkToolResult("Router Security Audit", listOf("Gateway Wi-Fi no disponible"), false)
        val ports = listOf(21, 22, 23, 53, 80, 443, 5000, 8080, 8443)
        val open = scanPorts(gateway, ports, 500)
        val findings = mutableListOf("Router: $gateway")
        when {
            23 in open -> findings += "ALTO: Telnet (23) está expuesto; desactívalo"
            else -> findings += "Correcto: Telnet (23) no está expuesto"
        }
        if (21 in open) findings += "ALTO: FTP (21) está expuesto; usa administración cifrada"
        if (80 in open && 443 !in open && 8443 !in open) findings += "MEDIO: panel HTTP sin HTTPS detectado"
        if (443 in open || 8443 in open) findings += "HTTPS de administración detectado"
        if (22 in open) findings += "INFO: SSH (22) está accesible en la red local"
        if (open.isEmpty()) findings += "No se detectaron servicios TCP administrativos comunes"
        findings += "Revisa firmware, contraseña administrativa, WPS y acceso remoto manualmente"
        NetworkToolResult("Router Security Audit", findings, 21 !in open && 23 !in open)
    }

    suspend fun cameraDiscovery(context: Context): NetworkToolResult = withContext(Dispatchers.IO) {
        val prefix = localIpv4Prefix(context)
            ?: return@withContext NetworkToolResult("Camera Discovery", listOf("No se pudo determinar la subred Wi-Fi"), false)
        val ownIp = deviceIpv4(context)
        val ports = listOf(554, 8000, 8080, 8899)
        val semaphore = Semaphore(32)
        val devices = coroutineScope {
            (1..254).map { suffix ->
                async {
                    semaphore.withPermit {
                        val host = "$prefix.$suffix"
                        if (host == ownIp) null else {
                            val open = scanPorts(host, ports, 180)
                            if (open.isEmpty()) null else host to open
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }
        val lines = if (devices.isEmpty()) {
            listOf("No se detectaron servicios típicos de cámaras en $prefix.0/24")
        } else {
            listOf("Posibles dispositivos; confirma cada uno antes de actuar:") + devices.take(30).map { (host, open) ->
                "$host · puertos ${open.joinToString()}"
            }
        }
        NetworkToolResult("Camera Discovery", lines, true)
    }

    private suspend fun scanPorts(host: String, ports: List<Int>, timeoutMs: Int): List<Int> = coroutineScope {
        val semaphore = Semaphore(12)
        ports.map { port ->
            async(Dispatchers.IO) {
                semaphore.withPermit { if (isPortOpen(host, port, timeoutMs)) port else null }
            }
        }.awaitAll().filterNotNull().sorted()
    }

    private fun isPortOpen(host: String, port: Int, timeoutMs: Int): Boolean = runCatching {
        Socket().use { it.connect(InetSocketAddress(host, port), timeoutMs) }
        true
    }.getOrDefault(false)

    private fun gatewayAddress(context: Context): String? {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION") val gateway = wifi.dhcpInfo?.gateway ?: return null
        return ipv4(gateway).takeUnless { it == "0.0.0.0" }
    }

    private fun deviceIpv4(context: Context): String? {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION") val ip = wifi.dhcpInfo?.ipAddress ?: return null
        return ipv4(ip).takeUnless { it == "0.0.0.0" }
    }

    private fun localIpv4Prefix(context: Context): String? = deviceIpv4(context)?.substringBeforeLast('.')

    private fun ipv4(value: Int): String = listOf(
        value and 0xff,
        value shr 8 and 0xff,
        value shr 16 and 0xff,
        value shr 24 and 0xff
    ).joinToString(".")

    private fun securityLabel(result: ScanResult): String {
        val caps = result.capabilities.uppercase()
        return when {
            "WPA3" in caps || "SAE" in caps -> "WPA3"
            "WPA2" in caps || "RSN" in caps -> "WPA2"
            "WPA" in caps -> "WPA"
            "WEP" in caps -> "WEP (insegura)"
            else -> "Abierta"
        }
    }

    private fun validatedHost(host: String): String {
        val cleanHost = host.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .substringBefore(':')
            .trimEnd('.')
        require(cleanHost.matches(Regex("^[A-Za-z0-9.-]{1,253}$"))) { "Introduce un hostname o IPv4 válido" }
        require(!cleanHost.contains("..") && cleanHost.split('.').none { it.isBlank() }) { "Hostname inválido" }
        return cleanHost
    }

    private fun yesNo(value: Boolean) = if (value) "Sí" else "No"
}

data class NetworkToolkitUiState(val isRunning: Boolean = false, val result: NetworkToolResult? = null)

class NetworkToolkitViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(NetworkToolkitUiState())
    val uiState: StateFlow<NetworkToolkitUiState> = _uiState.asStateFlow()

    fun runHealthCheck() = runLocal { NetworkDiagnosticsRepository.healthCheck(getApplication()) }
    fun readDhcpInfo() = runLocal { NetworkDiagnosticsRepository.dhcpInfo(getApplication()) }
    fun runDnsLookup(host: String) = runDiagnostic("DNS Lookup") { NetworkDiagnosticsRepository.dnsLookup(host) }
    fun runPing(host: String) = runDiagnostic("TCP Ping") { NetworkDiagnosticsRepository.ping(host) }
    fun runTraceroute(host: String) = runDiagnostic("Traceroute") { NetworkDiagnosticsRepository.traceroute(host) }
    fun runDnsBenchmark(host: String) = runDiagnostic("DNS Benchmark") { NetworkDiagnosticsRepository.dnsBenchmark(host) }
    fun runWifiScan() = runDiagnostic("Wi-Fi Scanner") { NetworkDiagnosticsRepository.wifiScan(getApplication()) }
    fun runPortScan(host: String) = runDiagnostic("Authorized Port Scan") { NetworkDiagnosticsRepository.portScan(host) }
    fun runRouterAudit() = runDiagnostic("Router Security Audit") { NetworkDiagnosticsRepository.routerAudit(getApplication()) }
    fun runCameraDiscovery() = runDiagnostic("Camera Discovery") { NetworkDiagnosticsRepository.cameraDiscovery(getApplication()) }

    fun permissionDenied() {
        _uiState.value = NetworkToolkitUiState(
            result = NetworkToolResult("Wi-Fi Scanner", listOf("Permiso de dispositivos cercanos y ubicación requerido"), false)
        )
    }

    private fun runDiagnostic(title: String, block: suspend () -> NetworkToolResult) {
        if (_uiState.value.isRunning) return
        viewModelScope.launch {
            _uiState.value = NetworkToolkitUiState(isRunning = true)
            _uiState.value = runCatching { block() }.fold(
                onSuccess = { NetworkToolkitUiState(result = it) },
                onFailure = {
                    NetworkToolkitUiState(
                        result = NetworkToolResult(title, listOf(it.message ?: "El diagnóstico falló"), false)
                    )
                }
            )
        }
    }

    fun clearResult() { _uiState.value = NetworkToolkitUiState() }

    private fun runLocal(block: () -> NetworkToolResult) {
        if (_uiState.value.isRunning) return
        viewModelScope.launch {
            _uiState.value = NetworkToolkitUiState(isRunning = true)
            _uiState.value = runCatching { block() }.fold(
                onSuccess = { NetworkToolkitUiState(result = it) },
                onFailure = { NetworkToolkitUiState(result = NetworkToolResult("Network Toolkit", listOf("No se pudo leer la red"), false)) }
            )
        }
    }
}
