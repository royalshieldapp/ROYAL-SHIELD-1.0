package com.royalshield.app.ui.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandRouterTest {
    @Test fun `routes spanish vpn command`() =
        assertEquals(VoiceDestination.VPN, VoiceCommandRouter.resolve("Activa la VPN"))

    @Test fun `routes qr command`() =
        assertEquals(VoiceDestination.QR_SCANNER, VoiceCommandRouter.resolve("Abre el escáner QR"))

    @Test fun `routes phishing command`() =
        assertEquals(VoiceDestination.PHISHING, VoiceCommandRouter.resolve("revisar phishing por SMS"))

    @Test fun `routes sos command`() =
        assertEquals(VoiceDestination.SOS, VoiceCommandRouter.resolve("Necesito ayuda de emergencia"))

    @Test fun `routes chatbot command`() =
        assertEquals(VoiceDestination.AI_CHAT, VoiceCommandRouter.resolve("abre el chatbot"))

    @Test fun `unknown command never invents a destination`() =
        assertEquals(VoiceDestination.UNKNOWN, VoiceCommandRouter.resolve("cuéntame el clima"))
}
