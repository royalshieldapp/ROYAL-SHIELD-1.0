package com.royalshield.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoyalEliteBadgePresentationTest {

    @Test
    fun promotionalBadgeDescribesAnEliteBenefitPreviewWithoutClaimingActivation() {
        val presentation = royalEliteBadgePresentation(RoyalEliteBadgeMode.PROMOTIONAL)

        assertEquals("Preview of Royal Elite benefit", presentation.statusText)
        assertEquals("Royal Elite benefit preview", presentation.contentDescription)
        assertEquals(1f, presentation.alpha, 0f)
        assertFalse(presentation.showLock)
    }

    @Test
    fun lockedBadgeMakesTheUnavailableEliteStatusClear() {
        val presentation = royalEliteBadgePresentation(RoyalEliteBadgeMode.LOCKED)

        assertEquals("Royal Elite badge locked", presentation.statusText)
        assertEquals("Royal Elite badge locked", presentation.contentDescription)
        assertEquals(0.58f, presentation.alpha, 0f)
        assertTrue(presentation.showLock)
    }

    @Test
    fun verifiedActiveBadgeUsesAnExplicitVerifiedStatus() {
        val presentation = royalEliteBadgePresentation(RoyalEliteBadgeMode.ACTIVE_VERIFIED)

        assertEquals("Royal Elite verified active badge", presentation.statusText)
        assertEquals("Royal Elite verified active badge", presentation.contentDescription)
        assertEquals(1f, presentation.alpha, 0f)
        assertFalse(presentation.showLock)
    }

    @Test
    fun subscriptionBadgeRemainsPromotionalWhenLocalPremiumStateIsTrue() {
        assertEquals(RoyalEliteBadgeMode.PROMOTIONAL, subscriptionEliteBadgeMode(isPremium = true))
    }

    @Test
    fun subscriptionBadgeRemainsPromotionalWhenLocalPremiumStateIsFalse() {
        assertEquals(RoyalEliteBadgeMode.PROMOTIONAL, subscriptionEliteBadgeMode(isPremium = false))
    }
}
