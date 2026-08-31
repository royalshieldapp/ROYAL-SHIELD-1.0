package com.royalshield.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.royalshield.app.R
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

enum class RoyalSecurityOrbState {
    LOCKED,
    SETUP,
    ACTIVE,
    LISTENING,
}

val ROYAL_SECURITY_ORB_FRAMES = intArrayOf(
    R.drawable.royal_security_orb_1,
    R.drawable.royal_security_orb_2,
    R.drawable.royal_security_orb_3,
    R.drawable.royal_security_orb_4,
    R.drawable.royal_security_orb_5,
    R.drawable.royal_security_orb_6,
    R.drawable.royal_security_orb_7,
    R.drawable.royal_security_orb_8,
    R.drawable.royal_security_orb_9,
    R.drawable.royal_security_orb_10,
)

data class OrbPlasmaMotion(
    val frameIndex: Int,
    val scale: Float,
    val offsetYDp: Float,
    val haloAlpha: Float,
    val innerRingRotation: Float,
    val outerRingRotation: Float,
    val cycleDurationMs: Int,
)

fun resolveOrbPlasmaMotion(
    state: RoyalSecurityOrbState,
    phase: Float,
    reducedMotion: Boolean,
): OrbPlasmaMotion {
    val cycleDurationMs = when (state) {
        RoyalSecurityOrbState.LOCKED -> 4_800
        RoyalSecurityOrbState.SETUP -> 3_200
        RoyalSecurityOrbState.ACTIVE -> 3_600
        RoyalSecurityOrbState.LISTENING -> 1_700
    }
    if (reducedMotion) {
        return OrbPlasmaMotion(
            frameIndex = 0,
            scale = 1f,
            offsetYDp = 0f,
            haloAlpha = if (state == RoyalSecurityOrbState.LOCKED) 0.12f else 0.18f,
            innerRingRotation = 0f,
            outerRingRotation = 0f,
            cycleDurationMs = cycleDurationMs,
        )
    }

    val normalized = phase.coerceIn(0f, 0.999_999f)
    val organicPhase = (
        normalized + 0.025f * sin(4f * PI.toFloat() * normalized)
    ).coerceIn(0f, 0.999_999f)
    val energy = ((sin(2f * PI.toFloat() * normalized) + 1f) / 2f)
    val scale = when (state) {
        RoyalSecurityOrbState.LOCKED -> 0.985f + (0.035f * energy)
        RoyalSecurityOrbState.SETUP,
        RoyalSecurityOrbState.ACTIVE -> 0.97f + (0.09f * energy)
        RoyalSecurityOrbState.LISTENING -> 0.96f + (0.14f * energy)
    }
    val haloBase = when (state) {
        RoyalSecurityOrbState.LOCKED -> 0.12f
        RoyalSecurityOrbState.SETUP -> 0.22f
        RoyalSecurityOrbState.ACTIVE -> 0.18f
        RoyalSecurityOrbState.LISTENING -> 0.34f
    }

    return OrbPlasmaMotion(
        frameIndex = floor(organicPhase * ROYAL_SECURITY_ORB_FRAMES.size).toInt()
            .coerceIn(0, ROYAL_SECURITY_ORB_FRAMES.lastIndex),
        scale = scale,
        offsetYDp = -4f * sin(2f * PI.toFloat() * normalized),
        haloAlpha = haloBase + (energy * if (state == RoyalSecurityOrbState.LISTENING) 0.18f else 0.10f),
        innerRingRotation = normalized * 360f,
        outerRingRotation = normalized * -280f,
        cycleDurationMs = cycleDurationMs,
    )
}

data class RoyalSecurityOrbIdleMotion(
    val scale: Float,
    val offsetYDp: Float,
)

data class RegistrationOrbMotionProfile(
    val minFloatOffsetYDp: Float,
    val maxFloatOffsetYDp: Float,
    val minBreathScale: Float,
    val maxBreathScale: Float,
    val minHaloAlpha: Float,
    val maxHaloAlpha: Float,
    val floatAndBreathDurationMs: Long,
    val haloDurationMs: Long,
    val sweepDurationMs: Long,
    val ringDurationMs: Long,
    val pressScale: Float,
    val pressDurationMs: Long,
    val releaseDurationMs: Long,
)

