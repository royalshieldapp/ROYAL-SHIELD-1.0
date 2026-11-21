package com.royalshield.app.managers

import com.royalshield.app.managers.PreferencesManager
import com.royalshield.app.subscription.EntitlementStore

/**
 * Centralized Feature Gating Logic.
 * When DEV_SUPERPOWERS is active, ALL features are unlocked.
 */
object FeatureManager {

    enum class Feature {
        BASIC_PROTECTION, // Always available
        FEATURE_A,        // Starter+
        FEATURE_B,        // Starter+
        FEATURE_C,        // Starter+
        FEATURE_D,        // Gold+
        FEATURE_E,        // Gold+
        GOLD_THEME,       // Gold+ (Visual)
        ELITE_BADGE,      // Ultimate only
        ALL_ACCESS        // Ultimate
    }

    private fun getCurrentPack(): String {
        return PreferencesManager.getPurchasedPack() ?: "free"
    }

    fun isFeatureEnabled(feature: Feature): Boolean {
        // SUPERPOWERS: All features unlocked
        if (EntitlementStore.DEV_SUPERPOWERS) return true
        
        val pack = getCurrentPack()
        
        // ULTIMATE HAS EVERYTHING
        if (pack == BillingManager.PRODUCT_ULTIMATE) return true
        
        return when (feature) {
            Feature.BASIC_PROTECTION -> true
            
            Feature.FEATURE_A, Feature.FEATURE_B, Feature.FEATURE_C -> {
                pack == BillingManager.PRODUCT_STARTER || pack == BillingManager.PRODUCT_GOLD
            }
            
            Feature.FEATURE_D, Feature.FEATURE_E, Feature.GOLD_THEME -> {
                pack == BillingManager.PRODUCT_GOLD
            }
            
            Feature.ELITE_BADGE, Feature.ALL_ACCESS -> false // Only Ultimate handled above
            
            else -> false
        }
    }
}

