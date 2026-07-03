package com.royalshield.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.data.db.AutomationRule
import com.royalshield.app.ui.theme.CyberCyan
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.SafeGreen
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// --- Smart Home Components ---

enum class DeviceType {
    LIGHT, PLUG, THERMOSTAT, CAMERA, LOCK, SPEAKER, TV, OTHER
}

data class SmartDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    var isConnected: Boolean = false,
    var isOn: Boolean = false,
    var brightness: Float = 1f, // 0f to 1f
    var lightColor: Color = Color(0xFFFFE4B5), // Warm white default
    val ipAddress: String = "192.168.1.${(100..200).random()}",
    val macAddress: String = "A1:B2:C3:D4:E5:${(10..99).random()}",
    val vendor: String = listOf("Apple", "Samsung", "TP-Link", "Google", "Amazon").random()
)

@Composable
fun ScannedDeviceItem(device: SmartDevice, onConnect: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onConnect() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BluetoothSearching, null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(device.name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${device.vendor} • ${device.ipAddress}", color = Color.Gray, fontSize = 11.sp)
                    Text(device.macAddress, color = Color.DarkGray, fontSize = 10.sp)
                }
            }
            Text("Connect", color = CyberCyan, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SmartDeviceControlCard(device: SmartDevice, onUpdate: (SmartDevice) -> Unit) {
    PremiumGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (device.isOn) device.lightColor else Color.Gray.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (device.type == DeviceType.LIGHT) Icons.Default.Lightbulb else Icons.Default.Power,
                                contentDescription = null,
                                tint = if (device.isOn) Color.Black else Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(device.name, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(if (device.isOn) "ON" else "OFF", color = if (device.isOn) SafeGreen else Color.Gray, fontSize = 12.sp)
                    }
                }
                
                Switch(
                    checked = device.isOn,
                    onCheckedChange = { isOn ->
                        onUpdate(device.copy(isOn = isOn))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RoyalGold,
                        checkedTrackColor = RoyalGold.copy(alpha = 0.5f)
                    )
                )
            }
            
            // Segmented Circular Slider for Light Brightness (User Requested Style)
            if (device.type == DeviceType.LIGHT && device.isOn) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Brightness Control
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    SegmentedCircularSlider(
                        value = device.brightness,
                        activeColor = device.lightColor,
                        onValueChange = { newBrightness ->
                            onUpdate(device.copy(brightness = newBrightness))
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Color Picker Panel
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "COLOR",
                        color = RoyalGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Color palette - Premium circular layout
                    val colorPalette = listOf(
                        Color(0xFFFFFFFF), // White
                        Color(0xFFFFE4B5), // Warm White
                        Color(0xFFFF6B6B), // Red
                        Color(0xFFFF9500), // Orange
                        Color(0xFFFFD700), // Gold
                        Color(0xFFFFFF00), // Yellow
                        Color(0xFF4CAF50), // Green
                        Color(0xFF00FFFF), // Cyan
                        Color(0xFF2196F3), // Blue
                        Color(0xFF9C27B0), // Purple
                        Color(0xFFFF1493), // Pink
                        Color(0xFFFF69B4), // Hot Pink
                    )
                    
                    // Arrange in 2 rows
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            colorPalette.take(6).forEach { color ->
                                ColorSwatch(
                                    color = color,
                                    isSelected = device.lightColor == color,
                                    onClick = {
                                        onUpdate(device.copy(lightColor = color))
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            colorPalette.drop(6).forEach { color ->
                                ColorSwatch(
                                    color = color,
                                    isSelected = device.lightColor == color,
                                    onClick = {
                                        onUpdate(device.copy(lightColor = color))
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// Extension for luminance calculation
fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}

@Composable
fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (isSelected) 42.dp else 36.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) {
                    Brush.radialGradient(
                        colors = listOf(
                            color,
                            color.copy(alpha = 0.8f)
                        )
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(color, color)
                    )
                }
            )
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(RoyalGold, RoyalGold.copy(alpha = 0.5f))
                        ),
                        shape = CircleShape
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SegmentedCircularSlider(
    value: Float, // 0f to 1f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 160.dp,
    segmentCount: Int = 60,
    activeColor: Color = Color(0xFFFF9800), // Orange/Gold
    inactiveColor: Color = Color.Gray.copy(alpha=0.3f), // Dim White/Gray
    indicatorColor: Color = Color.Red // The "needle" or last segment
) {
    var currentValue by remember { mutableStateOf(value) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Update internal state when external value changes
    LaunchedEffect(value) {
        currentValue = value
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val sizePx = with(density) { size.toPx() }
                    val center = Offset(sizePx / 2, sizePx / 2)
                    
                    detectDragGestures(
                        onDragStart = { offset ->
                             // Optional: Handle tap/start
                        },
                        onDrag = { change, _ ->
                            val touchPoint = change.position
                            // Calculate angle
                            val angleRad = atan2(touchPoint.y - center.y, touchPoint.x - center.x)
                            var angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                            
                            // Normalize: Top (-90 in atan2) -> 0
                            angleDeg += 90f 
                            if (angleDeg < 0) angleDeg += 360f
                            
                            val newValue = (angleDeg / 360f).coerceIn(0f, 1f)
                            currentValue = newValue
                            onValueChange(newValue)
                        }
                    )
                }
        ) {
            val center = this.center
            val radius = size.toPx() / 2
            val strokeWidth = 3.dp.toPx()
            val segmentLength = 10.dp.toPx()
            val innerRadius = radius - segmentLength
            
            val totalAngle = 360f
            val step = totalAngle / segmentCount
            
            for (i in 0 until segmentCount) {
                val angle = i * step - 90f 
                val angleRad = Math.toRadians(angle.toDouble())
                
                val startX = center.x + (innerRadius * cos(angleRad)).toFloat()
                val startY = center.y + (innerRadius * sin(angleRad)).toFloat()
                
                val endX = center.x + (radius * cos(angleRad)).toFloat()
                val endY = center.y + (radius * sin(angleRad)).toFloat()
                
                val segmentValue = i.toFloat() / segmentCount
                val isActive = segmentValue <= currentValue
                val color = if (isActive) activeColor else inactiveColor
                
                drawLine(
                   color = color,
                   start = Offset(startX, startY),
                   end = Offset(endX, endY),
                   strokeWidth = strokeWidth,
                   cap = StrokeCap.Round
                )
            }
        }
        
        Text(
            text = "${(currentValue * 100).toInt()}%",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = activeColor
        )
    }
}

// --- Network Tools Components ---

@Composable
fun NetworkToolItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CyberButtonRound(
            size = 56.dp,
            icon = icon,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(description, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

// --- Automation Rules Components ---

@Composable
fun AutomationRuleItem(
    rule: AutomationRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CompareArrows, null, tint = RoyalGold, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(rule.triggerType.name, color = RoyalGold, fontSize = 12.sp)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, null, tint = SafeGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(rule.actionType.name, color = SafeGreen, fontSize = 12.sp)
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberCyan,
                        checkedTrackColor = CyberCyan.copy(alpha = 0.5f)
                    )
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Gray)
                }
            }
        }
    }
}
