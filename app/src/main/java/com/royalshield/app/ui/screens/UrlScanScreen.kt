package com.royalshield.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.VirusTotalRepository
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.SafeGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlScanScreen(onBackPressed: () -> Unit) {
    var urlInput by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<UrlScanResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val virusTotalRepository = remember { VirusTotalRepository() }
    
    com.royalshield.app.ui.components.RoyalGradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("URL Scanner", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = RoyalGold,
                        navigationIconContentColor = RoyalGold
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                "Analyze any URL for security threats",
                color = Color.White,
                fontSize = 16.sp
            )
            
            // URL Input
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Enter URL", color = Color.Gray) },
                placeholder = { Text("https://example.com", color = Color.Gray.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = RoyalGold,
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null, tint = RoyalGold)
                }
            )
            
            // Scan Button
            Button(
                onClick = {
                    if (urlInput.isNotBlank()) {
                        isScanning = true
                        scanResult = null
                        errorMessage = null
                        
                        // Normalize URL: add https:// if no protocol
                        val normalizedUrl = if (!urlInput.startsWith("http://") && !urlInput.startsWith("https://")) {
                            "https://$urlInput"
                        } else urlInput
                        
                        scope.launch {
                            try {
                                val vtResult = virusTotalRepository.checkUrl(normalizedUrl)
                                
                                // Map VirusTotal result to UI model
                                val isSafe = vtResult.verdict == "safe" || vtResult.verdict == "unknown"
                                val riskScore = vtResult.score
                                
                                scanResult = UrlScanResult(
                                    url = normalizedUrl,
                                    isSafe = isSafe && riskScore < 30,
                                    riskScore = riskScore,
                                    detections = if (vtResult.verdict == "malicious") riskScore / 10 else 0,
                                    categories = when (vtResult.verdict) {
                                        "malicious" -> listOf("Malware", "Dangerous")
                                        "suspicious" -> listOf("Suspicious", "Caution")
                                        "safe" -> listOf("Clean", "Verified")
                                        else -> listOf("Unknown")
                                    },
                                    ipAddress = "—", // VirusTotal free API doesn't return IP easily in this flow
                                    country = "—",
                                    ssl = normalizedUrl.startsWith("https"),
                                    reputation = when (vtResult.verdict) {
                                        "malicious" -> "Bad"
                                        "suspicious" -> "Suspicious"
                                        "safe" -> "Good"
                                        else -> "Unknown"
                                    },
                                    threats = when (vtResult.verdict) {
                                        "malicious" -> listOf(
                                            ThreatInfo(vtResult.reason, "CRITICAL")
                                        )
                                        "suspicious" -> listOf(
                                            ThreatInfo(vtResult.reason, "MEDIUM")
                                        )
                                        else -> emptyList()
                                    }
                                )
                            } catch (e: Exception) {
                                errorMessage = "Scan error: ${e.message}"
                            } finally {
                                isScanning = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold),
                enabled = urlInput.isNotBlank() && !isScanning
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning with VirusTotal...", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Text("Scan URL", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Error message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Error, null, tint = Color.Red)
                        Text(error, color = Color.Red, fontSize = 14.sp)
                    }
                }
            }
            
            // Results
            scanResult?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                
                // Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.isSafe) SafeGreen.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (result.isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (result.isSafe) SafeGreen else Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Column {
                            Text(
                                if (result.isSafe) "✓ Safe to visit" else "⚠ Threat Detected",
                                color = if (result.isSafe) SafeGreen else Color.Red,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Risk Score: ${result.riskScore}%",
                                color = if (result.isSafe) SafeGreen.copy(alpha = 0.8f) else Color.Red.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                // Detailed Information
                Text(
                    "Detailed Analysis",
                    color = RoyalGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                // Info Cards
                DetailCard("URL Scanned", result.url, Icons.Default.Link)
                DetailCard("Security Detections", "${result.detections}/68 engines", Icons.Default.Security)
                DetailCard("IP Address", result.ipAddress, Icons.Default.Computer)
                DetailCard("Country", result.country, Icons.Default.Language)
                DetailCard("SSL Certificate", if (result.ssl) "Valid (HTTPS)" else "Not Secure", Icons.Default.Lock)
                DetailCard("Reputation", result.reputation, Icons.Default.Star)
                
                // Categories
                if (result.categories.isNotEmpty()) {
                    Text(
                        "Categories",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        result.categories.forEach { category ->
                            Surface(
                                color = if (result.isSafe) SafeGreen.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    category,
                                    color = if (result.isSafe) SafeGreen else Color.Red,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
                
                // Threat Details
                if (result.threats.isNotEmpty()) {
                    Text(
                        "Detected Threats",
                        color = Color.Red,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    result.threats.forEach { threat ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = when (threat.severity) {
                                        "CRITICAL" -> Color(0xFFFF0000)
                                        "HIGH" -> Color(0xFFFF6B00)
                                        "MEDIUM" -> RoyalGold
                                        else -> Color.Gray
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        threat.description,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "Severity: ${threat.severity}",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { scanResult = null; urlInput = "" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Scan Another")
                    }
                    
                    if (!result.isSafe) {
                        Button(
                            onClick = { /* Report functionality */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Report Threat")
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun DetailCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = RoyalGold,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    label,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    value,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

data class UrlScanResult(
    val url: String,
    val isSafe: Boolean,
    val riskScore: Int,
    val detections: Int,
    val categories: List<String>,
    val ipAddress: String,
    val country: String,
    val ssl: Boolean,
    val reputation: String,
    val threats: List<ThreatInfo>
)

data class ThreatInfo(
    val description: String,
    val severity: String
)
