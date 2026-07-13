package com.royalshield.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.managers.AiManager
import com.royalshield.app.ui.theme.RoyalGold
import kotlinx.coroutines.launch

private enum class ResolutionStatus { INITIAL, ANALYZING, RESULTS, FAILED }

private data class ResolutionCategory(val name: String, val description: String, val icon: ImageVector, val prompt: String)

@Composable
fun SolutionEngineResolutionScreen(
    onBack: () -> Unit,
    onHistory: () -> Unit,
    onOpenFileScan: () -> Unit,
    onOpenPhishing: () -> Unit,
    onOpenVpn: () -> Unit,
    onOpenTracking: () -> Unit,
    onOpenAutomation: () -> Unit,
    onOpenGrid: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val aiManager = remember { AiManager() }
    var problem by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ResolutionCategory?>(null) }
    var status by remember { mutableStateOf(ResolutionStatus.INITIAL) }
    var result by remember { mutableStateOf<String?>(null) }
    var stage by remember { mutableStateOf(0) }
    val categories = remember {
        listOf(
            ResolutionCategory("Malware", "Suspicious app or file", Icons.Default.BugReport, "Threat Intelligence"),
            ResolutionCategory("Suspicious SMS", "Unsafe message or link", Icons.Default.Sms, "Suspicious SMS"),
            ResolutionCategory("Scam Call", "Caller requests sensitive data", Icons.Default.Call, "Scam Call"),
            ResolutionCategory("Network Risk", "Unsafe Wi-Fi or connection", Icons.Default.Wifi, "Network Risk"),
            ResolutionCategory("Privacy", "Permissions or tracking", Icons.Default.PrivacyTip, "Privacy"),
            ResolutionCategory("Data Breach", "Account or identity exposure", Icons.Default.Report, "Data Breach"),
            ResolutionCategory("Lost Device", "Device recovery support", Icons.Default.PhoneAndroid, "Lost Device"),
            ResolutionCategory("Emergency", "Immediate personal safety", Icons.Default.Warning, "Emergency"),
            ResolutionCategory("Smart Home", "Connected home concern", Icons.Default.AccountTree, "Smart Home"),
            ResolutionCategory("Other", "Describe another issue", Icons.Default.Security, "Other")
        )
    }
    val scan = rememberInfiniteTransition(label = "solution_engine_scan")
    val ringRotation by scan.animateFloat(0f, 360f, infiniteRepeatable(tween(7000), RepeatMode.Restart), label = "ring")
    val canAnalyze = problem.isNotBlank() || selectedCategory != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = RoyalGold) }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Solution Engine", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("AI-Powered Security Resolution", color = RoyalGold, fontSize = 11.sp)
                }
                IconButton(onClick = onHistory) { Icon(Icons.Default.History, "History logs", tint = RoyalGold) }
                IconButton(onClick = onOpenGrid) { Icon(Icons.Default.Grid4x4, "3D Shield Grid", tint = RoyalGold) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF00FF94)))
                    Spacer(Modifier.width(5.dp))
                    Text("Online", color = Color(0xFF00FF94), fontSize = 10.sp)
                }
            }

            SecurityHero(ringRotation = ringRotation, isAnalyzing = status == ResolutionStatus.ANALYZING)

            Panel(title = "Describe the problem") {
                OutlinedTextField(
                    value = problem,
                    onValueChange = { if (it.length <= 500) problem = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6,
                    placeholder = { Text("What security problem are you experiencing?", color = Color.Gray) },
                    trailingIcon = {
                        if (problem.isNotEmpty()) TextButton(onClick = { problem = "" }) { Text("Clear", color = RoyalGold) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = RoyalGold, unfocusedBorderColor = RoyalGold.copy(alpha = 0.35f),
                        cursorColor = RoyalGold, focusedLabelColor = RoyalGold
                    )
                )
                Text("${problem.length}/500", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
            }

            Text("Quick problem categories", color = RoyalGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 12.dp)
            ) {
                items(categories, key = { it.name }) { category ->
                    CategoryChip(
                        category,
                        category == selectedCategory,
                        Modifier.width(154.dp)
                    ) {
                        selectedCategory = if (selectedCategory == category) null else category
                    }
                }
            }

            Button(
                onClick = {
                    if (!canAnalyze || status == ResolutionStatus.ANALYZING) return@Button
                    val selected = selectedCategory
                    val prompt = "${selected?.prompt ?: "Security issue"}: ${problem.ifBlank { selected?.description.orEmpty() }}. Analyze only the reported issue. Clearly separate confirmed, suspected, user-reported, and not verified findings. Recommend existing Royal Shield tools."
                    status = ResolutionStatus.ANALYZING
                    result = null
                    stage = 0
                    scope.launch {
                        stage = 1
                        aiManager.analyzeThreat(prompt) { response ->
                            result = response
                            status = if (response.startsWith("Error", true) || response.contains("unavailable", true)) ResolutionStatus.FAILED else ResolutionStatus.RESULTS
                            stage = 4
                        }
                    }
                },
                enabled = canAnalyze && status != ResolutionStatus.ANALYZING,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = Color.Black)
            ) {
                if (status == ResolutionStatus.ANALYZING) CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                else Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text(if (status == ResolutionStatus.ANALYZING) "Analyzing..." else "Analyze Problem", fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(status == ResolutionStatus.ANALYZING) { AnalysisPipeline(stage) }
            result?.let { text ->
                ResolutionResult(text, status == ResolutionStatus.FAILED, onOpenFileScan, onOpenPhishing, onOpenVpn, onOpenTracking, onOpenAutomation)
            }
        }
    }
}

