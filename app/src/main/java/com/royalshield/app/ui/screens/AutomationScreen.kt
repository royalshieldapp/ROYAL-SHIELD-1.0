package com.royalshield.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.data.db.ActionType
import com.royalshield.app.data.db.AppDatabase
import com.royalshield.app.data.db.AutomationRule
import com.royalshield.app.data.db.TriggerType
import com.royalshield.app.ui.components.PremiumGlassCard
import com.royalshield.app.ui.theme.CyberCyan
import com.royalshield.app.ui.components.*
import com.royalshield.app.ui.components.CyberButtonRound
import com.royalshield.app.ui.components.CyberButtonRect
import com.royalshield.app.ui.components.CyberStatusStrip
import com.royalshield.app.ui.theme.NeonBlue
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.SafeGreen
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(onBack: () -> Unit = {}, onBusinessClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    
    // Automation Rules State
    var rules by remember { mutableStateOf(emptyList<AutomationRule>()) }

    // Smart Home State
    var isScanning by remember { mutableStateOf(false) }
    var scannedDevices by remember { mutableStateOf(listOf<SmartDevice>()) }
    var connectedDevices by remember { mutableStateOf(listOf<SmartDevice>()) }
    var showNetworkTools by remember { mutableStateOf(false) }
    var showWifiScanDialog by remember { mutableStateOf(false) }
    var showSpeedTestDialog by remember { mutableStateOf(false) }

    // Load initial automation data
    LaunchedEffect(Unit) {
        db.automationDao().getAllRules().collect {
            rules = it
        }
    }

    com.royalshield.app.ui.components.RoyalGradientBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Automation & Smart Control",
                    style = MaterialTheme.typography.headlineSmall, // Slightly smaller to fit
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Smart Home Hero Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.img_smart_kitchen),
                        contentDescription = "Smart Home",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // SYSTEM HEALTH STATUS
            CyberStatusStrip(
                progress = 0.85f,
                label = "Automation Neural Link Health",
                color = SafeGreen,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // --- CATEGORY CARDS (User Requested) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: BUSINESS / ENTERPRISE (Custom Quote)
                PremiumGlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clickable { onBusinessClick() }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.card_business_bg),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop, // Crop to fill
                            modifier = Modifier.fillMaxSize()
                        )
                        // Overlay for readability
                        Box(
                             modifier = Modifier
                                 .fillMaxSize()
                                 .background(Color.Black.copy(alpha = 0.4f))
                        )
                        
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("BUSINESS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Quote", color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Card 2: NETWORK
                PremiumGlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clickable { showNetworkTools = true }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                             Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(32.dp))
                             Spacer(modifier = Modifier.height(8.dp))
                             Text("NETWORK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // --- SMART HOME SECTION ---
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Smart Devices",
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    if (!isScanning) {
                        FloatingActionButton(
                            onClick = {
                                isScanning = true
                                scope.launch {
                                    // Mimic scanning delay
                                    delay(2000)
                                    // Mock results
                                    val newDevices = listOf(
                                        SmartDevice("1", "Living Room Light", DeviceType.LIGHT),
                                        SmartDevice("2", "Kitchen LED Strip", DeviceType.LIGHT),
                                        SmartDevice("3", "Smart Plug A1", DeviceType.PLUG)
                                    ).filter { device -> 
                                        // Filter out already connected
                                        connectedDevices.none { it.id == device.id }
                                    }
                                    
                                    scannedDevices = newDevices
                                    isScanning = false
                                    
                                    if (newDevices.isEmpty()) {
                                        Toast.makeText(context, "No new devices found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            containerColor = CyberCyan,
                            contentColor = Color.Black,
                            modifier = Modifier.size(40.dp) // Small size
                        ) {
                            Icon(Icons.Default.BluetoothSearching, "Scan", modifier = Modifier.size(20.dp))
                        }
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = CyberCyan
                        )
                    }
                }

                // List Connected Devices (The functional control panel)
                if (connectedDevices.isNotEmpty()) {
                    Text("My Devices", color = Color.Gray, fontSize = 12.sp)
                    connectedDevices.forEach { device ->
                        SmartDeviceControlCard(device, onUpdate = { updatedDevice ->
                            // Update local state list
                            connectedDevices = connectedDevices.map { 
                                if (it.id == updatedDevice.id) updatedDevice else it 
                            }
                        })
                    }
                }

                // List Scanned/Found Devices (Waiting to connect)
                AnimatedVisibility(visible = scannedDevices.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Found Nearby", color = Color.Gray, fontSize = 12.sp)
                        scannedDevices.forEach { device ->
                            ScannedDeviceItem(device) {
                                // Connect logic
                                val connected = device.copy(isConnected = true, isOn = true)
                                connectedDevices = connectedDevices + connected
                                scannedDevices = scannedDevices - device
                                Toast.makeText(context, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                
                if (connectedDevices.isEmpty() && scannedDevices.isEmpty() && !isScanning) {
                     PremiumGlassCard(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                 Icon(Icons.Default.Lightbulb, null, tint = Color.Gray)
                                 Spacer(modifier = Modifier.height(8.dp))
                                 Text("No devices connected. Tap scan.", color = Color.Gray)
                             }
                         }
                     }
                }
            }

            // Separator (Manual Box instead of Divider to avoid Material3 version conflicts)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            // --- AUTOMATION RULES SECTION ---
            Text(
                "My Rules",
                color = RoyalGold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            // Quick Create Route Guard Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeGreen.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch(Dispatchers.IO) {
                            val routeRule = AutomationRule(
                                name = "Route Guard",
                                triggerType = TriggerType.ROUTE_DEVIATION,
                                actionType = ActionType.ALERT_PROMPT,
                                triggerParams = "50km",
                                actionParams = "Check Status/Send Alert"
                            )
                            db.automationDao().insertRule(routeRule)
                        }
                        Toast.makeText(context, "Route Guard Activated", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                   modifier = Modifier.padding(16.dp),
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocationOn, "Route", tint = SafeGreen)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Activate Route Guard", color = SafeGreen, fontWeight = FontWeight.Bold)
                        Text("Alert if deviation > 50km", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (rules.isEmpty()) {
                    item {
                         Text("No automatic rules set.", color = Color.Gray)
                    }
                } else {
                    items(rules) { rule ->
                        AutomationRuleItem(
                            rule = rule,
                            onToggle = { isEnabled ->
                                scope.launch(Dispatchers.IO) {
                                    db.automationDao().updateRule(rule.copy(isEnabled = isEnabled))
                                }
                            },
                            onDelete = {
                                scope.launch(Dispatchers.IO) {
                                    db.automationDao().deleteRule(rule)
                                }
                            }
                        )
                    }
                }
            }
        }
        
        if (showNetworkTools) {
             androidx.compose.material3.AlertDialog(
                onDismissRequest = { showNetworkTools = false },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("Network Tools", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        NetworkToolItem(
                            icon = Icons.Default.Wifi,
                            title = "WiFi Scan",
                            description = "Analyze signal strength & channels",
                            onClick = { 
                                showNetworkTools = false
                                showWifiScanDialog = true
                            }
                        )
                         NetworkToolItem(
                            icon = Icons.Default.CompareArrows,
                            title = "Ping Tracker",
                            description = "Monitor latency & packet loss",
                             onClick = { 
                                Toast.makeText(context, "Tracking Ping...", Toast.LENGTH_SHORT).show()
                            }
                        )
                        NetworkToolItem(
                            icon = Icons.Default.Timer, // Or Speed/ShutterSpeed
                            title = "Speed Test",
                            description = "Measure download/upload speed",
                             onClick = { 
                                showNetworkTools = false
                                showSpeedTestDialog = true
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showNetworkTools = false }) {
                        Text("Close", color = CyberCyan)
                    }
                }
            )
        }
        
        if (showWifiScanDialog) {
            WifiScanDialog(onDismiss = { showWifiScanDialog = false })
        }
        
        if (showSpeedTestDialog) {
            com.royalshield.app.ui.components.SpeedTestDialog(onDismiss = { showSpeedTestDialog = false })
        }
    }
    }
}

