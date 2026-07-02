package com.royalshield.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.royalshield.app.ui.theme.RoyalGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PairingDialog(
    code: String,
    onDismiss: () -> Unit
) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(code) {
        qrBitmap = generateQrCode("{\"type\":\"pair\",\"code\":\"$code\"}") // JSON payload for safer parsing
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = Color(0xFF1E1E1E), // Dark theme surface
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Scan to Pair",
                    style = MaterialTheme.typography.headlineSmall,
                    color = RoyalGold
                )
                Spacer(Modifier.height(16.dp))
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    qrBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Pairing QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: CircularProgressIndicator(color = RoyalGold)
                }
                
                Spacer(Modifier.height(16.dp))
                Text(
                    "Code: $code",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    "Enter this code on the child device",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)
                ) {
                    Text("Close", color = Color.Black)
                }
            }
        }
    }
}

suspend fun generateQrCode(content: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val w = bitMatrix.width
            val h = bitMatrix.height
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
            for (x in 0 until w) {
                for (y in 0 until h) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
@Composable
fun AddZoneDialog(
    draftZone: com.royalshield.app.features.trackingshield.data.SafeZone,
    onUpdate: (String, Double, com.royalshield.app.features.trackingshield.data.ZoneType) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(draftZone.name) }
    var radius by remember { mutableFloatStateOf(draftZone.radiusMeters.toFloat()) }
    var selectedType by remember { mutableStateOf(draftZone.type) }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = Color(0xFF1E1E1E),
            modifier = Modifier.padding(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGold.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Set Safe Zone",
                    style = MaterialTheme.typography.headlineSmall,
                    color = RoyalGold,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; onUpdate(it, radius.toDouble(), selectedType) },
                    label = { Text("Zone Name", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RoyalGold,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Radius: ${radius.toInt()}m", color = Color.White)
                Slider(
                    value = radius,
                    onValueChange = { radius = it; onUpdate(name, it.toDouble(), selectedType) },
                    valueRange = 100f..1000f,
                    colors = SliderDefaults.colors(thumbColor = RoyalGold, activeTrackColor = RoyalGold)
                )

                Text("Type", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    com.royalshield.app.features.trackingshield.data.ZoneType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type; onUpdate(name, radius.toDouble(), type) },
                            label = { Text(type.name, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalGold.copy(alpha = 0.2f),
                                selectedLabelColor = RoyalGold
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)
                    ) {
                        Text("Create Zone", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
