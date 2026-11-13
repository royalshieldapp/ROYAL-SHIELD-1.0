package com.royalshield.app.ui.voice

import java.text.Normalizer

enum class VoiceDestination {
    FILE_SCAN,
    PHISHING,
    QR_SCANNER,
    VPN,
    SOS,
    MAP,
    SETTINGS,
    AUTOMATION,
    AI_CHAT,
    UNKNOWN,
}

object VoiceCommandRouter {
    fun resolve(command: String): VoiceDestination {
        val normalized = Normalizer.normalize(command, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
            .trim()

        return when {
            containsAny(normalized, "qr", "codigo qr", "escaner qr") -> VoiceDestination.QR_SCANNER
            containsAny(normalized, "phishing", "smishing", "sms sospechoso") -> VoiceDestination.PHISHING
            containsAny(normalized, "vpn", "red privada virtual") -> VoiceDestination.VPN
            containsAny(normalized, "sos", "emergencia", "auxilio", "necesito ayuda") -> VoiceDestination.SOS
            containsAny(normalized, "chatbot", "chat con ia", "asistente de ia", "ai chat") -> VoiceDestination.AI_CHAT
            containsAny(normalized, "scan", "escanear", "malware", "analizar archivos") -> VoiceDestination.FILE_SCAN
            containsAny(normalized, "mapa", "map", "amenazas") -> VoiceDestination.MAP
            containsAny(normalized, "ajustes", "configuracion", "settings") -> VoiceDestination.SETTINGS
            containsAny(normalized, "automatizacion", "automation", "domotica") -> VoiceDestination.AUTOMATION
            else -> VoiceDestination.UNKNOWN
        }
    }

    private fun containsAny(command: String, vararg keywords: String): Boolean =
        keywords.any(command::contains)
}
