package com.royalshield.app.ui.screens

data class RegistrationSocialButtonLayout(
    val widthFraction: Float,
    val heightDp: Float,
    val iconSizeDp: Float,
    val leadingSlotWidthDp: Float,
    val trailingSlotWidthDp: Float,
    val horizontalPaddingDp: Float,
    val iconLabelGapDp: Float,
    val labelFontSizeSp: Float,
)

fun registrationSocialButtonLayout() = RegistrationSocialButtonLayout(
    widthFraction = 0.92f,
    heightDp = 74f,
    iconSizeDp = 52f,
    leadingSlotWidthDp = 64f,
    trailingSlotWidthDp = 64f,
    horizontalPaddingDp = 16f,
    iconLabelGapDp = 8f,
    labelFontSizeSp = 14f,
)

data class RegistrationOrbHeroLayout(
    val orbSizeDp: Float,
    val haloSizeDp: Float,
    val cornerRadiusDp: Float,
    val horizontalPaddingDp: Float,
)

fun registrationOrbHeroLayout() = RegistrationOrbHeroLayout(
    orbSizeDp = 196f,
    haloSizeDp = 220f,
    cornerRadiusDp = 24f,
    horizontalPaddingDp = 20f,
)

data class RegistrationParallax(
    val backgroundXdp: Float,
    val backgroundYdp: Float,
    val controlsXdp: Float,
    val controlsYdp: Float,
)

fun registrationParallax(tiltX: Float, tiltY: Float) = RegistrationParallax(
    backgroundXdp = tiltX.coerceIn(-1f, 1f) * 8f,
    backgroundYdp = tiltY.coerceIn(-1f, 1f) * 8f,
    controlsXdp = 0f,
    controlsYdp = 0f,
)
