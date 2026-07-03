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

    fun updateTier(newTier: SubscriptionTier) {
        val current = _tier.value
        // Only upgrade, never downgrade via simple update (unless explicit reset)
        // Actually, for restore logic, we might need to overwrite.
        // Let's rely on the billing repository to determine the *highest* valid entitlement.
        
        prefs.edit().putString(KEY_TIER, newTier.name).apply()
        _tier.value = newTier
    }
    
    // Explicitly reset (e.g. debug or refund handled manually)
    fun resetTier() {
        prefs.edit().remove(KEY_TIER).apply()
        _tier.value = SubscriptionTier.FREE
    }

    private fun getSavedTier(): SubscriptionTier {
        val tierName = prefs.getString(KEY_TIER, SubscriptionTier.FREE.name) ?: SubscriptionTier.FREE.name
        return try {
            SubscriptionTier.valueOf(tierName)
        } catch (e: Exception) {
            SubscriptionTier.FREE
        }
    }

    companion object {
        private const val KEY_TIER = "subscription_tier_v1"
    }
}
