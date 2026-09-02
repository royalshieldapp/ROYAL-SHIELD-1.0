package com.royalshield.app.securitysetup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecuritySetupStoreTest {
    @Test
    fun `setup presents exactly vpn sms and qr in order`() {
        assertEquals(
            listOf(SecurityFeature.VPN, SecurityFeature.SMS_PHISHING, SecurityFeature.QR_SCANNER),
            SecuritySetupFlow.features,
        )
    }

    @Test
    fun `new authenticated user needs setup`() {
        val store = SecuritySetupMemoryStore()

        assertTrue(store.needsSetup("uid-a"))
        assertFalse(store.load("uid-a").completed)
    }

    @Test
    fun `declines persist and completion requires every choice`() {
        val store = SecuritySetupMemoryStore()
        store.saveChoice("uid-a", SecurityFeature.VPN, false)
        store.saveChoice("uid-a", SecurityFeature.SMS_PHISHING, true)

        assertTrue(store.needsSetup("uid-a"))

        store.saveChoice("uid-a", SecurityFeature.QR_SCANNER, false)
        store.markCompleted("uid-a")

        val saved = store.load("uid-a")
        assertTrue(saved.completed)
        assertEquals(false, saved.vpnEnabled)
        assertEquals(true, saved.smsPhishingEnabled)
        assertEquals(false, saved.qrScannerEnabled)
    }

    @Test
    fun `choices are isolated by firebase uid`() {
        val store = SecuritySetupMemoryStore()
        store.saveChoice("uid-a", SecurityFeature.VPN, true)

        assertEquals(true, store.load("uid-a").vpnEnabled)
        assertEquals(null, store.load("uid-b").vpnEnabled)
    }

    @Test
    fun `session completes only after answering all three questions`() {
        val session = SecuritySetupSession()

        assertEquals(SecurityFeature.VPN, session.currentFeature)
        assertFalse(session.answer(false))
        assertEquals(SecurityFeature.SMS_PHISHING, session.currentFeature)
        assertFalse(session.answer(true))
        assertEquals(SecurityFeature.QR_SCANNER, session.currentFeature)
        assertTrue(session.answer(false))
        assertEquals(null, session.currentFeature)
    }

    @Test
    fun `session resumes at first unanswered feature`() {
        val saved = SecuritySetupPreferences(
            uid = "uid-a",
            vpnEnabled = false,
            smsPhishingEnabled = true,
            qrScannerEnabled = null,
        )

        val session = SecuritySetupSession(saved)

        assertEquals(SecurityFeature.QR_SCANNER, session.currentFeature)
    }

    @Test
    fun `review mode starts again at vpn without deleting saved choices`() {
        val saved = SecuritySetupPreferences(
            uid = "uid-a",
            completed = true,
            vpnEnabled = true,
            smsPhishingEnabled = false,
            qrScannerEnabled = true,
        )

        val startingPreferences = securitySetupStartingPreferences(saved, reviewExisting = true)
        val session = SecuritySetupSession(startingPreferences)

        assertEquals(SecurityFeature.VPN, session.currentFeature)
        assertEquals(true, saved.vpnEnabled)
        assertEquals(false, saved.smsPhishingEnabled)
        assertEquals(true, saved.qrScannerEnabled)
    }
}
