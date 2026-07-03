package com.royalshield.app.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.BorderStroke
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.features.intel.models.*
import com.royalshield.app.features.intel.viewmodels.IntelHubUiState
import com.royalshield.app.features.intel.viewmodels.IntelHubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntelHubScreen(
    onNavigateToAlertDetail: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: IntelHubViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Exposure, 1: Indicators, 2: Alerts

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("THREAT RADAR", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Tab Header
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Black,
                contentColor = Color(0xFF00E5FF),
                edgePadding = 16.dp,
                divider = {}
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("DARK WEB MONITOR", modifier = Modifier.padding(16.dp), fontWeight = if(selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("THREAT IOC", modifier = Modifier.padding(16.dp), fontWeight = if(selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("ALERTS", modifier = Modifier.padding(16.dp))
                }
            }

            when (val state = uiState) {
                is IntelHubUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF))
                    }
                }
                is IntelHubUiState.Content -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CompliancePanel(state.settings.complianceMode) {
                            viewModel.updateComplianceMode(it)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        when (selectedTab) {
                            0 -> ExposureList(state.monitors) { value, type -> viewModel.addMonitor(value, type) }
                            1 -> IocList(state.iocs) { value, type -> viewModel.addIoc(value, type) }
                            2 -> AlertList(state.alerts) { onNavigateToAlertDetail(it) }
                        }
                    }
                }
                is IntelHubUiState.Error -> {
                    Text("Error: ${state.message}", color = Color.Red, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun CompliancePanel(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("COMPLIANCE & SAFE MODE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                )
            }
            if (isEnabled) {
                Text(
                    "Passive monitoring only. No illegal interaction. Royal Shield security guidelines applied.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun ExposureList(monitors: List<Monitor>, onAdd: (String, String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    
    Column {
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RoyalGold.copy(alpha = 0.1f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null, tint = RoyalGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("MONITOR EMAIL / DOMAIN", color = RoyalGold, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(monitors) { monitor ->
                Surface(color = Color(0xFF0A0A0A), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (monitor.type == "EMAIL") Icons.Default.Email else Icons.Default.Public, contentDescription = null, tint = RoyalGold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(monitor.value, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Deep Web Surveillance Active", color = Color.Gray, fontSize = 10.sp)
                        }
                        Surface(
                            color = Color(0xFF00E676).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f))
                        ) {
                            Text("SAFE", color = Color(0xFF00E676), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var value by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("NEW MONITOR", color = Color.White) },
            text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Email or Domain") }) },
            confirmButton = {
                Button(onClick = { onAdd(value, "EMAIL"); showDialog = false }) { Text("MONITOR") }
            }
        )
    }
}

@Composable
fun IocList(iocs: List<IOC>, onAdd: (String, IocType) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(iocs) { ioc ->
            Surface(color = Color(0xFF0A0A0A), shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(12.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ioc.type.name, color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(ioc.confidence.name, color = Color.Gray, fontSize = 10.sp)
                    }
                    Text(ioc.value, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
fun AlertList(alerts: List<IntelAlert>, onClick: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(alerts) { alert ->
            Surface(
                modifier = Modifier.clickable { onClick(alert.id) },
                color = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(alert.title, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(alert.category.name, color = Color.Gray, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
        }
    }
}
