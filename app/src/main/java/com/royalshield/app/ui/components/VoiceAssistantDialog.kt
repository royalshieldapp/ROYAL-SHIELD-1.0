package com.royalshield.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.royalshield.app.ui.theme.RoyalGold
import kotlin.math.PI
import kotlin.math.sin

/**
 * Voice Assistant Dialog
 * Interactive AI voice assistant with waveform animation
 * Similar to Maia AI assistant
 */
@Composable
fun VoiceAssistantDialog(
    onDismiss: () -> Unit,
    onVoiceCommand: (String) -> Unit
) {
    var isListening by remember { mutableStateOf(false) }
    var assistantMessage by remember { mutableStateOf("Tap to Start") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1a0033),
                            Color(0xFF2d1b4e),
                            Color(0xFF1a0033)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Status message
                Text(
                    text = if (isListening) "AntiGravity is listening..." else assistantMessage,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Animated Waveform Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(280.dp)
                ) {
                    // Outer glow ring
                    AnimatedWaveformRing(isActive = isListening)
                    
                    // Main circle button
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF6B46C1),
                                        Color(0xFF553C9A)
                                    )
                                )
                            )
                            .clickable {
                                isListening = !isListening
                                if (isListening) {
                                    // Start voice recognition
                                    assistantMessage = "Listening..."
                                } else {
                                    // Stop and process
                                    assistantMessage = "Tap to Start"
                                    onVoiceCommand("Sample voice command")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Microphone",
                                tint = if (isListening) RoyalGold else Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            
                            if (!isListening) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Tap to Start",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Suggested questions
                if (!isListening) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "You can ask:",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        SuggestedQuestion("Analyze my device security")
                        Spacer(modifier = Modifier.height(8.dp))
                        SuggestedQuestion("Check for threats")
                        Spacer(modifier = Modifier.height(8.dp))
                        SuggestedQuestion("Review system logs")
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedWaveformRing(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2
        
        if (isActive) {
            // Draw animated waveform ring
            val path = Path()
            val segments = 100
            val waveAmplitude = 20f
            
            for (i in 0..segments) {
                val angle = (i.toFloat() / segments) * 2 * PI.toFloat()
                val wave = sin(angle * 8 + phase) * waveAmplitude
                val r = radius + wave * scale
                val x = center.x + r * kotlin.math.cos(angle.toDouble()).toFloat()
                val y = center.y + r * kotlin.math.sin(angle.toDouble()).toFloat()
                
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()
            
            // Draw glowing path
            drawPath(
                path = path,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF9333EA),
                        Color(0xFFEC4899),
                        Color(0xFF9333EA)
                    )
                ),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        } else {
            // Static ring when not active
            drawCircle(
                color = Color(0xFF6B46C1).copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun SuggestedQuestion(text: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Color.White.copy(alpha = 0.1f),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}
