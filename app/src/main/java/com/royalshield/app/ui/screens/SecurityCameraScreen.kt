package com.royalshield.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.ui.viewmodels.SecurityCameraViewModel
import com.royalshield.app.ui.components.CameraRadarMap
import com.royalshield.app.ui.components.CameraMiniatureList
import com.royalshield.app.ui.components.ControlPanelList
import com.royalshield.app.ui.components.GoldColor
import com.royalshield.app.ui.components.DarkBackground

@Composable
fun SecurityCameraScreen(
    onNavigateBack: () -> Unit,
    viewModel: SecurityCameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    LaunchedEffect(state.useFrontCamera, state.isStreaming, hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect

        try {
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()

            if (state.isStreaming) {
                val preview = Preview.Builder().build()
                val cameraSelector = if (state.useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                previewView?.let { pv ->
                    preview.setSurfaceProvider(pv.surfaceProvider)
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("SecurityCameraScreen", "Failed to bind camera use cases: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Bar Custom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = GoldColor)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "GOLD CAM",
                    color = GoldColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "CAMERA SYSTEM",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
            IconButton(onClick = { /* Profile Action */ }) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = GoldColor)
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Section 1: Radar
            CameraRadarMap()

            // Section 2: Miniatures
            CameraMiniatureList()

            // Section 3: Active View (CameraX)
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "VISTA ACTIVA",
                    color = GoldColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, GoldColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (!hasCameraPermission) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Camera permissions are required.", color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldColor)
                            ) {
                                Text("Grant Permission", color = Color.Black)
                            }
                        }
                    } else if (!state.isStreaming) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Camera paused.", color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.toggleStreaming() },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldColor)
                            ) {
                                Text("Activate Camera", color = Color.Black)
                            }
                        }
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    previewView = this
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // LIVE Badge
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color(0x80000000), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isRecording) Color.Red else Color.Green)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (state.isRecording) "REC" else "LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        if (state.isRecording) {
                            val mins = state.recordingDurationSeconds / 60
                            val secs = state.recordingDurationSeconds % 60
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                color = Color.Red,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
                            )
                        }
                        
                        Text(
                            text = "CAM 02 - LOBBY (You)",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                        )

                        // Action Controls inside camera view
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ActionIcon(Icons.Default.PhotoCamera, onClick = {
                                viewModel.logPhotoCaptured()
                            })
                            ActionIcon(
                                if (state.isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                tint = if (state.isRecording) Color.Red else GoldColor,
                                onClick = {
                                    if (state.isRecording) viewModel.stopRecording() else viewModel.startRecording()
                                }
                            )
                            ActionIcon(
                                if (state.micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                                tint = if (state.micEnabled) GoldColor else Color.Gray,
                                onClick = { viewModel.toggleMic() }
                            )
                            ActionIcon(
                                if (state.flashEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                tint = if (state.flashEnabled) GoldColor else Color.Gray,
                                onClick = { viewModel.toggleFlash() }
                            )
                            ActionIcon(
                                if (state.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                onClick = { viewModel.toggleFullscreen() }
                            )
                        }
                    }
                }
            }

            // Section 4: Control Panel
            ControlPanelList()
            
            Spacer(modifier = Modifier.height(100.dp)) // Space for bottom nav
        }
    }
}

@Composable
fun ActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color = GoldColor, onClick: () -> Unit = {}) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(Color(0x80000000), CircleShape)
            .border(1.dp, GoldColor.copy(alpha = 0.3f), CircleShape)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}
