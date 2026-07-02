package com.royalshield.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.components.RoyalGradientBackground
import com.royalshield.app.ui.theme.CyberCyan
import com.royalshield.app.ui.theme.NeonRed
import com.royalshield.app.ui.theme.RoyalGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import com.royalshield.app.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import com.royalshield.app.VirusTotalRepository
import com.royalshield.app.UrlResult
import com.royalshield.app.managers.PreferencesManager

@Composable
fun PhishingDetectorScreen(onBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<PhishingResult?>(null) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val context = LocalContext.current
    val vtRepository = remember { VirusTotalRepository() }
    var showApiKeyWarning by remember { mutableStateOf(false) }

    RoyalGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Text(
                text = "SMS / Link Analysis",
                style = MaterialTheme.typography.headlineSmall,
                color = RoyalGold,
                fontWeight = FontWeight.Bold
            )
            // Header Image
            Image(
                painter = painterResource(id = R.drawable.bg_cyber_gold),
                contentDescription = "SMS Phishing",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NeonRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Pegue el mensaje sospechoso aquí:",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Input Field
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = CyberCyan
                ),
                placeholder = { Text("Ej: 'Tu cuenta ha sido bloqueada. Entra en http://bit.ly...'", color = Color.Gray) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
            )

            // Paste Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    clipboardManager.getText()?.text?.let { messageText = it }
                }) {
                    Icon(Icons.Default.Email, null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pegar del Portapapeles", color = CyberCyan)
                }
            }

            // Action Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp) // Adjusted height for the pill image
                    .clip(RoundedCornerShape(30.dp))
                    .clickable(enabled = !isAnalyzing) {
                        if (messageText.isNotEmpty()) {
                            isAnalyzing = true
                            scope.launch {
                                // 1. Run local heuristics first
                                val localResult = analyzePhishing(messageText)
                                result = localResult
                                
                                // 2. If a URL is found, attempt VirusTotal scan
                                val urlStr = extractUrl(messageText)
                                if (urlStr != null) {
                                    val vtResult = vtRepository.checkUrl(urlStr)
                                    if (vtResult.verdict == "unknown" && vtResult.reason.contains("API Key")) {
                                        showApiKeyWarning = true
                                    } else if (vtResult.verdict != "unknown") {
                                        // Merge VT results with local results
                                        val combinedReasons = localResult.reasons.toMutableList()
                                        combinedReasons.add("RESULTADO VIRUSTOTAL: ${vtResult.reason}")
                                        
                                        // Update score if VT is more certain
                                        val finalScore = maxOf(localResult.score, vtResult.score)
                                        result = PhishingResult(finalScore, combinedReasons)
                                    }
                                }
                                
                                isAnalyzing = false
                                delay(500) // Small breather
                            }
                        }
                    }
            ) {
                 // Background Image
                 Image(
                     painter = painterResource(id = R.drawable.btn_analyze_risk_gold),
                     contentDescription = null,
                     contentScale = ContentScale.FillBounds,
                     modifier = Modifier.fillMaxSize()
                 )

                 // Content
                 Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ANALIZANDO...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        // Text is part of image usually, but keeping it if needed, or removing if image has it. 
                        // Assuming image has text "Analyze Risk" or similar based on name.
                        // If image has text, we might want to hide this text.
                        // User said "CAMBIA el BOTON DE ANALIZAR RIESGO POR el BOTON_GOLD"
                        // I will keep text but make it invisible if needed? 
                        // Let's assume image is background.
                        Icon(Icons.Default.Analytics, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ANALIZAR RIESGO", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                 }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Result Area
            result?.let { r ->
                Spacer(modifier = Modifier.height(16.dp))
                PhishingResultCard(r)
            }

            if (showApiKeyWarning) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = NeonRed.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("API Key Faltante", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "Para un análisis profundo de URLs por VirusTotal, configure su API Key en Ajustes.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhishingResultCard(result: PhishingResult) {
    val riskColor = when {
        result.score >= 70 -> NeonRed
        result.score >= 40 -> RoyalGold
        else -> Color(0xFF00FF94) // Safe Green
    }
    
    val riskLabel = when {
        result.score >= 70 -> "ALTO"
        result.score >= 40 -> "MEDIO"
        else -> "BAJO"
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(riskColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (result.score >= 40) Icons.Default.Warning else Icons.Default.CheckCircle,
                        null,
                        tint = riskColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Nivel de Riesgo", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        text = "${result.score}% ($riskLabel)",
                        color = riskColor,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Risk Graph
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Risk Analysis Graph", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(result.score / 100f)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Green, Color.Yellow, Color.Red)
                                )
                            )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Safe", color = Color.Green, fontSize = 10.sp)
                    Text("Moderate", color = Color.Yellow, fontSize = 10.sp)
                    Text("Critical", color = Color.Red, fontSize = 10.sp)
                }
            }

            Text("Análisis de IA:", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            result.reasons.forEach { reason ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Text("•", color = riskColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(reason, color = Color.LightGray, fontSize = 14.sp)
                }
            }
        }
    }
}

data class PhishingResult(val score: Int, val reasons: List<String>)

fun extractUrl(text: String): String? {
    val regex = "(https?://[\\w\\d.-]+\\.[\\w\\d]{2,}[/\\w\\d._?%&=-]*)".toRegex()
    return regex.find(text)?.value
}

fun analyzePhishing(text: String): PhishingResult {
    val t = text.lowercase()
    var score = 0
    val reasons = mutableListOf<String>()

    // Heuristics
    if (t.contains("http") && !t.contains("https")) {
        score += 30
        reasons.add("Uso de protocolo HTTP inseguro (no cifrado).")
    }
    if (t.contains("bit.ly") || t.contains("goo.gl") || t.contains("tinyurl") || t.contains("is.gd")) {
        score += 25
        reasons.add("Uso de acortador de URL (común para ocultar destinos).")
    }
    if (t.contains("urgente") || t.contains("inmediato") || t.contains("acción requerida") || t.contains("bloqueada") || t.contains("suspendida")) {
        score += 30
        reasons.add("Lenguaje de urgencia o amenaza para forzar acción rápida.")
    }
    if (t.contains("banco") || t.contains("verificar") || t.contains("contraseña") || t.contains("ganador") || t.contains("premio")) {
        score += 20
        reasons.add("Solicitud de verificación o promesa de premio sospechosa.")
    }
    if (t.contains("haga clic") || t.contains("click aquí") || t.contains("entra en")) {
        score += 15
        reasons.add("Incitación directa a hacer clic en enlaces.")
    }
    
    // Normalize score
    if (reasons.isEmpty()) {
        score = 5
        reasons.add("No se detectaron patrones obvios de phishing, pero siempre mantenga precaución.")
    } else {
        // Base risk for any unsolicited message with links
        if (t.contains("http")) score += 10
    }
    
    return PhishingResult(score.coerceIn(0, 100), reasons)
}
