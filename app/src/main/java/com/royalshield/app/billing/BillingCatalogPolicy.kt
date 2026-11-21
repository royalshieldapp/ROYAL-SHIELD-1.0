package com.royalshield.app.billing

sealed interface BillingCatalogState {
    data object WaitingForPlay : BillingCatalogState
    data object Ready : BillingCatalogState
    data class Unavailable(val message: String) : BillingCatalogState
}

object BillingCatalogPolicy {
    val requiredProductIds = setOf(
        "lifetime_starter",
        "lifetime_gold",
        "lifetime_ultimate",
    )

    fun evaluate(
        isBillingReady: Boolean,
        returnedProductIds: Set<String>,
    ): BillingCatalogState {
        if (!isBillingReady) return BillingCatalogState.WaitingForPlay

        val missingProducts = requiredProductIds - returnedProductIds
        return if (missingProducts.isEmpty()) {
            BillingCatalogState.Ready
        } else {
            BillingCatalogState.Unavailable(
                "Google Play did not return the required plans: " +
                    missingProducts.sorted().joinToString(),
            )
        }
    }
}
