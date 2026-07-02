package com.royalshield.app.subscription

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.royalshield.app.billing.BillingRepository
import kotlinx.coroutines.flow.StateFlow

class SubscriptionManager(context: Context) {

    private val entitlementStore = EntitlementStore(context)
    
    // Pass a callback to Billing Repo so it can update the store when a purchase happens
    private val billingRepository = BillingRepository(context) { newTier ->
        // Only update if the new tier is higher than the old one (or simply update)
        // EntitlementStore logic will handle it, but for simplicity here we just pass it on.
        // We typically want to respect the highest tier purchased.
        val current = entitlementStore.tier.value
        if (newTier.rank > current.rank) {
            entitlementStore.updateTier(newTier)
        } else if (newTier.rank == current.rank) {
             // Re-verify
             entitlementStore.updateTier(newTier)
        }
    }

    val currentTier: StateFlow<SubscriptionTier> = entitlementStore.tier
    val productDetails: StateFlow<List<ProductDetails>> = billingRepository.productDetails
    val purchaseStatus: StateFlow<String?> = billingRepository.purchaseStatus

    init {
        billingRepository.startConnection()
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        billingRepository.launchBillingFlow(activity, productDetails)
    }

    fun restorePurchases() {
        // Force a re-query from Google Play
        billingRepository.restorePurchases()
    }
    
    fun clearStatus() {
        billingRepository.clearStatus()
    }
    
    // Static singleton accessor for simplicity in this demo context
    companion object {
        @Volatile
        private var INSTANCE: SubscriptionManager? = null

        fun getInstance(context: Context): SubscriptionManager {
            return INSTANCE ?: synchronized(this) {
                SubscriptionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
