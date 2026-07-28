package com.royalshield.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.royalshield.app.ui.theme.Royal_shieldTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Activity that allows the user to pick a file, runs a malware scan, and shows the result.
 * When a threat is detected, it displays the visual "THREAT DETECTED" alert screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
class FileScanActivity : ComponentActivity() {
    private lateinit var scanner: MalwareScanner
    
    private var scanning by mutableStateOf(false)
    private var threatDetected by mutableStateOf<Boolean?>(null)

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleFileUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanner = MalwareScanner(this)
        
        setContent {
            Royal_shieldTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Scan File") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Image(
                                        painter = painterResource(id = R.drawable.btn_back_gold),
                                        contentDescription = "Back",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        FileScanScreen()
                    }
                }
            }
        }
    }

    @Composable
    fun FileScanScreen() {
        val context = LocalContext.current

        if (threatDetected == true) {
            ThreatDetectedView()
            return
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.scan_files),
                contentDescription = "Scan Folder",
                modifier = Modifier.size(120.dp)
            )

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
                        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(arrayOf(permission), 1001)
                            return@Button
                        }
                    }
                    pickFileLauncher.launch("*/*")
                },
                colors = ButtonDefaults.buttonColors(containerColor = com.royalshield.app.ui.theme.RoyalGold),
                enabled = !scanning,
                modifier = Modifier.fillMaxWidth(0.7f).height(50.dp)
            ) {
                Text(
                    text = if (scanning) "Scanning..." else "Select File to Scan",
                    color = androidx.compose.ui.graphics.Color.Black,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            if (scanning) {
                CircularProgressIndicator(color = com.royalshield.app.ui.theme.RoyalGold)
            }

            if (threatDetected == false) {
                Text("No threat detected.", color = com.royalshield.app.ui.theme.RoyalGold)
            }
        }
    }

    private fun handleFileUri(uri: Uri) {
        val file = File(uri.path ?: "")
        scanning = true
        threatDetected = null
        
        lifecycleScope.launch(Dispatchers.IO) {
            val result = scanner.scanFile(file)
            withContext(Dispatchers.Main) {
                scanning = false
                threatDetected = result
            }
        }
    }

    @Composable
    fun ThreatDetectedView() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.bg_threat_detected),
                contentDescription = "Threat Detected",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
