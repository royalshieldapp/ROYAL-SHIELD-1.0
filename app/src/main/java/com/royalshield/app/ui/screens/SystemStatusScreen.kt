package com.royalshield.app.ui.screens

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.components.RoyalGradientBackground
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.SafeGreen
import com.royalshield.app.ui.theme.CyberCyan
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemStatusScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val typography = MaterialTheme.typography
    
    // Hardware Metrics
    val batteryLevel = getBatteryLevel(context)
    val ramInfo = getRamInfo(context)
    val storageInfo = getStorageInfo()
    
    RoyalGradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "System Status", 
                            style = typography.headlineSmall,
                            color = Color.White
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Device Info Card
                StatusCard(
                    title = "DEVICE PROFILE",
                    items = listOf(
                        "Model" to Build.MODEL,
                        "Android" to "Version ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                        "Security Patch" to (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Unknown")
                    ),
                    icon = Icons.Default.Smartphone,
                    accentColor = CyberCyan
                )

                // Hardware Health
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MetricBox(
                        modifier = Modifier.weight(1f),
                        label = "Battery",
                        value = "$batteryLevel%",
                        icon = Icons.Default.BatteryChargingFull,
                        color = if (batteryLevel > 20) SafeGreen else Color.Red
                    )
                    MetricBox(
                        modifier = Modifier.weight(1f),
                        label = "RAM Free",
                        value = ramInfo,
                        icon = Icons.Default.Memory,
                        color = RoyalGold
                    )
                }

                // Storage Card
                StatusCard(
                    title = "STORAGE ANALYTICS",
                    items = listOf(
                        "Total Space" to storageInfo.first,
                        "Available" to storageInfo.second,
                        "Status" to "Optimized"
                    ),
                    icon = Icons.Default.Storage,
                    accentColor = RoyalGold
                )

                // Protection Stats
                StatusCard(
                    title = "ROYAL SHIELD CORE",
                    items = listOf(
                        "Firewall" to "Active",
                        "AI Monitor" to "Real-time",
                        "VPN Engine" to "Ready",
                        "SMS Guard" to "Active"
                    ),
                    icon = Icons.Default.VerifiedUser,
                    accentColor = SafeGreen
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    items: List<Pair<String, String>>,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    title, 
                    color = accentColor, 
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.Gray, fontSize = 13.sp)
                    Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun MetricBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.Gray, fontSize = 11.sp)
        }
    }
}

private fun getBatteryLevel(context: Context): Int {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}

private fun getRamInfo(context: Context): String {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val memoryInfo = android.app.ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    val availableMegs = memoryInfo.availMem / 1048576L
    return "${availableMegs}MB"
}

private fun getStorageInfo(): Pair<String, String> {
    val path = Environment.getDataDirectory()
    val stat = StatFs(path.path)
    val blockSize = stat.blockSizeLong
    val totalBlocks = stat.blockCountLong
    val availableBlocks = stat.availableBlocksLong
    
    val total = (totalBlocks * blockSize) / (1024 * 1024 * 1024)
    val available = (availableBlocks * blockSize) / (1024 * 1024 * 1024)
    
    return "${total}GB" to "${available}GB"
}
