package com.royalshield.app.featuregates

import com.royalshield.app.subscription.SubscriptionTier

object Capabilities {
    
    fun canUseFileScanner(tier: SubscriptionTier) = tier.isAtLeast(SubscriptionTier.STARTER)
    fun canUseAppScan(tier: SubscriptionTier) = tier.isAtLeast(SubscriptionTier.STARTER)
    fun canUsePermissionMonitor(tier: SubscriptionTier) = tier.isAtLeast(SubscriptionTier.STARTER)
    
    fun canUseScreenProtection(tier: SubscriptionTier) = tier.isAtLeast(SubscriptionTier.GOLD)
    fun canUseClipboardProtection(tier: SubscriptionTier) = tier.isAtLeast(SubscriptionTier.GOLD)
    fun canUseAutomation(tier: SubscriptionTier) = tier.isAtLeast(SubscriptionTier.GOLD)
    
    fun canUseAiAnalysis(tier: SubscriptionTier) = tier.isAtLeast(SubscriptionTier.ULTIMATE)
    fun canUseLiveThreatMonitor(tier: SubscriptionTier) = tier.isAtLeast(SubscriptionTier.ULTIMATE)
    fun canUseStealthMode(tier: SubscriptionTier) = tier.isAtLeast(SubscriptionTier.ULTIMATE)
}
