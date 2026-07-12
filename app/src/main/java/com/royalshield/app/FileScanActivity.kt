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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.royalshield.app.ui.theme.Royal_shieldTheme
import java.io.File

/**
 * Activity that allows the user to pick a file, runs a mock malware scan, and shows the result.
 * When a threat is detected, it displays an alert image and the text "Thread Detected".
 */
@OptIn(ExperimentalMaterial3Api::class)
class FileScanActivity : ComponentActivity() {
    // Initialize with context
    private lateinit var scanner: MalwareScanner
    
    // State lifted to Activity to be accessible by callbacks
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Image (Golden Folder)
            Image(
                painter = painterResource(id = R.drawable.scan_files),
                contentDescription = "Scan Folder",
                modifier = Modifier.size(120.dp)
            )

            Button(
                onClick = {
                    // Request storage permission on Android < 13
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

            when (threatDetected) {
                true -> ThreatDetectedView()
                false -> Text("No threat detected.", color = com.royalshield.app.ui.theme.RoyalGold)
                null -> { /* Initial state, do nothing */ }
            }
        }
    }

    private fun handleFileUri(uri: Uri) {
        // Convert Uri to File (simplified for demo; real code should handle content resolver)
        val file = File(uri.path ?: "")
        // Run mock scan on background thread
        scanning = true
        threatDetected = null // Reset previous result
        

        
        lifecycleScope.launch(Dispatchers.IO) {
            val result = scanner.scanFile(file)
            withContext(Dispatchers.Main) {
                scanning = false
                threatDetected = result
            }
        }
    }
}

    @Composable
    fun ThreatDetectedView() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // The image should be placed in res/drawable/threat_detected.png
            val painter: Painter = painterResource(id = R.drawable.malware_skull)
            Image(
                painter = painter,
                contentDescription = "Threat Detected",
                modifier = Modifier.size(120.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error)
            )
            Text(
                text = "Threat Detected",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
