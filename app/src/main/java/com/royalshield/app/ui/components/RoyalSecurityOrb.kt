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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.royalshield.app.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

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

data class OrbFrameBlend(
    val frameIndex: Int,
    val nextFrameIndex: Int,
    val blendFraction: Float,
)

fun resolveOrbFrameBlend(
    phase: Float,
    frameCount: Int = ROYAL_SECURITY_ORB_FRAMES.size,
): OrbFrameBlend {
    require(frameCount > 0) { "frameCount must be positive" }
    val normalized = phase.coerceIn(0f, 0.999_999f)
    val framePosition = normalized * frameCount
    val frameIndex = floor(framePosition).toInt().coerceIn(0, frameCount - 1)
    return OrbFrameBlend(
        frameIndex = frameIndex,
        nextFrameIndex = (frameIndex + 1) % frameCount,
        blendFraction = (framePosition - floor(framePosition)).coerceIn(0f, 1f),
    )
}

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
        RoyalSecurityOrbState.LOCKED -> (0.985f + (0.034f * energy)).coerceAtMost(1.019f)
        RoyalSecurityOrbState.SETUP,
        RoyalSecurityOrbState.ACTIVE -> (0.97f + (0.089f * energy)).coerceAtMost(1.059f)
        RoyalSecurityOrbState.LISTENING -> (0.96f + (0.139f * energy)).coerceAtMost(1.099f)
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
        offsetYDp =
            (-2.6f * sin(2f * PI.toFloat() * normalized)) -
                (0.8f * sin((4f * PI.toFloat() * normalized) + 0.65f)),
        haloAlpha = haloBase + (energy * if (state == RoyalSecurityOrbState.LISTENING) 0.18f else 0.10f),
        innerRingRotation = normalized * 360f,
        outerRingRotation = normalized * -280f,
        cycleDurationMs = cycleDurationMs,
    )
}

fun resolveOrbFluidRadius(
    state: RoyalSecurityOrbState,
    angleRadians: Float,
    phase: Float,
): Float {
    val normalized = phase - floor(phase)
    val cycle = 2f * PI.toFloat() * normalized
    val deformation =
        (0.52f * sin((3f * angleRadians) + cycle)) +
            (0.30f * sin((5f * angleRadians) - (2f * cycle) + 0.8f)) +
            (0.18f * sin((8f * angleRadians) + (3f * cycle) + 1.4f))
    val amplitude = when (state) {
        RoyalSecurityOrbState.LOCKED -> 0.034f
        RoyalSecurityOrbState.SETUP -> 0.052f
        RoyalSecurityOrbState.ACTIVE -> 0.064f
        RoyalSecurityOrbState.LISTENING -> 0.096f
    }
    return (0.76f + (amplitude * deformation)).coerceIn(0.63f, 0.89f)
}

private fun DrawScope.fluidOrbPath(
    state: RoyalSecurityOrbState,
    phase: Float,
    radiusMultiplier: Float,
): Path {
    val radius = size.minDimension * 0.5f
    val points = List(96) { index ->
        val angle = (2f * PI.toFloat() * index) / 96f
        val fluidRadius = radius * resolveOrbFluidRadius(state, angle, phase) * radiusMultiplier
        androidx.compose.ui.geometry.Offset(
            x = center.x + (cos(angle) * fluidRadius),
            y = center.y + (sin(angle) * fluidRadius),
        )
    }
    fun midpoint(a: androidx.compose.ui.geometry.Offset, b: androidx.compose.ui.geometry.Offset) =
        androidx.compose.ui.geometry.Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)

    return Path().apply {
        val first = midpoint(points.last(), points.first())
        moveTo(first.x, first.y)
        points.forEachIndexed { index, point ->
            val next = points[(index + 1) % points.size]
            val mid = midpoint(point, next)
            quadraticTo(point.x, point.y, mid.x, mid.y)
        }
        close()
    }
}

