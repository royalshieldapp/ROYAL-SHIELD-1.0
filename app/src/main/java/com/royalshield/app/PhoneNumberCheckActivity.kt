package com.royalshield.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.managers.ReputationManager
import com.royalshield.app.managers.ReputationResult
import com.royalshield.app.managers.ReputationStatus
import com.royalshield.app.ui.theme.NeonRed
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.Royal_shieldTheme
import com.royalshield.app.ui.theme.SafeGreen
import com.royalshield.app.ui.theme.CyberCyan
import kotlinx.coroutines.launch

class PhoneNumberCheckActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Royal_shieldTheme {
                PhoneIntelligenceScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneIntelligenceScreen(onBack: () -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ReputationResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "header")
    val headerGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "headerGlow"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Image(
            painter = painterResource(id = R.drawable.bg_cyber_earth),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp)
        ) {
            // ── TOP BAR ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "PHONE INTELLIGENCE",
                        color = RoyalGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Analyze & Verify Any Number",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Pulsing shield icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(RoyalGold.copy(alpha = headerGlow * 0.15f), CircleShape)
                        .border(1.dp, RoyalGold.copy(alpha = headerGlow), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhoneAndroid, null, tint = RoyalGold, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── INPUT CARD ──────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D14)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(RoyalGold.copy(alpha = 0.5f), CyberCyan.copy(alpha = 0.3f), RoyalGold.copy(alpha = 0.2f))
                    )
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = RoyalGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify Caller Identity", color = RoyalGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Enter a phone number to check if it's spam, scam, or safe. Connected to Royal Shield threat intelligence.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number", color = Color.Gray) },
                        placeholder = { Text("+1 555 123 4567", color = Color.Gray.copy(alpha = 0.4f)) },
                        leadingIcon = { Icon(Icons.Default.Call, null, tint = RoyalGold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalGold,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = RoyalGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (phoneNumber.isNotBlank()) {
                                isLoading = true
                                result = null
                                showResult = false
                                scope.launch {
                                    result = ReputationManager.checkPhoneNumber(phoneNumber)
                                    isLoading = false
                                    showResult = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        enabled = !isLoading && phoneNumber.isNotBlank()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(RoyalGold, Color(0xFFFFA000), RoyalGold)
                                    ),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("ANALYZING...", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Search, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ANALYZE NUMBER", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── RESULTS ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = showResult && result != null,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 }
            ) {
                result?.let { res ->
                    PhoneIntelResultSection(res)
                }
            }
        }
    }
}

