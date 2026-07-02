package com.royalshield.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.royalshield.app.managers.QrSecurityManager
import com.royalshield.app.managers.UrlSafety
import com.royalshield.app.ui.theme.NeonRed
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.Royal_shieldTheme
import com.royalshield.app.ui.theme.SafeGreen
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class QrScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Royal_shieldTheme {
                QrScannerScreen(onClose = { finish() })
            }
        }
    }
}

@Composable
fun QrScannerScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        CameraPreviewContent(onClose)
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black), 
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Camera permission required", 
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun CameraPreviewContent(onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var detectedUrl by remember { mutableStateOf<String?>(null) }
    var scanResult by remember { mutableStateOf<com.royalshield.app.managers.ScanResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var isVoiceUnlocking by remember { mutableStateOf(false) }
    var voiceUnlockSuccess by remember { mutableStateOf<Boolean?>(null) }
    var isDiscountApplying by remember { mutableStateOf(false) }
    var discountSuccess by remember { mutableStateOf<Boolean?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = Executors.newSingleThreadExecutor()
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        processImageProxy(imageProxy) { barcodes ->
                            if (detectedUrl == null && barcodes.isNotEmpty()) {
                                val barcode = barcodes.first()
                                val rawText = barcode.rawValue ?: barcode.displayValue
                                if (!rawText.isNullOrBlank()) {
                                    detectedUrl = rawText
                                    isAnalyzing = true
                                }
                            }
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("QrScanner", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Overlay HUD
        ScannerOverlay()

        // Close Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        // Analysis Dialog
        if (isAnalyzing && detectedUrl != null) {
            val url = detectedUrl!!
            
            // Check for Pairing Code
            var isPairing by remember { mutableStateOf(false) }
            var pairingSuccess by remember { mutableStateOf<Boolean?>(null) }
            
            LaunchedEffect(url) {
                try {
                    val rawUrl = url.trim()
                    if (rawUrl.equals("shield26", ignoreCase = true)) {
                        isDiscountApplying = true
                        com.royalshield.app.managers.PreferencesManager.setDiscountApplied(true)
                        discountSuccess = true
                    } else {
                        val json = org.json.JSONObject(url)
                        if (json.optString("type") == "pair") {
                            isPairing = true
                            val code = json.getString("code")
                            val repo = com.royalshield.app.features.trackingshield.data.FamilyRepository(context)
                            val parentUid = repo.linkToParent(code, com.royalshield.app.managers.PreferencesManager.getUserName() ?: "Child Device")
                            
                            if (parentUid != null) {
                                com.royalshield.app.managers.PreferencesManager.setDeviceRole("CHILD")
                                com.royalshield.app.managers.PreferencesManager.setParentUid(parentUid)
                                pairingSuccess = true
                                
                                // Start Location Service
                                val intent = Intent(context, com.royalshield.app.services.LocationTrackerService::class.java)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            } else {
                                pairingSuccess = false
                            }
                        } else if (json.optString("type") == "voice_unlock") {
                            isVoiceUnlocking = true
                            val token = json.optString("token")
                            if (token == "ROYAL-VOICE-UNLOCKED-2026") {
                                com.royalshield.app.managers.PreferencesManager.setVoiceUnlocked(true)
                                voiceUnlockSuccess = true
                            } else {
                                voiceUnlockSuccess = false
                            }
                        } else if (json.optString("type") == "discount" && json.optString("code").equals("shield26", ignoreCase = true)) {
                            isDiscountApplying = true
                            com.royalshield.app.managers.PreferencesManager.setDiscountApplied(true)
                            discountSuccess = true
                        } else {
                            scanResult = QrSecurityManager.analyzeUrl(url)
                        }
                    }
                } catch (e: Exception) {
                    val rawUrl = url.trim()
                    if (rawUrl.equals("shield26", ignoreCase = true)) {
                        isDiscountApplying = true
                        com.royalshield.app.managers.PreferencesManager.setDiscountApplied(true)
                        discountSuccess = true
                    } else {
                        // Not JSON or Not Pairing -> Standard URL Scan
                        scanResult = QrSecurityManager.analyzeUrl(url)
                    }
                }
                isAnalyzing = false
            }
            
            if (isVoiceUnlocking) {
                 Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (voiceUnlockSuccess == null) {
                            CircularProgressIndicator(color = RoyalGold)
                            Spacer(Modifier.height(16.dp))
                            Text("Verifying authorization...", color = RoyalGold, fontWeight = FontWeight.Bold)
                        } else if (voiceUnlockSuccess == true) {
                            Icon(Icons.Default.CheckCircle, null, tint = SafeGreen, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Voice Assistant Unlocked!", color = SafeGreen, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)) {
                                Text("Continue", color = Color.Black)
                            }
                        } else {
                             Icon(Icons.Default.Error, null, tint = NeonRed, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Invalid Unlock Token", color = NeonRed, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = {
                                isVoiceUnlocking = false
                                detectedUrl = null
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                                Text("Try Again", color = Color.White)
                            }
                        }
                    }
                }
            }
            
            if (isDiscountApplying) {
                 Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (discountSuccess == null) {
                            CircularProgressIndicator(color = RoyalGold)
                            Spacer(Modifier.height(16.dp))
                            Text("Applying discount...", color = RoyalGold, fontWeight = FontWeight.Bold)
                        } else if (discountSuccess == true) {
                            Icon(Icons.Default.CheckCircle, null, tint = SafeGreen, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("10% Discount Applied!", color = SafeGreen, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Your subscription prices are now discounted.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)) {
                                Text("Continue", color = Color.Black)
                            }
                        } else {
                             Icon(Icons.Default.Error, null, tint = NeonRed, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Failed to Apply Discount", color = NeonRed, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = {
                                isDiscountApplying = false
                                detectedUrl = null
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                                Text("Try Again", color = Color.White)
                            }
                        }
                    }
                }
            }

            if (isPairing) {
                 Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (pairingSuccess == null) {
                            CircularProgressIndicator(color = RoyalGold)
                            Spacer(Modifier.height(16.dp))
                            Text("Pairing with Parent...", color = RoyalGold, fontWeight = FontWeight.Bold)
                        } else if (pairingSuccess == true) {
                            Icon(Icons.Default.CheckCircle, null, tint = SafeGreen, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Successfully Linked!", color = SafeGreen, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)) {
                                Text("Continue", color = Color.Black)
                            }
                        } else {
                             Icon(Icons.Default.Error, null, tint = NeonRed, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Pairing Failed", color = NeonRed, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = {
                                isPairing = false
                                detectedUrl = null
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                                Text("Try Again", color = Color.White)
                            }
                        }
                    }
                }
            } else {
                // Loading Overlay for URL Scan
                 Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = RoyalGold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyzing link security...", color = RoyalGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Result Dialog (Standard URL)
        scanResult?.let { result ->
             ScanResultDialog(
                 result = result,
                 onDismiss = {
                     scanResult = null
                     detectedUrl = null
                 },
                 onOpen = {
                     val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.url))
                     context.startActivity(intent)
                     onClose() // Close scanner after opening
                 }
             )
        }
    }
}