@Composable
fun ProceduralSecurityOrbCanvas(
    state: RoyalSecurityOrbState,
    phase: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.5f
        val energy = (sin(2f * PI.toFloat() * phase) + 1f) / 2f
        val outerPath = fluidOrbPath(state, phase, radiusMultiplier = 1f)
        val innerPath = fluidOrbPath(state, phase + 0.19f, radiusMultiplier = 0.76f)
        val stateEnergy = when (state) {
            RoyalSecurityOrbState.LOCKED -> 0.84f
            RoyalSecurityOrbState.SETUP -> 0.90f
            RoyalSecurityOrbState.ACTIVE -> 0.95f
            RoyalSecurityOrbState.LISTENING -> 1f
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f * stateEnergy),
                    Color(0xFFFFD54F).copy(alpha = (0.34f + energy * 0.12f) * stateEnergy),
                    Color(0xFFFF8F00).copy(alpha = 0.18f * stateEnergy),
                    Color.Transparent,
                ),
                center = center,
                radius = radius * 0.93f,
            ),
            radius = radius * 0.93f,
        )
        drawPath(
            path = outerPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF7D6).copy(alpha = 0.42f * stateEnergy),
                    Color(0xFFFFD54F).copy(alpha = 0.32f * stateEnergy),
                    Color(0xFFB86500).copy(alpha = 0.30f * stateEnergy),
                    Color(0xFF130B00).copy(alpha = 0.68f * stateEnergy),
                ),
                center = center,
                radius = radius * 0.82f,
            ),
        )
        drawPath(
            path = outerPath,
            color = Color(0xFFFFB300).copy(alpha = (0.22f + energy * 0.16f) * stateEnergy),
            style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            path = outerPath,
            color = Color(0xFFFFE082).copy(alpha = (0.72f + energy * 0.20f) * stateEnergy),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            path = innerPath,
            color = Color.White.copy(alpha = (0.12f + energy * 0.11f) * stateEnergy),
            style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round),
        )

        val goldenAngle = 2.3999632f
        repeat(88) { index ->
            val distribution = sqrt((index + 0.5f) / 88f)
            val angle = (index * goldenAngle) + (phase * 2f * PI.toFloat() * (if (index % 2 == 0) 1f else -0.7f))
            val wobble = 1f + (0.06f * sin((phase * 2f * PI.toFloat()) + (index * 0.83f)))
            val particleRadius = radius * 0.61f * distribution * wobble
            val particleCenter = androidx.compose.ui.geometry.Offset(
                x = center.x + cos(angle) * particleRadius,
                y = center.y + sin(angle) * particleRadius,
            )
            val shimmer = (sin((phase * 4f * PI.toFloat()) + index) + 1f) / 2f
            drawCircle(
                color = if (index % 7 == 0) Color.White else Color(0xFFFFD54F),
                radius = (0.65f + (index % 4) * 0.28f).dp.toPx(),
                center = particleCenter,
                alpha = (0.30f + shimmer * 0.62f) * stateEnergy,
            )
        }

        repeat(14) { index ->
            val angle = (index * goldenAngle) - (phase * 2f * PI.toFloat())
            val innerRadius = radius * (0.20f + (index % 3) * 0.055f)
            val outerRadius = radius * (0.47f + (index % 4) * 0.045f)
            drawLine(
                color = Color(0xFFFFD54F).copy(alpha = (0.10f + energy * 0.12f) * stateEnergy),
                start = androidx.compose.ui.geometry.Offset(
                    center.x + cos(angle + 0.18f) * innerRadius,
                    center.y + sin(angle + 0.18f) * innerRadius,
                ),
                end = androidx.compose.ui.geometry.Offset(
                    center.x + cos(angle) * outerRadius,
                    center.y + sin(angle) * outerRadius,
                ),
                strokeWidth = 0.75.dp.toPx(),
            )
        }
    }
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
    val interactionSource = remember { MutableInteractionSource() }
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
    val frameBlend = resolveOrbFrameBlend(if (reducedMotion) 0f else plasmaPhase)
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
            .semantics { contentDescription = description }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
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
        val orbCoreModifier = Modifier
            .fillMaxSize(0.86f)
            .graphicsLayer {
                translationY = plasma.offsetYDp * density
                scaleX = scale.value * plasma.scale
                scaleY = scale.value * plasma.scale
            }
        Image(
            painter = painterResource(ROYAL_SECURITY_ORB_FRAMES[frameBlend.frameIndex]),
            contentDescription = null,
            modifier = orbCoreModifier.alpha(0.94f * (1f - frameBlend.blendFraction)),
        )
        if (frameBlend.blendFraction > 0f) {
            Image(
                painter = painterResource(ROYAL_SECURITY_ORB_FRAMES[frameBlend.nextFrameIndex]),
                contentDescription = null,
                modifier = orbCoreModifier.alpha(0.94f * frameBlend.blendFraction),
            )
        }
        ProceduralSecurityOrbCanvas(
            state = state,
            phase = if (reducedMotion) 0f else plasmaPhase,
            modifier = orbCoreModifier.alpha(0.34f),
        )
    }
}
