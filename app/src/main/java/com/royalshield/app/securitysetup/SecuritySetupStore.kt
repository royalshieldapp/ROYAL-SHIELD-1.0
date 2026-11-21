package com.royalshield.app.securitysetup

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

interface SecuritySetupStore {
    fun load(uid: String): SecuritySetupPreferences
    fun saveChoice(uid: String, feature: SecurityFeature, enabled: Boolean)
    fun markCompleted(uid: String)
    fun needsSetup(uid: String): Boolean = !load(uid).completed
}

class SecuritySetupMemoryStore : SecuritySetupStore {
    private val values = mutableMapOf<String, SecuritySetupPreferences>()

    override fun load(uid: String): SecuritySetupPreferences =
        values[uid] ?: SecuritySetupPreferences(uid = uid)

    override fun saveChoice(uid: String, feature: SecurityFeature, enabled: Boolean) {
        val current = load(uid)
        values[uid] = when (feature) {
            SecurityFeature.VPN -> current.copy(vpnEnabled = enabled, completed = false)
            SecurityFeature.SMS_PHISHING -> current.copy(smsPhishingEnabled = enabled, completed = false)
            SecurityFeature.QR_SCANNER -> current.copy(qrScannerEnabled = enabled, completed = false)
        }
    }

    override fun markCompleted(uid: String) {
        val current = load(uid)
        if (current.hasEveryChoice()) values[uid] = current.copy(completed = true)
    }
}

class EncryptedSecuritySetupStore(context: Context) : SecuritySetupStore {
    private val preferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        "royal_security_setup",
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun load(uid: String): SecuritySetupPreferences {
        val prefix = prefix(uid)
        val vpn = optionalBoolean("${prefix}_vpn")
        val sms = optionalBoolean("${prefix}_sms")
        val qr = optionalBoolean("${prefix}_qr")
        val everyChoice = vpn != null && sms != null && qr != null
        return SecuritySetupPreferences(
            uid = uid,
            completed = everyChoice && preferences.getBoolean("${prefix}_completed", false),
            vpnEnabled = vpn,
            smsPhishingEnabled = sms,
            qrScannerEnabled = qr,
        )
    }

    override fun saveChoice(uid: String, feature: SecurityFeature, enabled: Boolean) {
        val suffix = when (feature) {
            SecurityFeature.VPN -> "vpn"
            SecurityFeature.SMS_PHISHING -> "sms"
            SecurityFeature.QR_SCANNER -> "qr"
        }
        preferences.edit()
            .putBoolean("${prefix(uid)}_$suffix", enabled)
            .putBoolean("${prefix(uid)}_completed", false)
            .apply()
    }

    override fun markCompleted(uid: String) {
        if (load(uid).hasEveryChoice()) {
            preferences.edit().putBoolean("${prefix(uid)}_completed", true).apply()
        }
    }

    private fun optionalBoolean(key: String): Boolean? =
        if (preferences.contains(key)) preferences.getBoolean(key, false) else null

    private fun prefix(uid: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