@Composable
fun ScanResultDialog(
    result: com.royalshield.app.managers.ScanResult,
    onDismiss: () -> Unit,
    onOpen: () -> Unit
) {
    val typography = MaterialTheme.typography
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        icon = {
            Icon(
                imageVector = when (result.safety) {
                    UrlSafety.SAFE -> Icons.Default.CheckCircle
                    UrlSafety.SUSPICIOUS -> Icons.Default.Warning
                    UrlSafety.MALICIOUS -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when (result.safety) {
                    UrlSafety.SAFE -> SafeGreen
                    UrlSafety.SUSPICIOUS -> RoyalGold
                    UrlSafety.MALICIOUS -> NeonRed
                },
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = when (result.safety) {
                    UrlSafety.SAFE -> "SECURE LINK"
                    UrlSafety.SUSPICIOUS -> "SUSPICIOUS LINK"
                    UrlSafety.MALICIOUS -> "MALICIOUS ALERT"
                },
                style = typography.headlineSmall,
                color = when (result.safety) {
                    UrlSafety.SAFE -> SafeGreen
                    UrlSafety.SUSPICIOUS -> RoyalGold
                    UrlSafety.MALICIOUS -> NeonRed
                }
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = result.url,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                result.threatType?.let {
                    Text(
                        it.uppercase(),
                        color = Color.White,
                        style = typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                } ?: Text("NO THREATS DETECTED", color = Color.LightGray, style = typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = onOpen,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (result.safety) {
                        UrlSafety.MALICIOUS -> NeonRed
                        else -> RoyalGold
                    },
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("OPEN LINK", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val boxSize = 250.dp.toPx()
        val left = (width - boxSize) / 2
        val top = (height - boxSize) / 2

        // Darken outside
        drawRect(Color.Black.copy(alpha = 0.5f), size = Size(width, top)) // Top
        drawRect(Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, top + boxSize), size = Size(width, height - (top + boxSize))) // Bottom
        drawRect(Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, top), size = Size(left, boxSize)) // Left
        drawRect(Color.Black.copy(alpha = 0.5f), topLeft = Offset(left + boxSize, top), size = Size(width - (left + boxSize), boxSize)) // Right

        // Corners
        val cornerLength = 40.dp.toPx()
        val stroke = 4.dp.toPx()
        val color = RoyalGold

        // Top Left
        drawLine(color, Offset(left, top), Offset(left + cornerLength, top), stroke)
        drawLine(color, Offset(left, top), Offset(left, top + cornerLength), stroke)

        // Top Right
        drawLine(color, Offset(left + boxSize, top), Offset(left + boxSize - cornerLength, top), stroke)
        drawLine(color, Offset(left + boxSize, top), Offset(left + boxSize, top + cornerLength), stroke)

        // Bottom Left
        drawLine(color, Offset(left, top + boxSize), Offset(left + cornerLength, top + boxSize), stroke)
        drawLine(color, Offset(left, top + boxSize), Offset(left, top + boxSize - cornerLength), stroke)

        // Bottom Right
        drawLine(color, Offset(left + boxSize, top + boxSize), Offset(left + boxSize - cornerLength, top + boxSize), stroke)
        drawLine(color, Offset(left + boxSize, top + boxSize), Offset(left + boxSize, top + boxSize - cornerLength), stroke)
        
        // Scanning Line
        drawLine(
            color = RoyalGold.copy(alpha = 0.8f),
            start = Offset(left, top + (boxSize * alpha)),
            end = Offset(left + boxSize, top + (boxSize * alpha)),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    onBarcodeDetected: (List<Barcode>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                onBarcodeDetected(barcodes)
                imageProxy.close()
            }
            .addOnFailureListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
