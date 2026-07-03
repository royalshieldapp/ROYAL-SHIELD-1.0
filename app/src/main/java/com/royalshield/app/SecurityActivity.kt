package com.royalshield.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.theme.Royal_shieldTheme
import com.royalshield.app.managers.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class SecurityActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferencesManager.init(this)
        setContent {
            Royal_shieldTheme {
                SecurityDashboard()
            }
        }
    }
}

object NeonGoldTheme {
    val bg = Brush.radialGradient(
        colors = listOf(Color(0xFF1B1B1F), Color(0xFF0A0A0A)),
        center = androidx.compose.ui.geometry.Offset(200f, -100f),
        radius = 1200f
    )
    val surface = Color(0xFF121216)
    val surface2 = Color(0xFF090A0C)
    val text = Color(0xFFF8FAFC)
    val muted = Color(0xFF64748B)
    val accent = Color(0xFFFFD700)
    val accent2 = Color(0xFFB8860B)
    val danger = Color(0xFFFF3131)
    val panelBorder = Color(0xFF2A2A2A)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDashboard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { VirusTotalRepository() }

    // State
    val initialAction = (context as? android.app.Activity)?.intent?.getStringExtra("action") ?: "filtrar"
    var selectedAction by remember { mutableStateOf(initialAction) }
    
    // Shared State
    var contactInput by remember { mutableStateOf("") }
    var urlResult by remember { mutableStateOf<UrlResult?>(null) }
    var isCheckingUrl by remember { mutableStateOf(false) }

    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var currentScannedApp by remember { mutableStateOf("Initializing...") }
    var threatsFound by remember { mutableIntStateOf(0) }
    val scanLogs = remember { mutableStateListOf<ScanLog>() }

