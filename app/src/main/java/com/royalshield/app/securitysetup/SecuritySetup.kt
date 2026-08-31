package com.royalshield.app.securitysetup

enum class SecurityFeature {
    VPN,
    SMS_PHISHING,
    QR_SCANNER,
}

object SecuritySetupFlow {
    val features: List<SecurityFeature> = listOf(
        SecurityFeature.VPN,
        SecurityFeature.SMS_PHISHING,
        SecurityFeature.QR_SCANNER,
    )
}

class SecuritySetupSession(saved: SecuritySetupPreferences? = null) {
    private var index: Int = when {
        saved?.vpnEnabled == null -> 0
        saved.smsPhishingEnabled == null -> 1
        saved.qrScannerEnabled == null -> 2
        else -> SecuritySetupFlow.features.size
    }

    val currentFeature: SecurityFeature?
        get() = SecuritySetupFlow.features.getOrNull(index)

    fun answer(@Suppress("UNUSED_PARAMETER") enabled: Boolean): Boolean {
        if (currentFeature == null) return true
        index += 1
        return currentFeature == null
    }
}

data class SecuritySetupPreferences(
    val uid: String,
    val completed: Boolean = false,
    val vpnEnabled: Boolean? = null,
    val smsPhishingEnabled: Boolean? = null,
    val qrScannerEnabled: Boolean? = null,
) {
    fun hasEveryChoice(): Boolean =
        vpnEnabled != null && smsPhishingEnabled != null && qrScannerEnabled != null
}
