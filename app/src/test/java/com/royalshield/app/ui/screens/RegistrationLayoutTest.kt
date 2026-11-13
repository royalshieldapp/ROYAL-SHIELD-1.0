package com.royalshield.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class RegistrationLayoutTest {

    @Test
    fun socialButtonsUseApprovedRegistrationGeometry() {
        val layout = registrationSocialButtonLayout()

        assertEquals(0.92f, layout.widthFraction, 0.0001f)
        assertTrue(layout.heightDp in 72f..76f)
        assertEquals(52f, layout.iconSizeDp, 0.0001f)
        assertEquals(64f, layout.leadingSlotWidthDp, 0.0001f)
        assertEquals(layout.leadingSlotWidthDp, layout.trailingSlotWidthDp, 0.0001f)
        assertEquals(16f, layout.horizontalPaddingDp, 0.0001f)
        assertEquals(8f, layout.iconLabelGapDp, 0.0001f)
        assertEquals(14f, layout.labelFontSizeSp, 0.0001f)
    }

    @Test
    fun registrationOrbHeroUsesApprovedGeometry() {
        val layout = registrationOrbHeroLayout()

        assertEquals(196f, layout.orbSizeDp, 0.0001f)
        assertEquals(220f, layout.haloSizeDp, 0.0001f)
        assertEquals(24f, layout.cornerRadiusDp, 0.0001f)
        assertEquals(20f, layout.horizontalPaddingDp, 0.0001f)
    }

    @Test
    fun backgroundMovesSubtlyWhileControlsStayCentered() {
        val motion = registrationParallax(tiltX = 1f, tiltY = -1f)

        assertEquals(8f, motion.backgroundXdp, 0.0001f)
        assertEquals(-8f, motion.backgroundYdp, 0.0001f)
        assertEquals(0f, motion.controlsXdp, 0.0001f)
        assertEquals(0f, motion.controlsYdp, 0.0001f)
    }

    @Test
    fun socialButtonOverlaysSlotsWithoutShrinkingTheCenteredLabelLayer() {
        val layout = registrationSocialButtonLayout()
        val compactScreenWidthDp = 360f
        val screenHorizontalPaddingDp = 48f
        val panelBorderPaddingDp = 10f
        val panelContentPaddingDp = 40f
        val buttonWidthDp = (
            compactScreenWidthDp -
                screenHorizontalPaddingDp -
                panelBorderPaddingDp -
                panelContentPaddingDp
            ) * layout.widthFraction
        val sequentialLabelWidthDp = buttonWidthDp -
            (layout.horizontalPaddingDp * 2f) -
            layout.leadingSlotWidthDp -
            layout.trailingSlotWidthDp -
            (layout.iconLabelGapDp * 2f)

        assertTrue("Sequential slots reproduce the compact-screen defect", sequentialLabelWidthDp < 80f)
        assertTrue("The layered label retains the full button width", buttonWidthDp > 200f)

        val sourcePath = sequenceOf(
            Path.of("src", "main", "java", "com", "royalshield", "app", "ui", "screens", "RegistrationScreen.kt"),
            Path.of("app", "src", "main", "java", "com", "royalshield", "app", "ui", "screens", "RegistrationScreen.kt"),
        ).first { Files.isRegularFile(it) }
        val source = String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8)
        val buttonSource = source.substringAfter("fun SocialLoginButton(")

        assertFalse("Side slots must not consume label width in a sequential Row", buttonSource.contains("Row("))
        assertTrue(
            "The label must occupy the complete centered button layer",
            Regex(
                """Text\(\s*text = text,\s*color = textColor,\s*modifier = Modifier\.fillMaxSize\(\)""",
            ).containsMatchIn(buttonSource),
        )
        assertTrue("The leading icon slot must be overlaid", buttonSource.contains("contentAlignment = Alignment.CenterStart"))
        assertTrue("An equal trailing safety slot must be overlaid", buttonSource.contains("contentAlignment = Alignment.CenterEnd"))
    }
}