fun registrationOrbMotionProfile() = RegistrationOrbMotionProfile(
    minFloatOffsetYDp = -3f,
    maxFloatOffsetYDp = 3f,
    minBreathScale = 0.985f,
    maxBreathScale = 1.015f,
    minHaloAlpha = 0.14f,
    maxHaloAlpha = 0.24f,
    floatAndBreathDurationMs = 3_200L,
    haloDurationMs = 2_800L,
    sweepDurationMs = 12_000L,
    ringDurationMs = 18_000L,
    pressScale = 0.97f,
    pressDurationMs = 90L,
    releaseDurationMs = 220L,
)

data class RegistrationOrbResolvedMotion(
    val offsetYDp: Float,
    val scale: Float,
    val haloAlpha: Float,
    val continuousMotionEnabled: Boolean,
)

fun resolveRegistrationOrbMotion(
    reducedMotion: Boolean,
    profile: RegistrationOrbMotionProfile = registrationOrbMotionProfile(),
) = RegistrationOrbResolvedMotion(
    offsetYDp = 0f,
    scale = 1f,
    haloAlpha = profile.minHaloAlpha,
    continuousMotionEnabled = !reducedMotion,
)

fun royalSecurityOrbIdleMotion(phase: Float): RoyalSecurityOrbIdleMotion {
    val pulse = sin(PI * phase.coerceIn(0f, 1f)).toFloat()
    return RoyalSecurityOrbIdleMotion(
        scale = 1f + (0.02f * pulse),
        offsetYDp = -3f * pulse,
    )
}

@Composable
fun RoyalSecurityOrb(
    state: RoyalSecurityOrbState,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    onClick: () -> Unit = {},
) {
    val plasmaTransition = rememberInfiniteTransition(label = "security-orb-plasma")
    val cycleDuration = resolveOrbPlasmaMotion(state, 0f, reducedMotion).cycleDurationMs
    val plasmaPhase by plasmaTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(cycleDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "security-orb-plasma-phase",
    )
    val plasma = resolveOrbPlasmaMotion(state, plasmaPhase, reducedMotion)
    val targetScale = when (state) {
        RoyalSecurityOrbState.LOCKED -> 0.78f
        RoyalSecurityOrbState.SETUP -> 1f
        RoyalSecurityOrbState.ACTIVE -> 1f
        RoyalSecurityOrbState.LISTENING -> 1f
    }
    val scale = animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "security-orb-scale",
    )
    val description = when (state) {
        RoyalSecurityOrbState.LOCKED -> "Agente de seguridad bloqueado"
        RoyalSecurityOrbState.SETUP -> "Agente configurando protección"
        RoyalSecurityOrbState.ACTIVE -> "Agente de seguridad por voz"
        RoyalSecurityOrbState.LISTENING -> "Agente escuchando"
    }

    Box(
        modifier = modifier
            .alpha(if (state == RoyalSecurityOrbState.LOCKED) 0.72f else 1f)
        .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0xFFFFD54F).copy(alpha = plasma.haloAlpha),
                        Color(0xFFFF8F00).copy(alpha = plasma.haloAlpha * 0.42f),
                        Color.Transparent,
                    ),
                ),
                radius = size.minDimension * 0.49f,
            )
            if (!reducedMotion) {
                rotate(plasma.innerRingRotation) {
                    drawArc(
                        color = Color(0xFFFFD54F).copy(alpha = plasma.haloAlpha + 0.10f),
                        startAngle = 8f,
                        sweepAngle = 244f,
                        useCenter = false,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                rotate(plasma.outerRingRotation) {
                    drawArc(
                        color = Color.White.copy(alpha = plasma.haloAlpha * 0.72f),
                        startAngle = 172f,
                        sweepAngle = 126f,
                        useCenter = false,
                        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
        }
        Image(
            painter = painterResource(ROYAL_SECURITY_ORB_FRAMES[plasma.frameIndex]),
            contentDescription = description,
            modifier = Modifier
                .fillMaxSize(0.86f)
                .graphicsLayer { translationY = plasma.offsetYDp * density }
                .scale(scale.value * plasma.scale),
        )
    }
}