@Composable
private fun PhoneIntelResultSection(result: ReputationResult) {
    val statusColor = when (result.status) {
        ReputationStatus.SAFE -> SafeGreen
        ReputationStatus.CAUTION -> RoyalGold
        ReputationStatus.SPAM -> Color(0xFFFF9800)
        ReputationStatus.MALICIOUS -> NeonRed
    }
    val statusLabel = when (result.status) {
        ReputationStatus.SAFE -> "SAFE"
        ReputationStatus.CAUTION -> "CAUTION"
        ReputationStatus.SPAM -> "SPAM"
        ReputationStatus.MALICIOUS -> "MALICIOUS"
    }
    val riskLevelColor = when (result.riskLevel.uppercase()) {
        "LOW" -> SafeGreen
        "MEDIUM" -> RoyalGold
        "HIGH" -> Color(0xFFFF9800)
        "CRITICAL" -> NeonRed
        else -> Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── SCORE + STATUS CARD ─────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D14)),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Score Circle
                PhoneScoreCircle(score = result.score, color = statusColor)

                Spacer(modifier = Modifier.height(16.dp))

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        statusLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Number display
                Text(
                    result.internationalFormat.ifEmpty { result.number },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                // Source indicator
                Text(
                    "Source: ${result.source.uppercase()}",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── DETAILS GRID ────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D14)),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "NUMBER DETAILS",
                    color = RoyalGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Grid 2x3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailCell(
                        icon = Icons.Default.Business,
                        label = "CARRIER",
                        value = result.carrier,
                        modifier = Modifier.weight(1f)
                    )
                    DetailCell(
                        icon = Icons.Default.Public,
                        label = "COUNTRY",
                        value = result.country,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailCell(
                        icon = Icons.Default.PhoneAndroid,
                        label = "LINE TYPE",
                        value = result.lineType.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.weight(1f)
                    )
                    DetailCell(
                        icon = Icons.Default.LocationOn,
                        label = "LOCATION",
                        value = result.location.ifEmpty { "Unknown" },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailCell(
                        icon = Icons.Default.Flag,
                        label = "COUNTRY CODE",
                        value = "${result.countryPrefix} (${result.countryCode})",
                        modifier = Modifier.weight(1f)
                    )
                    DetailCell(
                        icon = Icons.Default.Tag,
                        label = "FORMAT",
                        value = result.localFormat.ifEmpty { "N/A" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── RISK ANALYSIS CARD ──────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D14)),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, riskLevelColor.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "RISK ANALYSIS",
                    color = RoyalGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Risk Level Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Risk Level", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = riskLevelColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, riskLevelColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            result.riskLevel.uppercase(),
                            color = riskLevelColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Report count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("User Reports", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "${result.reportCount}",
                        color = if (result.reportCount > 0) NeonRed else SafeGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Last reported
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Last Reported", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        result.lastReported?.take(10) ?: "Never",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tags
                Text("INTELLIGENCE TAGS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                // FlowRow alternative: wrap manually
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    result.tags.take(4).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusColor.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                statusColor.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                tag,
                                color = statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── ACTION BUTTONS ──────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val context = LocalContext.current
            // Block Number
            PhoneActionButton(
                icon = Icons.Default.Block,
                label = "Block",
                color = NeonRed,
                modifier = Modifier.weight(1f),
                onClick = { Toast.makeText(context, "Number blocked", Toast.LENGTH_SHORT).show() }
            )
            // Report Spam
            PhoneActionButton(
                icon = Icons.Default.Report,
                label = "Report",
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f),
                onClick = { Toast.makeText(context, "Reported as spam", Toast.LENGTH_SHORT).show() }
            )
            // Save
            PhoneActionButton(
                icon = Icons.Default.PersonAdd,
                label = "Save",
                color = SafeGreen,
                modifier = Modifier.weight(1f),
                onClick = { Toast.makeText(context, "Saved to contacts", Toast.LENGTH_SHORT).show() }
            )
        }

        // Validity check
        if (!result.valid) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NeonRed.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = NeonRed, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("INVALID NUMBER", color = NeonRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "This number could not be validated. It may be spoofed or non-existent.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// ── SCORE CIRCLE ────────────────────────────────────────────────
@Composable
private fun PhoneScoreCircle(score: Int, color: Color) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "score"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "scoreGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scoreGlowAlpha"
    )
    val rotAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotAngle"
    )

    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
        // Outer glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = glowAlpha * 0.5f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension / 2f
                )
            )
        }

        // Background track
        Canvas(modifier = Modifier.size(120.dp)) {
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Score arc
        Canvas(modifier = Modifier.size(120.dp)) {
            val sweep = (animatedScore / 100f) * 360f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(color.copy(alpha = 0.3f), color, color.copy(alpha = 0.6f))
                ),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Thin rotating decorative arc
        Canvas(modifier = Modifier.size(132.dp)) {
            rotate(rotAngle) {
                drawArc(
                    color = color.copy(alpha = 0.25f),
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Score text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$animatedScore",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 36.sp
            )
            Text(
                "TRUST SCORE",
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── DETAIL CELL ─────────────────────────────────────────────────
@Composable
private fun DetailCell(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = RoyalGold.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}

// ── ACTION BUTTON ───────────────────────────────────────────────
@Composable
private fun PhoneActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}
