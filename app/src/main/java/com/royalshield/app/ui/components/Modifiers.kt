package com.royalshield.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

fun Modifier.tilt3D(
    defaultRotationX: Float = 10f,
    defaultRotationY: Float = -16f,
    maxRotationX: Float = 18f,
    maxRotationY: Float = 22f
): Modifier = composed {
    var rotateX by remember { mutableStateOf(defaultRotationX) }
    var rotateY by remember { mutableStateOf(defaultRotationY) }
    var isPressed by remember { mutableStateOf(false) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }

    val animatedRotateX by animateFloatAsState(
        targetValue = rotateX,
        animationSpec = tween(
            durationMillis = if (isPressed) 0 else 350,
            easing = LinearOutSlowInEasing
        ),
        label = "dashboardCardRotationX"
    )

    val animatedRotateY by animateFloatAsState(
        targetValue = rotateY,
        animationSpec = tween(
            durationMillis = if (isPressed) 0 else 350,
            easing = LinearOutSlowInEasing
        ),
        label = "dashboardCardRotationY"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.04f else 1f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "dashboardCardScale"
    )

    this
        .onGloballyPositioned { coordinates ->
            cardSize = coordinates.size
        }
        .graphicsLayer {
            rotationX = animatedRotateX
            rotationY = animatedRotateY
            scaleX = scale
            scaleY = scale
            cameraDistance = 18f * density
            shadowElevation = if (isPressed) 28f else 18f
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull()

                    if (change != null && change.pressed) {
                        isPressed = true

                        val midX = cardSize.width / 2f
                        val midY = cardSize.height / 2f

                        if (midX > 0f && midY > 0f) {
                            val x = change.position.x.coerceIn(0f, cardSize.width.toFloat())
                            val y = change.position.y.coerceIn(0f, cardSize.height.toFloat())

                            rotateY = ((x - midX) / midX) * maxRotationY
                            rotateX = -((y - midY) / midY) * maxRotationX
                        }
                    } else {
                        isPressed = false
                        rotateX = defaultRotationX
                        rotateY = defaultRotationY
                    }
                }
            }
        }
}
