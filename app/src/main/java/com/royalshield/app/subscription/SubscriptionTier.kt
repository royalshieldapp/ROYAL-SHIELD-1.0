package com.royalshield.app.subscription

enum class SubscriptionTier(val rank: Int) {
    FREE(0),
    STARTER(1),
    GOLD(2),
    ULTIMATE(3);

    fun isAtLeast(other: SubscriptionTier): Boolean {
        return this.rank >= other.rank
    }
}
