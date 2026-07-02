package com.royalshield.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.royalshield.app.ui.theme.RoyalGold
import java.text.SimpleDateFormat
import java.util.*

data class AuditEvent(
    val id: String,
    val timestamp: Long,
    val eventType: EventType,
    val severity: EventSeverity,
    val user: String,
    val action: String,
    val details: String,
    val ipAddress: String? = null
)

enum class EventType {
    SECURITY_SCAN, POLICY_CHANGE, USER_ACCESS, THREAT_BLOCKED, 
    DATA_ACCESS, SYSTEM_CONFIG, LOGIN_ATTEMPT, FILE_MODIFICATION
}

enum class EventSeverity {
    CRITICAL, WARNING, INFO, SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogsScreen(onBack: () -> Unit) {
    val events = remember { getAuditEvents() }
    var selectedFilter by remember { mutableStateOf<EventSeverity?>(null) }
    var selectedEvent by remember { mutableStateOf<AuditEvent?>(null) }
    
    val filteredEvents = if (selectedFilter != null) {
        events.filter { it.severity == selectedFilter }
    } else {
        events
    }
    
    com.royalshield.app.ui.components.RoyalGradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Audit Logs & Events", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.btn_back_gold),
                                contentDescription = "Back",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Export logs */ }) {
                            Icon(Icons.Default.Download, contentDescription = "Export", tint = RoyalGold)
                        }
                        IconButton(onClick = { /* Search */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = RoyalGold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent, // Transparent
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
                    .padding(16.dp)
            ) {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventCountCard(
                    "Total", 
                    events.size.toString(), 
                    Color.Gray,
                    modifier = Modifier.weight(1f),
                    isSelected = selectedFilter == null,
                    onClick = { selectedFilter = null }
                )
                EventCountCard(
                    "Critical", 
                    events.count { it.severity == EventSeverity.CRITICAL }.toString(), 
                    Color(0xFFFF5252),
                    modifier = Modifier.weight(1f),
                    isSelected = selectedFilter == EventSeverity.CRITICAL,
                    onClick = { selectedFilter = if (selectedFilter == EventSeverity.CRITICAL) null else EventSeverity.CRITICAL }
                )
                EventCountCard(
                    "Warning", 
                    events.count { it.severity == EventSeverity.WARNING }.toString(), 
                    Color(0xFFFF9800),
                    modifier = Modifier.weight(1f),
                    isSelected = selectedFilter == EventSeverity.WARNING,
                    onClick = { selectedFilter = if (selectedFilter == EventSeverity.WARNING) null else EventSeverity.WARNING }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Event Log",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${filteredEvents.size} events",
                color = Color.Gray,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredEvents) { event ->
                    AuditEventCard(
                        event = event,
                        onClick = { selectedEvent = event }
                    )
                }
            }
        }
    }
    }
    
    // Event Details Dialog
    selectedEvent?.let { event ->
        AlertDialog(
            onDismissRequest = { selectedEvent = null },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        getEventIcon(event.eventType),
                        contentDescription = null,
                        tint = getSeverityColor(event.severity),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(event.action, color = RoyalGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp)),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            text = {
                Column {
                    EventDetailRow("User", event.user)
                    EventDetailRow("Event Type", event.eventType.name.replace("_", " "))
                    EventDetailRow("Severity",event.severity.name)
                    event.ipAddress?.let {
                        EventDetailRow("IP Address", it)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(event.details, color = Color.Gray, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEvent = null }) {
                    Text("Close", color = RoyalGold)
                }
            }
        )
    }
}

@Composable
fun EventCountCard(
    label: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) color else Color(0xFF333333),
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.1f) else Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count, color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(label, color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
fun AuditEventCard(
    event: AuditEvent,
    onClick: () -> Unit
) {
    val severityColor = getSeverityColor(event.severity)
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Severity Indicator
            Surface(
                shape = CircleShape,
                color = severityColor.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        getEventIcon(event.eventType),
                        contentDescription = null,
                        tint = severityColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(event.action, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.timestamp)),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(event.user, color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    event.details,
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun EventDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

fun getSeverityColor(severity: EventSeverity): Color {
    return when (severity) {
        EventSeverity.CRITICAL -> Color(0xFFFF5252)
        EventSeverity.WARNING -> Color(0xFFFF9800)
        EventSeverity.INFO -> Color(0xFF2196F3)
        EventSeverity.SUCCESS -> Color(0xFF4CAF50)
    }
}

fun getEventIcon(type: EventType): ImageVector {
    return when (type) {
        EventType.SECURITY_SCAN -> Icons.Default.Security
        EventType.POLICY_CHANGE -> Icons.Default.Policy
        EventType.USER_ACCESS -> Icons.Default.Person
        EventType.THREAT_BLOCKED -> Icons.Default.Block
        EventType.DATA_ACCESS -> Icons.Default.Dataset
        EventType.SYSTEM_CONFIG -> Icons.Default.Settings
        EventType.LOGIN_ATTEMPT -> Icons.Default.Login
        EventType.FILE_MODIFICATION -> Icons.Default.Edit
    }
}

fun getAuditEvents(): List<AuditEvent> {
    val now = System.currentTimeMillis()
    return listOf(
        AuditEvent(
            "1",
            now - 300000,
            EventType.THREAT_BLOCKED,
            EventSeverity.CRITICAL,
            "System",
            "Malware Blocked",
            "Detected and quarantined suspicious APK: trojan.apk",
            "192.168.1.105"
        ),
        AuditEvent(
            "2",
            now - 600000,
            EventType.POLICY_CHANGE,
            EventSeverity.WARNING,
            "admin@company.com",
            "Policy Modified",
            "Updated firewall rules: Block social media during work hours",
            "10.0.0.12"
        ),
        AuditEvent(
            "3",
            now - 900000,
            EventType.SECURITY_SCAN,
            EventSeverity.SUCCESS,
            "System",
            "Security Scan Completed",
            "Full system scan completed. 0 threats found. 156 apps analyzed.",
            null
        ),
        AuditEvent(
            "4",
            now - 1200000,
            EventType.LOGIN_ATTEMPT,
            EventSeverity.WARNING,
            "john.doe@company.com",
            "Failed Login Attempt",
            "Multiple failed login attempts detected from unknown IP",
            "203.45.67.89"
        ),
        AuditEvent(
            "5",
            now - 1800000,
            EventType.DATA_ACCESS,
            EventSeverity.INFO,
            "jane.smith@company.com",
            "Sensitive Data Accessed",
            "User accessed customer database. Action logged for compliance.",
            "10.0.0.45"
        ),
        AuditEvent(
            "6",
            now - 3600000,
            EventType.SYSTEM_CONFIG,
            EventSeverity.INFO,
            "admin@company.com",
            "Configuration Updated",
            "Updated backup retention policy to 90 days",
            "10.0.0.12"
        ),
        AuditEvent(
            "7",
            now - 7200000,
            EventType.USER_ACCESS,
            EventSeverity.SUCCESS,
            "mike.johnson@company.com",
            "User Logged In",
            "Successful authentication via 2FA",
            "10.0.0.78"
        ),
        AuditEvent(
            "8",
            now - 10800000,
            EventType.FILE_MODIFICATION,
            EventSeverity.INFO,
            "sarah.williams@company.com",
            "File Modified",
            "Updated security_policy.pdf document",
            "10.0.0.34"
        )
    )
}
