package com.royalshield.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test
import com.royalshield.app.R

class RoyalSecurityOrbMotionTest {

    @Test
    fun lockedOrbHasSubtleIdleMotionWithoutChangingItsStateScale() {
        val start = royalSecurityOrbIdleMotion(phase = 0f)
        val middle = royalSecurityOrbIdleMotion(phase = 0.5f)

        assertEquals(1f, start.scale, 0.0001f)
        assertEquals(0f, start.offsetYDp, 0.0001f)
        assertNotEquals(start, middle)
        assertEquals(1.02f, middle.scale, 0.0001f)
        assertEquals(-3f, middle.offsetYDp, 0.0001f)
    }

    @Test
    fun registrationMotionProfileUsesApprovedBoundsAndDurations() {
        val profile = registrationOrbMotionProfile()

        assertEquals(-3f, profile.minFloatOffsetYDp, 0.0001f)
        assertEquals(3f, profile.maxFloatOffsetYDp, 0.0001f)
        assertEquals(0.985f, profile.minBreathScale, 0.0001f)
        assertEquals(1.015f, profile.maxBreathScale, 0.0001f)
        assertEquals(0.14f, profile.minHaloAlpha, 0.0001f)
        assertEquals(0.24f, profile.maxHaloAlpha, 0.0001f)
        assertEquals(3_200L, profile.floatAndBreathDurationMs)
        assertEquals(2_800L, profile.haloDurationMs)
        assertEquals(12_000L, profile.sweepDurationMs)
        assertEquals(18_000L, profile.ringDurationMs)
        assertEquals(0.97f, profile.pressScale, 0.0001f)
        assertEquals(90L, profile.pressDurationMs)
        assertEquals(220L, profile.releaseDurationMs)
    }

    @Test
    fun reducedMotionResolvesToNeutralStaticOrbValues() {
        val motion = resolveRegistrationOrbMotion(reducedMotion = true)

        assertEquals(0f, motion.offsetYDp, 0.0001f)
        assertEquals(1f, motion.scale, 0.0001f)
        assertEquals(0.14f, motion.haloAlpha, 0.0001f)
        assertEquals(false, motion.continuousMotionEnabled)
    }

    @Test
    fun plasmaSequenceUsesAllTenApprovedFramesInOrder() {
        assertTrue(
            ROYAL_SECURITY_ORB_FRAMES.contentEquals(
                intArrayOf(
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
                ),
            ),
        )
    }

    @Test
    fun reducedMotionFreezesPlasmaAndMagneticRings() {
        val motion = resolveOrbPlasmaMotion(
            state = RoyalSecurityOrbState.LISTENING,
            phase = 0.73f,
            reducedMotion = true,
        )

        assertEquals(0, motion.frameIndex)
        assertEquals(1f, motion.scale, 0.0001f)
        assertEquals(0f, motion.innerRingRotation, 0.0001f)
        assertEquals(0f, motion.outerRingRotation, 0.0001f)
    }

    @Test
    fun plasmaScaleNeverExceedsApprovedStateCeilings() {
        for (step in 0..100) {
            val phase = step / 100f
            val active = resolveOrbPlasmaMotion(RoyalSecurityOrbState.ACTIVE, phase, false)
            val listening = resolveOrbPlasmaMotion(RoyalSecurityOrbState.LISTENING, phase, false)

            assertTrue("active scale=$phase", active.scale in 0.97f..1.06f)
            assertTrue("listening scale=$phase", listening.scale in 0.96f..1.10f)
        }
    }

    @Test
    fun listeningUsesFasterReactorCadenceThanActiveFusion() {
        val active = resolveOrbPlasmaMotion(RoyalSecurityOrbState.ACTIVE, 0.35f, false)
        val listening = resolveOrbPlasmaMotion(RoyalSecurityOrbState.LISTENING, 0.35f, false)

        assertTrue(listening.cycleDurationMs < active.cycleDurationMs)
        assertTrue(listening.haloAlpha > active.haloAlpha)
    }
}
