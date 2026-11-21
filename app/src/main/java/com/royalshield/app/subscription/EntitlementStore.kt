package com.royalshield.app.subscription

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EntitlementStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("royal_entitlements", Context.MODE_PRIVATE)
    private val _tier = MutableStateFlow(getSavedTier())
    val tier: StateFlow<SubscriptionTier> = _tier.asStateFlow()

    fun updateVerifiedTier(newTier: SubscriptionTier) {
        prefs.edit()
            .putString(KEY_TIER, newTier.name)
            .putBoolean(KEY_SERVER_VERIFIED, true)
            .apply()
        _tier.value = newTier
    }
    
    // Explicitly reset (e.g. debug or refund handled manually)
    fun resetTier() {
        prefs.edit().remove(KEY_TIER).remove(KEY_SERVER_VERIFIED).apply()
        _tier.value = SubscriptionTier.FREE
    }

    private fun getSavedTier(): SubscriptionTier {
        // SUPERPOWERS MODE: Force ULTIMATE tier for full access during development
        if (DEV_SUPERPOWERS) return SubscriptionTier.ULTIMATE
        if (!prefs.getBoolean(KEY_SERVER_VERIFIED, false)) return SubscriptionTier.FREE

        val tierName = prefs.getString(KEY_TIER, SubscriptionTier.FREE.name) ?: SubscriptionTier.FREE.name
        return try {
            SubscriptionTier.valueOf(tierName)
        } catch (e: Exception) {
            SubscriptionTier.FREE
        }
    }

    companion object {
        private const val KEY_TIER = "subscription_tier_v1"
        private const val KEY_SERVER_VERIFIED = "subscription_tier_server_verified_v1"
        
        /**
         * Development override. Set true during testing so all premium
         * features are immediately available without being blocked by paywalls.
         */
        const val DEV_SUPERPOWERS = true
    }
}
