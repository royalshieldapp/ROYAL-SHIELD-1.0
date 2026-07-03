package com.royalshield.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.features.ir.models.ChecklistItem
import com.royalshield.app.features.ir.models.Incident
import com.royalshield.app.features.ir.models.TimelineEvent
import com.royalshield.app.features.ir.viewmodels.IrDetailUiState
import com.royalshield.app.features.ir.viewmodels.IrIncidentDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrIncidentDetailScreen(
    incidentId: String,
    onBack: () -> Unit,
) {
    // Factory for ViewModel to pass incidentId
    val factory = object : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return IrIncidentDetailViewModel(incidentId) as T
        }
    }
    val viewModel: IrIncidentDetailViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Timeline, 1: Playbook

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("INCIDENT DETAILS", color = Color.White, fontWeight = FontWeight.Bold) },
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
        when (val state = uiState) {
            is IrDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF1744))
                }
            }
            is IrDetailUiState.Content -> {
                Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                    IncidentInfoCard(state.incident)
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFFFF1744),
                        divider = {}
                    ) {
                        Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                            Text("TIMELINE", modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                            Text("PLAYBOOK", modifier = Modifier.padding(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (activeTab == 0) {
                            TimelineList(state.timeline) { action, who ->
                                viewModel.addTimelineEvent(action, who)
                            }
                        } else {
                            PlaybookChecklist(state.checklist) { itemId, completed ->
                                viewModel.toggleChecklistItem(itemId, completed)
                            }
                        }
                    }
                }
            }
            is IrDetailUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun IncidentInfoCard(incident: Incident) {
    Surface(
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(incident.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.weight(1f))
                Badge(containerColor = Color(0xFFFF1744)) { 
                    Text(incident.severity.name, color = Color.White, modifier = Modifier.padding(4.dp)) 
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("TYPE: ${incident.type.name}", color = Color.Gray, fontSize = 12.sp)
            Text("ASSETS: ${incident.affectedAssets}", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun TimelineList(timeline: List<TimelineEvent>, onAddEvent: (String, String) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column {
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("LOG ACTION")
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(timeline) { event ->
                Row(verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(12.dp).background(Color(0xFFFF1744), CircleShape))
                        Box(Modifier.width(1.dp).height(40.dp).background(Color(0xFF333333)))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(event.actionTaken, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(event.who, color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var action by remember { mutableStateOf("") }
        var who by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("LOG EVENT", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = action, onValueChange = { action = it }, label = { Text("Action Taken") })
                    TextField(value = who, onValueChange = { who = it }, label = { Text("Performed By") })
                }
            },
            confirmButton = {
                Button(onClick = { onAddEvent(action, who); showAddDialog = false }) { Text("SAVE") }
            }
        )
    }
}

@Composable
fun PlaybookChecklist(items: List<ChecklistItem>, onToggle: (String, Boolean) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { item ->
            Surface(
                color = Color(0xFF111111),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isCompleted,
                        onCheckedChange = { onToggle(item.id, it) },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF1744))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        item.title,
                        color = if (item.isCompleted) Color.Gray else Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