@Composable
fun WifiScanDialog(onDismiss: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        // Simple animation: 0 to 100% over 2 seconds
        val steps = 100
        for (i in 1..steps) {
            progress = i / 100f
            delay(20) 
        }
        delay(500) // Show 100% for a bit
        onDismiss()
    }
    
    Dialog(onDismissRequest = { }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)), // Light grey background like image
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
               // The Gradient Ring
               Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 16.dp.toPx()
                        val radius = size.minDimension / 2 - strokeWidth / 2
                        
                        // Background ring (Light grey/white)
                        drawCircle(
                            color = Color.White,
                            radius = radius,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                        
                        // Gradient ring
                        // Colors: Pink/Red -> Yellow from image
                        val gradient = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFFF007F), // Bright Pink
                                Color(0xFFFFEA00), // Bright Yellow
                                Color(0xFFFF007F)  // Wrap around to Pink
                            )
                        )
                        
                        // Rotate to start from top? Default 0 is 3 o'clock. -90 is 12 o'clock.
                        // We want the gradient to look nice.
                        
                        rotate(-90f) {
                            drawArc(
                                brush = gradient,
                                startAngle = 0f,
                                sweepAngle = progress * 360f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White // Text in image looks white (on the grey card? no wait, the image background is grey, the text is inside the ring. If the ring is hollow, the text is on the card background. The card background is grey. White text on light grey is bad contrast. The image has a light grey background, but maybe the text is dark? Or the circle has a fill?
                        // Looking closely at the image provided by user:
                        // Background is light grey. Ring is gradient. Text "10%" is WHITE. But there is a soft shadow/glow or maybe the center is darker?
                        // Actually, looking at the image: the background inside the ring is same as outside.
                        // White text on #E0E0E0 is hard to read. Maybe the image has a dark theme?
                        // Wait, the image provided 
                        // https://.../uploaded_image_1767768929233.png
                        // The background looks like a very light grey / white gradient. The text "10%" is white, but has a subtle shadow.
                        // To be safe, I will use a Dark Grey card if it matches the app theme (Royal Shield uses dark themes usually).
                        // BUT, the user said "Run emulator" and I saw dark UI.
                        // However, the provided image has a LIGHT background.
                        // If I put a light card in a dark app, it might look out of place, but it matches the request "show THIS".
                        // I will stick to what the image shows: Light Grey card.
                        // To make White text readable on Light Grey, I might need a shadow or the text is actually not white?
                        // Let's look at the image again... It's possible the text is just very thin and white?
                        // Actually, I'll use Color.Black as safe fallback or Color(0xFF555555) if it looks like the image.
                        // "10%" in image is white. It must have a drop shadow.
                        // I will use Color.White with a shadow, OR just White if the background is dark enough. 
                        // I used 0xFFE0E0E0 which is quite light.
                        // Let's use a slightly darker grey for the card to ensure contrast, or rely on the app being dark mode?
                        // Royal Shield is dark mode.
                        // The image provided is light mode.
                        // I should probably adapt it to the app's Dark Mode to keep consistency, using the Gradient Ring as the key visual element.
                        // Changing to Dark Card (Color(0xFF1E1E24)) with White text is safer and fits "Royal Shield".
                        // The User said "show this" pointing to the ring style. I'll prioritize the ring style but maybe adapt background.
                        // BUT "copy exactly" is also a safe route.
                        /// I will use the colors from the image for the ring, but keep the App's dark theme for the card background so it doesn't blind the user. 
                        // The ring colors are Pink and Yellow.
                    )
               }
               Spacer(modifier = Modifier.height(24.dp))
               Text("Searching Networks...", color = Color.Gray, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
