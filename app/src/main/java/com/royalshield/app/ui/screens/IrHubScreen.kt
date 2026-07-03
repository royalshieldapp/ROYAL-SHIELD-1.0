package com.royalshield.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.features.ir.models.Incident
import com.royalshield.app.features.ir.models.IncidentSeverity
import com.royalshield.app.features.ir.models.IncidentType
import com.royalshield.app.features.ir.models.IncidentStatus
import com.royalshield.app.features.ir.viewmodels.IrHubUiState
import com.royalshield.app.features.ir.viewmodels.IrHubViewModel

private val CyberCyan = Color(0xFF00E5FF)
private val RoyalGold = Color(0xFFFFD700)
private val CyberDark = Color(0xFF08090C)
private val CyberCardBg = Color(0xFF121318)
private val CyberBorder = Color(0xFF21232C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrHubScreen(
    onNavigateToDetail: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: IrHubViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "INCIDENT RESPONSE", 
                        color = Color.White, 
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CyberDark)
            )
        },
        containerColor = CyberDark
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is IrHubUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyberCyan)
                    }
                }
                is IrHubUiState.Content -> {
                    // Stats Grid
                    val activeCount = state.incidents.count { it.status == IncidentStatus.ACTIVE }
                    val resolvedCount = state.incidents.count { it.status == IncidentStatus.RESOLVED || it.status == IncidentStatus.CLOSED || it.status == IncidentStatus.MITIGATED }
                    val highSeverityCount = state.incidents.count { it.severity == IncidentSeverity.HIGH && it.status == IncidentStatus.ACTIVE }
                    val medLowSeverityCount = state.incidents.count { (it.severity == IncidentSeverity.MEDIUM || it.severity == IncidentSeverity.LOW) && it.status == IncidentStatus.ACTIVE }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            label = "ACTIVE THREATS",
                            value = activeCount.toString(),
                            icon = Icons.Default.Warning,
                            color = Color(0xFFFF1744),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "RESOLVED SYSTEM",
                            value = resolvedCount.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF00E676),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            label = "HIGH SEVERITY",
                            value = highSeverityCount.toString(),
                            icon = Icons.Default.Shield,
                            color = RoyalGold,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "LOW/MED THREATS",
                            value = medLowSeverityCount.toString(),
                            icon = Icons.Default.Info,
                            color = CyberCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Button (Start New Incident with gradient borders/glow)
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(listOf(CyberCyan, RoyalGold)),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(CyberCyan.copy(alpha = 0.15f), RoyalGold.copy(alpha = 0.15f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = CyberCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DECLARE INCIDENT STATUS", 
                                    fontSize = 14.sp, 
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "RECENT INCIDENTS LOG", 
                        color = Color.Gray, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (state.incidents.isEmpty()) {
                        EmptyState("No active threats. Systems operational.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.incidents) { incident ->
                                IncidentCard(incident) { onNavigateToDetail(incident.id) }
                            }
                        }
                    }
                }
                is IrHubUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("ERROR // ${state.message}", color = Color(0xFFFF1744), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateIncidentDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { title, type, severity, assets, notes ->
                    viewModel.startIncident(title, type, assets, notes) { id ->
                        showCreateDialog = false
                        onNavigateToDetail(id)
                    }
                }
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier.height(80.dp),
        color = CyberCardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(color.copy(alpha = 0.6f), color.copy(alpha = 0.1f))))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label, 
                    color = Color.Gray, 
                    fontSize = 9.sp, 
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value, 
                    color = Color.White, 
                    fontSize = 22.sp, 
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = color, 
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun IncidentCard(incident: Incident, onClick: () -> Unit) {
    val severityColor = when (incident.severity) {
        IncidentSeverity.HIGH -> Color(0xFFFF1744)
        IncidentSeverity.MEDIUM -> Color(0xFFFFD600)
        else -> CyberCyan
    }

    val statusText = incident.status.name
    val statusColor = when (incident.status) {
        IncidentStatus.ACTIVE -> Color(0xFFFF1744)
        IncidentStatus.MITIGATED -> CyberCyan
        IncidentStatus.RESOLVED, IncidentStatus.CLOSED -> Color(0xFF00E676)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = CyberCardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CyberBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Severity Indicator Line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(severityColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = incident.title, 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    // Status Badge
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = incident.type.name, 
                        color = Color.Gray, 
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (incident.affectedAssets.isNotBlank()) {
                        Text(
                            text = "Assets: ${incident.affectedAssets}", 
                            color = Color.Gray.copy(alpha = 0.7f), 
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateIncidentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, IncidentType, IncidentSeverity, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(IncidentType.OTHER) }
    var severity by remember { mutableStateOf(IncidentSeverity.MEDIUM) }
    var assets by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberDark,
        modifier = Modifier.border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
        title = { 
            Text(
                text = "DECLARE INCIDENT", 
                color = Color.White, 
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Incident Title", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CyberCyan,
                        focusedLabelColor = CyberCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Type Selection Dropdown Menu
                Box(modifier = Modifier.fillMaxWidth()) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = type.name,
                            onValueChange = {},
                            label = { Text("Incident Type", color = Color.Gray) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = CyberCyan
                            ),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(CyberDark).border(1.dp, CyberBorder)
                        ) {
                            IncidentType.values().forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.name, color = Color.White) },
                                    onClick = {
                                        type = t
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text("Severity Level", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IncidentSeverity.values().forEach { s ->
                        val chipColor = when(s) {
                            IncidentSeverity.HIGH -> Color(0xFFFF1744)
                            IncidentSeverity.MEDIUM -> Color(0xFFFFD600)
                            IncidentSeverity.LOW -> CyberCyan
                        }
                        val isSelected = severity == s
                        FilterChip(
                            selected = isSelected,
                            onClick = { severity = s },
                            label = { 
                                Text(
                                    text = s.name, 
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor,
                                containerColor = Color.Transparent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = chipColor.copy(alpha = 0.5f),
                                selectedBorderColor = chipColor,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = assets,
                    onValueChange = { assets = it },
                    label = { Text("Affected Assets", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CyberCyan,
                        focusedLabelColor = CyberCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title, type, severity, assets, notes) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = Color.Black
                )
            ) {
                Text("DEPLOY RESPONSE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace) 
            }
        }
    )
}

@Composable
fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Shield, 
                contentDescription = null, 
                modifier = Modifier.size(64.dp), 
                tint = CyberCyan.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message, 
                color = Color.Gray, 
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }
    }
}