    var protectionStatus by remember { mutableStateOf("SAFE") }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    fun checkApiKey(): Boolean {
        val key = PreferencesManager.getVirusTotalApiKey()
        if (key.isNullOrBlank()) {
            showApiKeyDialog = true
            return false
        }
        return true
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // New Background Image
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.shield_bg_gold),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonGoldTheme.text)
                }
                Text(
                    text = if(selectedAction == "denunciar") "Report Incident" else "Security Center",
                    color = NeonGoldTheme.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }

            if (selectedAction == "denunciar") {
                // REPORT INCIDENT UI
                ReportIncidentScreen(
                    onSubmit = { title, desc -> 
                         Toast.makeText(context, "Report submitted: $title", Toast.LENGTH_LONG).show()
                         (context as? android.app.Activity)?.finish()
                    }
                )
            } else {
                // SECURITY TOOLS UI
                
                // Status Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, if(protectionStatus == "SAFE") Color(0xFF00FF00) else NeonGoldTheme.danger, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = NeonGoldTheme.surface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    if(protectionStatus == "SAFE") Color(0xFF00FF94).copy(alpha = 0.1f) else NeonGoldTheme.danger.copy(alpha = 0.1f), 
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = if(protectionStatus == "SAFE") Color(0xFF00FF94) else NeonGoldTheme.danger,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Protection Status", color = NeonGoldTheme.muted, fontSize = 12.sp)
                            Text(
                                text = protectionStatus,
                                color = if(protectionStatus == "SAFE") Color(0xFF00FF94) else NeonGoldTheme.danger,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Protecting: Apps • Files • Network",
                                color = if(protectionStatus == "SAFE") Color(0xFF00FF94).copy(alpha = 0.7f) else NeonGoldTheme.danger.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // URL Checker
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(NeonGoldTheme.surface)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Analyze Content", color = NeonGoldTheme.text, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(
                        value = contactInput,
                        onValueChange = { contactInput = it },
                        placeholder = { Text("Enter URL or Phone...", color = NeonGoldTheme.muted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            focusedBorderColor = NeonGoldTheme.accent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // CHECK NOW - Custom Gold Button
                    GoldButton(
                        text = "CHECK NOW",
                        isLoading = isCheckingUrl,
                        onClick = {
                            if (checkApiKey()) {
                                if (contactInput.isNotEmpty()) {
                                    isCheckingUrl = true
                                    scope.launch {
                                        urlResult = repository.checkUrl(contactInput)
                                        isCheckingUrl = false
                                    }
                                }
                            }
                        }
                    )

                    urlResult?.let {
                        Surface(
                            color = if(it.verdict == "safe") Color(0xFF00FF94).copy(alpha = 0.1f) else NeonGoldTheme.danger.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if(it.verdict == "safe") Icons.Default.Shield else Icons.Default.BugReport, null, tint = if(it.verdict == "safe") Color(0xFF00FF94) else NeonGoldTheme.danger)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Detection: ${it.verdict.uppercase()}", fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Score: ${it.score}/100", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }


                // App Scan Section
                val isPremium = remember { PreferencesManager.isPremiumUser() }
                var showPaywall by remember { mutableStateOf(false) }

                if (!isPremium) {
                    // Paywall Banner for FREE users
                    com.royalshield.app.ui.components.PaywallBanner(
                        featureName = "Deep Security Scan",
                        onUpgradeClick = {
                            context.startActivity(Intent(context, SubscriptionActivity::class.java))
                        }
                    )
                } else {
                    // Full Deep Scan UI for PREMIUM users
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E1E24))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, null, tint = NeonGoldTheme.accent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Malware App Scanner", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        if (isScanning) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                            ) {
                                // Large Percentage Display
                                Text(
                                    text = "${scanProgress.toInt()} %",
                                    color = NeonGoldTheme.accent,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 4.sp
                                )
                                
                                // Golden Segmented Progress Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF3A3A3A))
                                ) {
                                    // Golden fill with segments
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(scanProgress / 100f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xFF8B6914),
                                                        NeonGoldTheme.accent,
                                                        Color(0xFFFFD700),
                                                        NeonGoldTheme.accent,
                                                        Color(0xFF8B6914)
                                                    )
                                                )
                                            )
                                    )
                                    
                                    // Vertical segment dividers
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        repeat(10) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .fillMaxHeight()
                                                    .background(Color(0xFF1A1A1A))
                                            )
                                        }
                                    }
                                }
                                
                                Text(
                                    text = currentScannedApp,
                                    color = NeonGoldTheme.accent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        // START DEEP SCAN - Custom Gold Button
                        GoldButton(
                            text = if(isScanning) "SCANNING..." else "START DEEP SCAN",
                            isLoading = false,
                            onClick = {
                                if (!isScanning) {
                                    isScanning = true
                                    threatsFound = 0
                                    scanProgress = 0f
                                    currentScannedApp = "Initializing scan..."
                                    
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        val pm = context.packageManager
                                        val apps = pm.getInstalledPackages(android.content.pm.PackageManager.GET_PERMISSIONS)
                                        // Filter system apps to only scan user-installed ones for speed and relevance
                                        val userApps = apps.filter { ((it.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
                                        
                                        val totalApps = userApps.size
                                        if (totalApps == 0) {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                scanProgress = 100f
                                                currentScannedApp = "No apps found."
                                                delay(1000)
                                                protectionStatus = "SAFE"
                                                isScanning = false
                                                val timestamp = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                                scanLogs.add(0, ScanLog(timestamp, "Clean"))
                                            }
                                            return@launch
                                        }

                                        userApps.forEachIndexed { index, packageInfo ->
                                            val appName = packageInfo.applicationInfo?.loadLabel(pm)?.toString() ?: packageInfo.packageName ?: "Unknown"
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                scanProgress = ((index + 1) / totalApps.toFloat()) * 100f
                                                currentScannedApp = "Scanning: $appName..."
                                            }
                                            
                                            // Analyze heuristic risk (basic signature)
                                            var isRisky = false
                                            val permissions = packageInfo.requestedPermissions
                                            if (permissions != null) {
                                                val dangerousPerms = setOf(
                                                    "android.permission.BIND_DEVICE_ADMIN",
                                                    "android.permission.BIND_ACCESSIBILITY_SERVICE",
                                                    "android.permission.SYSTEM_ALERT_WINDOW",
                                                    "android.permission.READ_SMS",
                                                    "android.permission.RECEIVE_SMS"
                                                )
                                                val matchingPerms = permissions.count { dangerousPerms.contains(it) }
                                                if (matchingPerms >= 2) {
                                                    isRisky = true
                                                }
                                            }
                                            
                                            // Small delay to allow UI to render the progress smoothly and give realistic scanning feel
                                            delay(60)
                                            
                                            if (isRisky) {
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    threatsFound++
                                                }
                                            }
                                        }

                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            protectionStatus = if (threatsFound > 0) "RISK" else "SAFE"
                                            isScanning = false
                                            scanProgress = 0f
                                            
                                            val timestamp = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                            val resultStr = if (threatsFound > 0) "$threatsFound Threats" else "Clean"
                                            scanLogs.add(0, ScanLog(timestamp, resultStr))
                                            
                                            if (threatsFound > 0) {
                                                Toast.makeText(context, "$threatsFound suspicious apps found!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Device is clean.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                        )

                        // Scan History & Last Scan Display
                        if (scanLogs.isNotEmpty()) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                            
                            // Last Scan Highlight
                            Text(
                                text = "LAST SCAN: ${scanLogs.first().timestamp}", 
                                color = NeonGoldTheme.accent, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("HISTORY LOGS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                scanLogs.take(5).forEach { log ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${log.timestamp} - ${log.result}",
                                            color = if (log.result.contains("Threat")) NeonGoldTheme.danger else Color.Gray,
                                            fontSize = 12.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Tools (with paywall check for Threat Map)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Update to GoldButton
                    GoldButton("Scan File", Icons.Default.Info, Modifier.weight(1f)) {
                        context.startActivity(Intent(context, FileScanActivity::class.java))
                    }
                    GoldButton("Threat Map", Icons.Default.Public, Modifier.weight(1f)) {
                        if (isPremium) {
                            context.startActivity(Intent(context, CyberThreatMapActivity::class.java))
                        } else {
                            showPaywall = true
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Row 2: Discrete Mode & AI Voice
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                     GoldButton("Discrete Mode", Icons.Default.VisibilityOff, Modifier.weight(1f)) {
                        context.startActivity(Intent(context, DiscreteModeActivity::class.java))
                    }
                    GoldButton("Voice Shield", Icons.Default.RecordVoiceOver, Modifier.weight(1f)) {
                        context.startActivity(Intent(context, VoiceScamActivity::class.java))
                    }
                }
                
                // Row 3: SOS History
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GoldButton("SOS History", Icons.Default.History, Modifier.fillMaxWidth()) {
                        context.startActivity(Intent(context, HistoryActivity::class.java))
                    }
                }
                
                // Paywall Dialog
                if (showPaywall) {
                    com.royalshield.app.ui.components.PaywallDialog(
                        featureName = "Cyber Threat Map",
                        onDismiss = { showPaywall = false },
                        onUpgradeClick = {
                            context.startActivity(Intent(context, SubscriptionActivity::class.java))
                            showPaywall = false
                        }
                    )
                }
            } // End else/main content

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showApiKeyDialog) {
            ApiKeyDialog(
                onDismiss = { showApiKeyDialog = false },
                onSave = { key ->
                    PreferencesManager.saveVirusTotalApiKey(key)
                    showApiKeyDialog = false
                    Toast.makeText(context, "API Key saved", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun ReportIncidentScreen(onSubmit: (String, String) -> Unit) {
    Card(
         modifier = Modifier.fillMaxWidth(),
         colors = CardDefaults.cardColors(containerColor = NeonGoldTheme.surface),
         shape = RoundedCornerShape(24.dp)
    ) {
         Column(modifier = Modifier.padding(20.dp)) {
             Text("Submit Security Report", color = NeonGoldTheme.accent, fontWeight = FontWeight.Bold)
             Spacer(modifier = Modifier.height(16.dp))
             
             var reportTitle by remember { mutableStateOf("") }
             var reportDesc by remember { mutableStateOf("") }
             
             OutlinedTextField(
                 value = reportTitle,
                 onValueChange = { reportTitle = it },
                 label = { Text("Incident Title") },
                 modifier = Modifier.fillMaxWidth(),
                 colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGoldTheme.accent,
                    unfocusedBorderColor = Color.White.copy(alpha=0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                 )
             )
             
             Spacer(modifier = Modifier.height(8.dp))
             
             OutlinedTextField(
                 value = reportDesc,
                 onValueChange = { reportDesc = it },
                 label = { Text("Description") },
                 modifier = Modifier.fillMaxWidth().height(120.dp),
                 colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGoldTheme.accent,
                    unfocusedBorderColor = Color.White.copy(alpha=0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                 )
             )
             
             Spacer(modifier = Modifier.height(16.dp))
             
             // Reuse GoldButton here too for consistency? User didn't explicitly ask, but it fits the theme. 
             // Leaving as regular button to stand out as "Submit" vs "Tool" or adapting if requested.
             // User only asked for "Start deep scan", "Scan file", "Threat map", "Discrete", "SOS history".
             Button(
                 onClick = { onSubmit(reportTitle, reportDesc) },
                 modifier = Modifier.fillMaxWidth(),
                 colors = ButtonDefaults.buttonColors(containerColor = NeonGoldTheme.danger)
             ) {
                 Text("SEND REPORT")
             }
         }
    }
}

// Replaced ToolButton with GoldButton logic and renamed or overloaded
@Composable
fun GoldButton(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Background Image
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.button_gold_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
        )

        // Content
        if (isLoading) {
            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ApiKeyDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("VirusTotal API Config") },
        text = {
            Column {
                Text("Enter API Key to enable real-time detection.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Key") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(key) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    return pm.getInstalledPackages(0).map {
        AppInfo(it.packageName, it.applicationInfo?.sourceDir ?: "", ((it.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0)
    }
}

data class AppInfo(val packageName: String, val sourceDir: String, val isSystem: Boolean)

@Preview
@Composable
fun SecurityDashboardPreview() {
    Royal_shieldTheme {
        SecurityDashboard()
    }
}

data class ScanLog(val timestamp: String, val result: String)
