package com.royalshield.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.R
import com.royalshield.app.ui.components.ROYAL_SECURITY_ORB_FRAMES
import com.royalshield.app.ui.components.RegistrationOrbResolvedMotion
import com.royalshield.app.ui.components.registrationOrbMotionProfile
import com.royalshield.app.ui.components.resolveRegistrationOrbMotion
import com.royalshield.app.ui.theme.Orbitron
import com.royalshield.app.ui.theme.RoyalGold
import kotlinx.coroutines.launch

@Composable
fun RegistrationOrbHero(
    reducedMotion: Boolean,
    onLockedInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = remember { registrationOrbMotionProfile() }
    if (reducedMotion) {
        RegistrationOrbHeroContent(
            motion = resolveRegistrationOrbMotion(reducedMotion = true, profile = profile),
            sweepPhase = 0f,
            ringRotation = 0f,
            pressScale = 1f,
            haloPulse = 0f,
            onLockedInfo = onLockedInfo,
            modifier = modifier,
        )
    } else {
        val transition = rememberInfiniteTransition(label = "registration-orb-motion")
        val floatOffsetY by transition.animateFloat(
            initialValue = profile.minFloatOffsetYDp,
            targetValue = profile.maxFloatOffsetYDp,
            animationSpec = infiniteRepeatable(
                animation = tween(profile.floatAndBreathDurationMs.toInt()),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "registration-orb-float",
        )
        val breathScale by transition.animateFloat(
            initialValue = profile.minBreathScale,
            targetValue = profile.maxBreathScale,
            animationSpec = infiniteRepeatable(
                animation = tween(profile.floatAndBreathDurationMs.toInt()),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "registration-orb-breath",
        )
        val haloAlpha by transition.animateFloat(
            initialValue = profile.minHaloAlpha,
            targetValue = profile.maxHaloAlpha,
            animationSpec = infiniteRepeatable(
                animation = tween(profile.haloDurationMs.toInt()),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "registration-orb-halo",
        )
        val sweepPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(profile.sweepDurationMs.toInt(), easing = LinearEasing),
            ),
            label = "registration-orb-sweep",
        )
        val ringRotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(profile.ringDurationMs.toInt(), easing = LinearEasing),
            ),
            label = "registration-orb-ring",
        )
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (isPressed) profile.pressScale else 1f,
            animationSpec = tween(
                durationMillis = if (isPressed) {
                    profile.pressDurationMs.toInt()
                } else {
                    profile.releaseDurationMs.toInt()
                },
            ),
            label = "registration-orb-press",
        )
        val haloPulse = remember { Animatable(0f) }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Press) {
                    launch {
                        haloPulse.snapTo(0f)
                        haloPulse.animateTo(1f, tween(profile.pressDurationMs.toInt()))
                        haloPulse.animateTo(0f, tween(profile.releaseDurationMs.toInt()))
                    }
                }
            }
        }

        RegistrationOrbHeroContent(
            motion = resolveRegistrationOrbMotion(
                reducedMotion = false,
                profile = profile,
            ).copy(
                offsetYDp = floatOffsetY,
                scale = breathScale,
                haloAlpha = haloAlpha,
            ),
            sweepPhase = sweepPhase,
            ringRotation = ringRotation,
            pressScale = pressScale,
            haloPulse = haloPulse.value,
            interactionSource = interactionSource,
            onLockedInfo = onLockedInfo,
            modifier = modifier,
        )
    }
}

@Composable
private fun RegistrationOrbHeroContent(
    motion: RegistrationOrbResolvedMotion,
    sweepPhase: Float,
    ringRotation: Float,
    pressScale: Float,
    haloPulse: Float,
    onLockedInfo: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val layout = registrationOrbHeroLayout()
    val shape = RoundedCornerShape(layout.cornerRadiusDp.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.30f), shape)
            .border(1.5.dp, RoyalGold.copy(alpha = 0.72f), shape)
            .padding(horizontal = layout.horizontalPaddingDp.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(layout.haloSizeDp.dp)
                .semantics {
                    contentDescription = "Security agent locked. Tap for information."
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onLockedInfo,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(layout.haloSizeDp.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                RoyalGold.copy(alpha = motion.haloAlpha + haloPulse * 0.10f),
                                Color(0xFFFFA000).copy(alpha = motion.haloAlpha * 0.45f),
                                Color.Transparent,
                            ),
                        ),
                        CircleShape,
                    ),
            )
            Canvas(modifier = Modifier.size(layout.haloSizeDp.dp)) {
                rotate(ringRotation) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                RoyalGold.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.58f),
                                Color.Transparent,
                            ),
                        ),
                        startAngle = 0f,
                        sweepAngle = 285f,
                        useCenter = false,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                drawArc(
                    color = Color.White.copy(alpha = 0.55f + haloPulse * 0.20f),
                    startAngle = (sweepPhase * 360f) - 40f,
                    sweepAngle = 32f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Image(
                painter = painterResource(
                    ROYAL_SECURITY_ORB_FRAMES[
                        (sweepPhase * ROYAL_SECURITY_ORB_FRAMES.size)
                            .toInt()
                            .coerceIn(0, ROYAL_SECURITY_ORB_FRAMES.lastIndex)
                    ],
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(layout.orbSizeDp.dp)
                    .graphicsLayer {
                        translationY = motion.offsetYDp.dp.toPx()
                        scaleX = motion.scale * pressScale
                        scaleY = motion.scale * pressScale
                    }
                    .alpha(0.88f),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Your security agent is waiting.",
            color = Color(0xFFFFD66B),
            fontSize = 23.sp,
            lineHeight = 30.sp,
            fontFamily = Orbitron,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Sign up to configure available security, privacy, and family protection features.",
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
            color = RoyalGold.copy(alpha = 0.12f),
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGold.copy(alpha = 0.62f)),
            modifier = Modifier.semantics {
                contentDescription = "Security agent status: locked"
            },
        ) {
            Text(
                text = "SECURITY AGENT • LOCKED",
                color = RoyalGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}
