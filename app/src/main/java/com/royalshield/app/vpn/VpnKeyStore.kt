package com.royalshield.app.vpn

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import java.util.UUID

/**
 * Persists the WireGuard client identity using Android Keystore-backed encryption.
 * The private key never leaves the device.
 */
class VpnKeyStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFS_NAME,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getOrCreateKeyPair(): KeyPair {
        val storedPrivateKey = preferences.getString(KEY_PRIVATE_KEY, null)
        if (!storedPrivateKey.isNullOrBlank()) {
            return KeyPair(Key.fromBase64(storedPrivateKey))
        }

        val keyPair = KeyPair()
        check(
            preferences.edit()
                .putString(KEY_PRIVATE_KEY, keyPair.privateKey.toBase64())
                .commit()
        ) { "Unable to persist WireGuard private key" }
        return keyPair
    }

    fun getOrCreateClientId(): String {
        preferences.getString(KEY_CLIENT_ID, null)?.takeIf { it.isNotBlank() }?.let { return it }

        val clientId = UUID.randomUUID().toString()
        check(preferences.edit().putString(KEY_CLIENT_ID, clientId).commit()) {
            "Unable to persist VPN client identity"
        }
        return clientId
    }

    private companion object {
        private const val PREFS_NAME = "royal_vpn_wireguard_identity"
        private const val KEY_PRIVATE_KEY = "private_key"
        private const val KEY_CLIENT_ID = "client_id"
    }
}
