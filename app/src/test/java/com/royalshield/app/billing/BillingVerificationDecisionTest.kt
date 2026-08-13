package com.royalshield.app.billing

import com.royalshield.app.subscription.SubscriptionTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BillingVerificationDecisionTest {
    @Test
    fun `grants tier only for ready server verified entitlement`() {
        assertEquals(
            SubscriptionTier.GOLD,
            BillingVerificationDecision.tierFor("READY", true, "GOLD")
        )
    }

    @Test
    fun `does not grant tier for unverified response`() {
        assertNull(BillingVerificationDecision.tierFor("READY", false, "ULTIMATE"))
        assertNull(BillingVerificationDecision.tierFor("UNAVAILABLE", true, "ULTIMATE"))
        assertNull(BillingVerificationDecision.tierFor("READY", true, "UNKNOWN"))
    }

    @Test
    fun `account id is a stable hash and never exposes firebase uid`() {
        val uid = "firebase-user-1"
        val hash = BillingVerificationDecision.obfuscatedAccountId(uid)

        assertEquals(64, hash.length)
        assertEquals(hash, BillingVerificationDecision.obfuscatedAccountId(uid))
        assertEquals(false, hash.contains(uid))
    }
}
