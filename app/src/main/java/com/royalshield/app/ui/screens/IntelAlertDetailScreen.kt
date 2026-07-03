package com.royalshield.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.features.intel.models.IntelAlert
import com.royalshield.app.features.intel.viewmodels.IntelAlertDetailUiState
import com.royalshield.app.features.intel.viewmodels.IntelAlertDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntelAlertDetailScreen(
    alertId: String,
    onBack: () -> Unit
) {
    val factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IntelAlertDetailViewModel(alertId) as T
        }
    }
    val viewModel: IntelAlertDetailViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("THREAT ADVISORY", color = Color.White, fontWeight = FontWeight.Bold) },
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
            is IntelAlertDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00E5FF))
                }
            }
            is IntelAlertDetailUiState.Content -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    AlertHeader(state.alert)
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("RECOMMENDED ACTIONS", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    ActionGuideItem("Force Password Reset", "Highly recommended for credential leaks.")
                    ActionGuideItem("Enable MFA (Guide)", "Add an extra layer of protection.")
                    ActionGuideItem("Block Domain (Guide)", "Prevent future interactions with this source.")
                }
            }
            is IntelAlertDetailUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun AlertHeader(alert: IntelAlert) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF00E5FF))
                Spacer(modifier = Modifier.width(12.dp))
                Text(alert.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(alert.description.ifBlank { "Priority threat detected. Immediate review required." }, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun ActionGuideItem(title: String, description: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(description, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.Default.Launch, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
        }
    }
}
