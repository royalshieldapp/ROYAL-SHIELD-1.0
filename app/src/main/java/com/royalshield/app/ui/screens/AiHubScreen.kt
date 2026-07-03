package com.royalshield.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.managers.AiManager
import com.royalshield.app.managers.BillingManager
import com.royalshield.app.managers.PreferencesManager
import com.royalshield.app.ui.components.CyberConsole
import com.royalshield.app.ui.components.GoldLuxFrame
import com.royalshield.app.ui.components.GoldLuxText
import com.royalshield.app.ui.components.RoyalGradientBackground
import com.royalshield.app.ui.components.ShieldStatusBackground
import com.royalshield.app.ui.theme.CyberCyan
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.SafeGreen

private val AiHubBackground = Color(0xFF05070A)
private val AiHubCard = Color(0xFF0C1117)
private val AiHubCardSoft = Color(0xFF111822)
private val AiHubBody = Color(0xFFB3BECC)
private val AiHubDanger = Color(0xFFFF7A7A)

private sealed interface AiHubAction {
    data object OpenScriptLab : AiHubAction
    data object LaunchEmergencyAssistant : AiHubAction
    data class Analyze(val moduleName: String) : AiHubAction
}

private data class AiHubModule(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val badge: String,
    val status: String,
    val actionLabel: String,
    val isPremium: Boolean,
    val action: AiHubAction
)

private data class AiHubResultStyle(
    val label: String,
    val accent: Color
)

