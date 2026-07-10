package com.royalshield.app.featuregates

import com.royalshield.app.subscription.EntitlementStore
import com.royalshield.app.subscription.SubscriptionTier

/**
 * Capability checks for all app features.
 * When DEV_SUPERPOWERS is active, ALL capabilities return true.
 */
object Capabilities {
    
    // ── Starter Tier Features ──
    fun canUseFileScanner(tier: SubscriptionTier) = EntitlementStore.DEV_SUPERPOWERS || tier.isAtLeast(SubscriptionTier.STARTER)
    fun canUseAppScan(tier: SubscriptionTier) = EntitlementStore.DEV_SUPERPOWERS || tier.isAtLeast(SubscriptionTier.STARTER)
    fun canUsePermissionMonitor(tier: SubscriptionTier) = EntitlementStore.DEV_SUPERPOWERS || tier.isAtLeast(SubscriptionTier.STARTER)
    
    // ── Gold Tier Features ──
    fun canUseScreenProtection(tier: SubscriptionTier) = EntitlementStore.DEV_SUPERPOWERS || tier.isAtLeast(SubscriptionTier.GOLD)
    fun canUseClipboardProtection(tier: SubscriptionTier) = EntitlementStore.DEV_SUPERPOWERS || tier.isAtLeast(SubscriptionTier.GOLD)
    fun canUseAutomation(tier: SubscriptionTier) = EntitlementStore.DEV_SUPERPOWERS || tier.isAtLeast(SubscriptionTier.GOLD)
    
    // ── Ultimate Tier Features ──
    fun canUseAiAnalysis(tier: SubscriptionTier) = EntitlementStore.DEV_SUPERPOWERS || tier.isAtLeast(SubscriptionTier.ULTIMATE)
    fun canUseLiveThreatMonitor(tier: SubscriptionTier) = EntitlementStore.DEV_SUPERPOWERS || tier.isAtLeast(SubscriptionTier.ULTIMATE)
    fun canUseStealthMode(tier: SubscriptionTier) = EntitlementStore.DEV_SUPERPOWERS || tier.isAtLeast(SubscriptionTier.ULTIMATE)
}