@Composable
private fun SecurityHero(ringRotation: Float, isAnalyzing: Boolean) {
    Panel(title = "Security status") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(116.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = ringRotation }.border(1.dp, RoyalGold.copy(alpha = 0.7f), CircleShape))
                Box(Modifier.size(92.dp).border(1.dp, RoyalGold.copy(alpha = 0.3f), CircleShape))
                Icon(Icons.Default.Lock, "Royal Shield", tint = RoyalGold, modifier = Modifier.size(46.dp))
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Text(if (isAnalyzing) "Analyzing security context" else "Ready to analyze", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Security score unavailable - run a security assessment.", color = Color.Gray, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Text("Device   Network   Privacy   Identity", color = RoyalGold.copy(alpha = 0.85f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.045f)).border(1.dp, RoyalGold.copy(alpha = 0.35f), RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = RoyalGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun CategoryChip(category: ResolutionCategory, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier = modifier.heightIn(min = 58.dp).clip(RoundedCornerShape(12.dp)).background(if (selected) RoyalGold.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.035f)).border(1.dp, if (selected) RoyalGold else Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(category.icon, category.name, tint = if (selected) RoyalGold else Color.LightGray, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Column { Text(category.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(category.description, color = Color.Gray, fontSize = 9.sp, maxLines = 2) }
    }
}

@Composable
private fun AnalysisPipeline(stage: Int) {
    Panel(title = "Analysis pipeline") {
        listOf("Understanding the issue", "Checking device context", "Matching Royal Shield tools", "Evaluating risk", "Preparing recommended actions").forEachIndexed { index, label ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (index < stage) Icons.Default.CheckCircle else Icons.Default.AccountTree, null, tint = if (index < stage) Color(0xFF00FF94) else Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp)); Text(label, color = if (index <= stage) Color.White else Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ResolutionResult(text: String, failed: Boolean, onOpenFileScan: () -> Unit, onOpenPhishing: () -> Unit, onOpenVpn: () -> Unit, onOpenTracking: () -> Unit, onOpenAutomation: () -> Unit) {
    Panel(title = if (failed) "Analysis failed" else "Analysis result") {
        Text(if (failed) "This security service is not currently configured or available." else "Risk assessment: not verified until the recommended tool completes.", color = if (failed) Color(0xFFFF8A80) else RoyalGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(text, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 18.sp)
        Text("Recommended actions", color = RoyalGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        ActionButton("Run Malware Scan", Icons.Default.BugReport, onOpenFileScan)
        ActionButton("Check Suspicious URL", Icons.Default.NetworkCheck, onOpenPhishing)
        ActionButton("Enable VPN", Icons.Default.Wifi, onOpenVpn)
        ActionButton("Review Tracking Shield", Icons.Default.Security, onOpenTracking)
        ActionButton("Open Smart Control", Icons.Default.AccountTree, onOpenAutomation)
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(RoyalGold.copy(alpha = 0.08f)).border(1.dp, RoyalGold.copy(alpha = 0.25f), RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = RoyalGold, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
