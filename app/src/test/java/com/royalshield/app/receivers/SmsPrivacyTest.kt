package com.royalshield.app.receivers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsPrivacyTest {
    @Test
    fun `diagnostic event excludes sms body sender and url`() {
        val body = "Banco: visita https://phishing.example ahora"
        val sender = "+15551234567"

        val diagnostic = SmsPrivacy.receivedMessage(urlCount = 1)

        assertTrue(diagnostic.contains("urlCount=1"))
        assertFalse(diagnostic.contains(body))
        assertFalse(diagnostic.contains(sender))
        assertFalse(diagnostic.contains("https://"))
    }
}