@Composable
fun AiHubScreen(
    onBack: () -> Unit,
    billingManager: BillingManager,
    onNavigateToPremium: () -> Unit,
    onNavigateToScriptLab: () -> Unit
) {
    val aiManager = remember { AiManager() }
    val context = LocalContext.current
    val hasPremium by billingManager.hasPremiumAccess.collectAsState()
    val hasCloudAi = remember { !PreferencesManager.getGeminiApiKey().isNullOrBlank() }
    val scrollState = rememberScrollState()

    var showUpgradeDialog by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var activeModule by remember { mutableStateOf("") }

    val modules = remember {
        listOf(
            AiHubModule(
                title = "AI Script Lab",
                description = "Generate educational Python, Bash, PowerShell, and security lab scripts without leaving Royal Shield.",
                icon = Icons.Default.Code,
                color = Color(0xFFFFC857),
                badge = "Included",
                status = "Sandbox ready",
                actionLabel = "Open lab",
                isPremium = false,
                action = AiHubAction.OpenScriptLab
            ),
            AiHubModule(
                title = "Threat Radar",
                description = "Run deep analysis on suspicious files and URLs with neural summaries designed for rapid triage.",
                icon = Icons.Default.Psychology,
                color = RoyalGold,
                badge = "Elite",
                status = "Deep scan ready",
                actionLabel = "Run analysis",
                isPremium = true,
                action = AiHubAction.Analyze("Threat Intelligence")
            ),
            AiHubModule(
                title = "Emergency Assistant",
                description = "Launch voice-aware emergency protection flows for fast guidance and crisis escalation.",
                icon = Icons.AutoMirrored.Filled.Chat,
                color = Color(0xFFFF8A65),
                badge = "Live",
                status = "Voice ready",
                actionLabel = "Launch assistant",
                isPremium = false,
                action = AiHubAction.LaunchEmergencyAssistant
            ),
            AiHubModule(
                title = "Log Analyzer",
                description = "Audit system activity for unauthorized access attempts, hidden persistence, and suspicious patterns.",
                icon = Icons.Default.Terminal,
                color = CyberCyan,
                badge = "Elite",
                status = "Audit ready",
                actionLabel = "Inspect logs",
                isPremium = true,
                action = AiHubAction.Analyze("Log Analyzer")
            ),
            AiHubModule(
                title = "Skill Optimizer",
                description = "Surface AI-driven coaching and next-step recommendations to strengthen operator discipline.",
                icon = Icons.Default.School,
                color = Color(0xFF7DFFC1),
                badge = "Elite",
                status = "Training ready",
                actionLabel = "Get guidance",
                isPremium = true,
                action = AiHubAction.Analyze("Skill Optimizer")
            )
        )
    }

    val lockedModuleCount = modules.count { it.isPremium && !hasPremium }

    val onModuleClick: (AiHubModule) -> Unit = { module ->
        when (val action = module.action) {
            AiHubAction.OpenScriptLab -> onNavigateToScriptLab()
            AiHubAction.LaunchEmergencyAssistant -> {
                val intent = Intent(context, com.royalshield.app.VoiceScamActivity::class.java)
                context.startActivity(intent)
            }

            is AiHubAction.Analyze -> {
                if (module.isPremium && !hasPremium) {
                    showUpgradeDialog = true
                } else {
                    activeModule = module.title
                    isAnalyzing = true
                    resultText = null
                    aiManager.analyzeThreat(action.moduleName) { result ->
                        isAnalyzing = false
                        resultText = result
                    }
                }
            }
        }
    }

    RoyalGradientBackground(containerColor = AiHubBackground) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF030507),
                            Color(0xFF070B11),
                            Color(0xFF04070C),
                            Color(0xFF020406)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            RoyalGold.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI HUB v2.0",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Royal Shield intelligence center for analysis, response, and secure AI workflows.",
                            color = AiHubBody,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    AiStatusBadge(
                        text = if (hasPremium) "Elite" else "Free",
                        accent = if (hasPremium) RoyalGold else Color.White,
                        filled = hasPremium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                GoldLuxFrame {
                    GoldLuxText(
                        text = "Intelligence Center",
                        fontSize = 10,
                        letterSpacing = 1f
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                AiHubHeroCard(
                    hasPremium = hasPremium,
                    hasCloudAi = hasCloudAi,
                    isAnalyzing = isAnalyzing,
                    activeModule = activeModule,
                    totalModules = modules.size,
                    lockedModuleCount = lockedModuleCount
                )

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = "ADVANCED MODULES",
                    color = RoyalGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "All existing entries stay in place. This pass improves clarity, access visibility, and operational context.",
                    color = AiHubBody,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                if (!hasPremium) {
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumNoticeCard(
                        lockedModuleCount = lockedModuleCount,
                        onNavigateToPremium = onNavigateToPremium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                modules.forEach { module ->
                    val isLocked = module.isPremium && !hasPremium
                    AiFeatureItem(
                        title = module.title,
                        desc = module.description,
                        icon = module.icon,
                        color = module.color,
                        badge = if (isLocked) "Premium" else module.badge,
                        status = if (isLocked) "Unlock required" else module.status,
                        actionLabel = if (isLocked) "Upgrade to use" else module.actionLabel,
                        isPremium = module.isPremium,
                        isLocked = isLocked,
                        onClick = { onModuleClick(module) }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = "REAL-TIME NEURAL TRAFFIC",
                    color = RoyalGold.copy(alpha = 0.82f),
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Live operator feed for prompts, responses, and fallback telemetry generated by the current AI engine.",
                    color = AiHubBody,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AiHubCard,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (hasCloudAi) "Cloud intelligence linked" else "Fallback intelligence active",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isAnalyzing) {
                                        "Streaming telemetry for $activeModule."
                                    } else {
                                        "Console remains on standby until a module starts a task."
                                    },
                                    color = AiHubBody,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            AiStatusBadge(
                                text = if (isAnalyzing) "Live" else "Standby",
                                accent = if (isAnalyzing) CyberCyan else RoyalGold,
                                filled = isAnalyzing
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        CyberConsole(promptFlow = aiManager.prompts)
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ShieldStatusBackground(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(1.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(128.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            RoyalGold.copy(alpha = 0.24f),
                                            CyberCyan.copy(alpha = 0.10f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .background(Color(0xFF0F141B), CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.08f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = RoyalGold,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "INTELLIGENCE CORE ONLINE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Royal Shield AI remains ready for rapid analysis and guided response.",
                                color = AiHubBody,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Educational and demo logic remain compatible with future backend integrations.",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            if (isAnalyzing) {
                LoadingOverlay(
                    moduleName = activeModule,
                    hasCloudAi = hasCloudAi
                )
            }

            resultText?.let { result ->
                AnalysisResultDialog(
                    moduleTitle = activeModule,
                    result = result,
                    onDismiss = { resultText = null }
                )
            }

            if (showUpgradeDialog) {
                AlertDialog(
                    onDismissRequest = { showUpgradeDialog = false },
                    containerColor = AiHubCard,
                    title = {
                        Text(
                            text = "Premium Required",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "This elite AI module is available on the Gold or Ultimate plan.",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Threat Radar, Log Analyzer, and Skill Optimizer stay visible in the hub and unlock instantly once premium access is active.",
                                color = AiHubBody,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showUpgradeDialog = false
                                onNavigateToPremium()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)
                        ) {
                            Text("VIEW PLANS", color = Color.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUpgradeDialog = false }) {
                            Text("Not now", color = AiHubBody)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AiHubHeroCard(
    hasPremium: Boolean,
    hasCloudAi: Boolean,
    isAnalyzing: Boolean,
    activeModule: String,
    totalModules: Int,
    lockedModuleCount: Int
) {
    val liveAccent = if (isAnalyzing) CyberCyan else SafeGreen

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AiHubCard,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.18f)),
        shadowElevation = 10.dp
    ) {
        Box {
            ShieldStatusBackground(
                modifier = Modifier
                    .matchParentSize()
                    .padding(1.dp)
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                RoyalGold.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        RoyalGold.copy(alpha = 0.25f),
                                        CyberCyan.copy(alpha = 0.15f)
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = RoyalGold,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAnalyzing) "Neural analysis in progress" else "Neural engine active",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hasCloudAi) {
                                "Gemini-backed intelligence is linked for cloud analysis when available."
                            } else {
                                "Secure backend fallback and local simulation are ready for resilient AI coverage."
                            },
                            color = AiHubBody,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AiHubMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Modules",
                        value = totalModules.toString(),
                        accent = RoyalGold
                    )
                    AiHubMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Engine",
                        value = if (hasCloudAi) "Gemini" else "Fallback",
                        accent = CyberCyan
                    )
                    AiHubMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Access",
                        value = if (hasPremium) "Elite" else "$lockedModuleCount locked",
                        accent = if (hasPremium) RoyalGold else Color(0xFFFFB74D)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAnalyzing && activeModule.isNotBlank()) {
                            "Current task: $activeModule"
                        } else {
                            "Ready for secure module execution"
                        },
                        color = Color.White.copy(alpha = 0.86f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    AiStatusBadge(
                        text = if (isAnalyzing) "Live Analysis" else if (hasPremium) "Elite Access" else "Free Mode",
                        accent = if (isAnalyzing) liveAccent else if (hasPremium) RoyalGold else Color.White,
                        filled = isAnalyzing || hasPremium
                    )
                }
            }
        }
    }
}

@Composable
private fun AiHubMetricCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 76.dp),
        color = AiHubCardSoft.copy(alpha = 0.92f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label.uppercase(),
                color = Color.White.copy(alpha = 0.54f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                maxLines = 1
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun PremiumNoticeCard(
    lockedModuleCount: Int,
    onNavigateToPremium: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF15110A),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.20f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(RoyalGold.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = RoyalGold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Unlock $lockedModuleCount elite AI modules",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Upgrade to activate advanced threat analysis, log intelligence, and guided skill optimization.",
                    color = AiHubBody,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            TextButton(onClick = onNavigateToPremium) {
                Text("VIEW", color = RoyalGold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LoadingOverlay(
    moduleName: String,
    hasCloudAi: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            color = AiHubCard,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.24f))
        ) {
            Box {
                ShieldStatusBackground(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(1.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        RoyalGold.copy(alpha = 0.22f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = RoyalGold)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Neural Analysis in Progress",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = moduleName.ifBlank { "Current module" },
                        color = RoyalGold,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (hasCloudAi) {
                            "Correlating cloud intelligence with Royal Shield telemetry and simulated safeguards."
                        } else {
                            "Using secure backend fallback and local simulation to keep the intelligence flow available."
                        },
                        color = AiHubBody,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisResultDialog(
    moduleTitle: String,
    result: String,
    onDismiss: () -> Unit
) {
    val resultStyle = remember(result) { resolveAiResultStyle(result) }
    val resultScrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AiHubCard,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = moduleTitle.ifBlank { "AI Module" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                AiStatusBadge(
                    text = resultStyle.label,
                    accent = resultStyle.accent,
                    filled = true
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Latest intelligence summary from the selected Royal Shield AI module.",
                    color = AiHubBody,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.03f),
                    border = BorderStroke(1.dp, resultStyle.accent.copy(alpha = 0.18f))
                ) {
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .verticalScroll(resultScrollState)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = result,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)
            ) {
                Text("ACKNOWLEDGE", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AiHubBody)
            }
        }
    )
}

private fun resolveAiResultStyle(result: String): AiHubResultStyle {
    val normalized = result.lowercase()
    return when {
        listOf("error", "failed", "blocked", "parse", "unavailable", "no response").any {
            normalized.contains(it)
        } -> AiHubResultStyle(
            label = "Attention Required",
            accent = AiHubDanger
        )

        listOf("secure", "verified", "active", "clean", "safe").any {
            normalized.contains(it)
        } -> AiHubResultStyle(
            label = "Secure Result",
            accent = SafeGreen
        )

        else -> AiHubResultStyle(
            label = "Analysis Complete",
            accent = CyberCyan
        )
    }
}

@Composable
private fun AiStatusBadge(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = if (filled) accent.copy(alpha = 0.14f) else Color.Transparent,
        border = BorderStroke(1.dp, accent.copy(alpha = if (filled) 0.36f else 0.22f))
    ) {
        Text(
            text = text.uppercase(),
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun AiFeatureItem(
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    badge: String,
    status: String,
    actionLabel: String,
    isPremium: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val accent = if (isLocked) RoyalGold else color
    val borderColor = when {
        isLocked -> RoyalGold.copy(alpha = 0.24f)
        isPremium -> RoyalGold.copy(alpha = 0.14f)
        else -> color.copy(alpha = 0.22f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        color = AiHubCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.background(
                Brush.horizontalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                )
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(accent.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            color = AiHubBody,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    AiStatusBadge(
                        text = badge,
                        accent = accent,
                        filled = isLocked
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AiStatusBadge(
                        text = status,
                        accent = accent
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = actionLabel.uppercase(),
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
