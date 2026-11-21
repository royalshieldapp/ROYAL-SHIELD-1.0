package com.royalshield.app.ui.screens

import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.royalshield.app.features.smarthome.SmartHomeViewModel
import com.royalshield.app.data.AvailabilityState


data class AutomationRuleTemplate(
    val title: String,
    val description: String,
    val triggerType: TriggerType,
    val actionType: ActionType,
    val triggerParams: String = "",
    val actionParams: String = "",
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color = RoyalGold,
    val requiresVerification: Boolean = false
)

val defaultRuleTemplates = listOf(
    AutomationRuleTemplate(
        title = "Route Guard",
        description = "Alert contacts if GPS route deviates > 50km",
        triggerType = TriggerType.ROUTE_DEVIATION,
        actionType = ActionType.ALERT_PROMPT,
        triggerParams = "50km",
        actionParams = "Check Status/Send Alert",
        icon = Icons.Filled.LocationOn,
        accentColor = SafeGreen,
        requiresVerification = true
    ),
    AutomationRuleTemplate(
        title = "Night Lockdown",
        description = "Automatically arm security perimeter at 22:00",
        triggerType = TriggerType.TIME_SCHEDULE,
        actionType = ActionType.ACTIVATE_ALARM,
        triggerParams = "22:00",
        actionParams = "Arm Perimeter",
        icon = Icons.Filled.Timer,
        accentColor = RoyalGold
    ),
    AutomationRuleTemplate(
        title = "Battery SOS Beacon",
        description = "Send emergency GPS location when battery < 15%",
        triggerType = TriggerType.BATTERY_LOW,
        actionType = ActionType.SEND_SOS,
        triggerParams = "15%",
        actionParams = "Emergency Contacts",
        icon = Icons.Filled.Power,
        accentColor = Color(0xFFFF6B6B)
    ),
    AutomationRuleTemplate(
        title = "Public Wi-Fi Shield",
        description = "Automatically activate VPN on untrusted Wi-Fi",
        triggerType = TriggerType.WIFI_CONNECT,
        actionType = ActionType.TOGGLE_VPN,
        triggerParams = "Public Wi-Fi",
        actionParams = "Connect Secure VPN",
        icon = Icons.Filled.Wifi,
        accentColor = CyberCyan
    ),
    AutomationRuleTemplate(
        title = "Safe Arrival Home",
        description = "Disarm perimeter siren upon entering Home zone",
        triggerType = TriggerType.LOCATION_ENTER,
        actionType = ActionType.MUTE_AUDIO,
        triggerParams = "Home",
        actionParams = "Disarm Siren",
        icon = Icons.Filled.LocationOn,
        accentColor = SafeGreen
    ),
    AutomationRuleTemplate(
        title = "Car Sentry S.O.S.",
        description = "Enable highway travel guard on car Bluetooth connect",
        triggerType = TriggerType.BLUETOOTH_CONNECT,
        actionType = ActionType.ALERT_PROMPT,
        triggerParams = "Car Bluetooth",
        actionParams = "Highway Travel Monitor",
        icon = Icons.Filled.BluetoothSearching,
        accentColor = NeonBlue
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(
    availability: AvailabilityState = AvailabilityState.Unavailable("STATUS_UNVERIFIED"),
    onBack: () -> Unit = {},
    onBusinessClick: () -> Unit = {}
) {
    val actionsVerified = availability is AvailabilityState.Ready
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val smartHomeViewModel: SmartHomeViewModel = viewModel()
    val smartHomeState by smartHomeViewModel.uiState.collectAsStateWithLifecycle()
    val bluetoothPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            smartHomeViewModel.scan()
        } else {
            Toast.makeText(
                context,
                "Nearby devices permission is required for Smart Home discovery",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun scanSmartHomeDevices() {
        val missingPermissions = bluetoothPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isEmpty()) {
            smartHomeViewModel.scan()
        } else {
            bluetoothPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
    
    // Automation Rules State
    var rules by remember { mutableStateOf(emptyList<AutomationRule>()) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    // Load initial automation data
    LaunchedEffect(Unit) {
        db.automationDao().getAllRules().collect {
            rules = it
        }
    }

    LaunchedEffect(smartHomeState.message) {
        smartHomeState.message?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            smartHomeViewModel.consumeMessage()
        }
    }

    com.royalshield.app.ui.components.RoyalGradientBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                    .height(140.dp)
                    .tilt3D(defaultRotationX = 0f, defaultRotationY = 0f, maxRotationX = 5f, maxRotationY = 8f)
                    .border(1.5.dp, RoyalGold.copy(alpha = 0.85f), RoundedCornerShape(20.dp)),
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
                progress = if (actionsVerified) 1.0f else 0.0f,
                label = if (actionsVerified) "Automation Neural Link Active" else "Automation Neural Link (Unverified)",
                color = if (actionsVerified) SafeGreen else Color.Gray,
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
                        .tilt3D(defaultRotationX = 0f, defaultRotationY = 0f, maxRotationX = 7f, maxRotationY = 10f)
                        .clickable { onBusinessClick() }
                    ,
                    borderColor = RoyalGold.copy(alpha = 0.75f)
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
                    
                    if (!smartHomeState.isScanning) {
                        FloatingActionButton(
                            onClick = ::scanSmartHomeDevices,
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
                if (smartHomeState.connectedDevices.isNotEmpty()) {
                    Text("My Devices", color = Color.Gray, fontSize = 12.sp)
                    smartHomeState.connectedDevices.forEach { device ->
                        SmartDeviceControlCard(device, onUpdate = smartHomeViewModel::updateDevice)
                    }
                }

                // List Scanned/Found Devices (Waiting to connect)
                AnimatedVisibility(visible = smartHomeState.scannedDevices.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Google Home devices", color = Color.Gray, fontSize = 12.sp)
                        smartHomeState.scannedDevices.forEach { device ->
                            ScannedDeviceItem(device) {
                                smartHomeViewModel.connect(device)
                            }
                        }
                    }
                }
                
                if (smartHomeState.connectedDevices.isEmpty() && smartHomeState.scannedDevices.isEmpty() && !smartHomeState.isScanning) {
                     PremiumGlassCard(
                         modifier = Modifier
                             .fillMaxWidth()
                             .height(100.dp)
                             .tilt3D(defaultRotationX = 0f, defaultRotationY = 0f, maxRotationX = 5f, maxRotationY = 8f),
                         borderColor = RoyalGold.copy(alpha = 0.65f)
                     ) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "My Rules",
                    color = RoyalGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(
                    onClick = { showAddRuleDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Rule", tint = RoyalGold)
                }
            }

            // Rule Templates Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Rule Templates",
                    color = RoyalGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(defaultRuleTemplates) { template ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = template.accentColor.copy(alpha = 0.10f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(200.dp)
                                .height(115.dp)
                                .border(1.2.dp, template.accentColor.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                                .clickable {
                                    if (template.requiresVerification && !actionsVerified) {
                                        Toast.makeText(context, "${template.title} unavailable: automations unverified", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    scope.launch(Dispatchers.IO) {
                                        val rule = AutomationRule(
                                            name = template.title,
                                            triggerType = template.triggerType,
                                            actionType = template.actionType,
                                            triggerParams = template.triggerParams,
                                            actionParams = template.actionParams
                                        )
                                        db.automationDao().insertRule(rule)
                                    }
                                    Toast.makeText(context, "Added: ${template.title}", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(template.icon, null, tint = template.accentColor, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        template.title,
                                        color = template.accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    template.description,
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    lineHeight = 13.sp
                                )
                                Text(
                                    "+ Tap to apply",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (rules.isEmpty()) {
                    Text("No automatic rules set.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    rules.forEach { rule ->
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
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showAddRuleDialog) {
            CreateAutomationRuleDialog(
                onDismiss = { showAddRuleDialog = false },
                onConfirm = { name, triggerType, actionType ->
                    scope.launch(Dispatchers.IO) {
                        db.automationDao().insertRule(
                            AutomationRule(
                                name = name,
                                triggerType = triggerType,
                                actionType = actionType
                            )
                        )
                    }
                    showAddRuleDialog = false
                }
            )
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAutomationRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, trigger: TriggerType, action: ActionType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedTrigger by remember { mutableStateOf(TriggerType.TIME_SCHEDULE) }
    var selectedAction by remember { mutableStateOf(ActionType.ALERT_PROMPT) }
    var triggerExpanded by remember { mutableStateOf(false) }
    var actionExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, RoyalGold.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("New Automation Rule", color = RoyalGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                Text("Pick a template (optional):", color = Color.Gray, fontSize = 11.sp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(defaultRuleTemplates) { t ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (name == t.title) RoyalGold.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, if (name == t.title) RoyalGold else Color.Gray.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                name = t.title
                                selectedTrigger = t.triggerType
                                selectedAction = t.actionType
                            }
                        ) {
                            Text(
                                t.title,
                                color = if (name == t.title) RoyalGold else Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalGold,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = RoyalGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Trigger dropdown
                ExposedDropdownMenuBox(expanded = triggerExpanded, onExpandedChange = { triggerExpanded = it }) {
                    OutlinedTextField(
                        value = selectedTrigger.name.replace('_', ' '),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Trigger", color = Color.Gray) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(triggerExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalGold,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = triggerExpanded,
                        onDismissRequest = { triggerExpanded = false },
                        modifier = Modifier.background(Color(0xFF1A1A2E))
                    ) {
                        TriggerType.entries.forEach { trigger ->
                            DropdownMenuItem(
                                text = { Text(trigger.name.replace('_', ' '), color = Color.White) },
                                onClick = { selectedTrigger = trigger; triggerExpanded = false }
                            )
                        }
                    }
                }

                // Action dropdown
                ExposedDropdownMenuBox(expanded = actionExpanded, onExpandedChange = { actionExpanded = it }) {
                    OutlinedTextField(
                        value = selectedAction.name.replace('_', ' '),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Action", color = Color.Gray) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(actionExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalGold,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = actionExpanded,
                        onDismissRequest = { actionExpanded = false },
                        modifier = Modifier.background(Color(0xFF1A1A2E))
                    ) {
                        ActionType.entries.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.name.replace('_', ' '), color = Color.White) },
                                onClick = { selectedAction = action; actionExpanded = false }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) { Text("Cancel") }

                    val dialogContext = LocalContext.current
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name.trim(), selectedTrigger, selectedAction)
                            } else {
                                Toast.makeText(dialogContext, "Please enter a rule name", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalGold,
                            contentColor = Color.Black
                        )
                    ) { Text("Create", fontWeight = FontWeight.Bold) }
                }
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
