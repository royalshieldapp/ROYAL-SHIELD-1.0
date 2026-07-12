package com.royalshield.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.royalshield.app.R
import com.royalshield.app.ui.viewmodels.FileScanViewModel
import com.royalshield.app.ui.viewmodels.ScanState
import com.royalshield.app.managers.BillingManager
import com.royalshield.app.ui.theme.RoyalGold
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileScanScreen(
    onBack: () -> Unit,
    billingManager: BillingManager,
    onNavigateToPremium: () -> Unit
) {
    val viewModel: FileScanViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val scanState by viewModel.scanState.collectAsState()
    val hasPremium by billingManager.hasPremiumAccess.collectAsState()
    val context = LocalContext.current
    var showUpgradeDialog by remember { mutableStateOf(false) }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.scanFile(context, it) }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) pickFileLauncher.launch("*/*")
    }

    com.royalshield.app.ui.components.RoyalGradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("File Scanner") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Image(
                                painter = painterResource(id = R.drawable.btn_back_gold),
                                contentDescription = "Back",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = RoyalGold,
                        navigationIconContentColor = RoyalGold
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_automation_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.35f),
                            0.65f to Color.Black.copy(alpha = 0.72f),
                            1.0f to Color.Black.copy(alpha = 0.9f)
                        )
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                when (val state = scanState) {
                    is ScanState.Idle -> {
                        Image(
                            painter = painterResource(id = R.drawable.scan_files), 
                            contentDescription = null, 
                            modifier = Modifier.size(120.dp)
                        )
                        Button(onClick = {
                            if (!hasPremium) {
                                showUpgradeDialog = true
                            } else {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                        pickFileLauncher.launch("*/*")
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                } else {
                                    pickFileLauncher.launch("*/*")
                                }
                            }
                        }) {
                            Text("Select File to Scan")
                        }
                    }
                    is ScanState.Scanning -> {
                        com.royalshield.app.ui.components.CyberLoadingIndicator(
                            text = "Analyzing file hash...",
                            size = 160.dp
                        )
                    }
                    is ScanState.Result -> {
                         if (state.isThreat) {
                             ThreatDetectedView(state.message) { viewModel.reset() }
                         } else {
                             CleanFileView(state.message) { viewModel.reset() }
                         }
                    }
                    is ScanState.Error -> {
                        Text(state.error, color = Color.Red)
                        Button(onClick = { viewModel.reset() }) { Text("Try Again") }
                    }
                }
            }
        }
    }
    }
        if (showUpgradeDialog) {
            AlertDialog(
                onDismissRequest = { showUpgradeDialog = false },
                containerColor = Color(0xFF1A1A2E),
                title = { Text("PREMIUM REQUIRED", color = RoyalGold, fontWeight = FontWeight.Bold) },
                text = { Text("Deep File Scanning requires a Starter plan or higher.", color = Color.White) },
                confirmButton = {
                    Button(
                        onClick = { 
                            showUpgradeDialog = false
                            onNavigateToPremium() 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)
                    ) {
                        Text("UPGRADE NOW", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpgradeDialog = false }) {
                        Text("Not Now", color = Color.Gray)
                    }
                }
            )
        }
}

@Composable
fun ThreatDetectedView(message: String, onReset: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Fallback to Icon if image resource missing, or use local painter if known
        // Assuming R.drawable.threat_detected exists as per existing Activity
        val painter = painterResource(id = R.drawable.malware_skull)
        Image(
            painter = painter,
            contentDescription = "Threat Detected",
            modifier = Modifier.size(120.dp),
            colorFilter = ColorFilter.tint(Color.Red)
        )
        Text("THREAT DETECTED", color = Color.Red, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onReset, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
            Text("Details / Scan Another")
        }
    }
}

@Composable
fun CleanFileView(message: String, onReset: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Shield, null, tint = Color.Green, modifier = Modifier.size(120.dp))
        Text("No Threats Found", color = Color.Green, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onReset) {
            Text("Scan Another")
        }
    }
}
