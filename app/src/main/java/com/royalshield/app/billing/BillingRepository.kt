package com.royalshield.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.royalshield.app.subscription.SubscriptionTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingRepository(
    private val context: Context,
    private val onTierUpgrade: (SubscriptionTier) -> Unit
) : PurchasesUpdatedListener {

    private val _billingConnectionState = MutableStateFlow(false)
    val billingConnectionState = _billingConnectionState.asStateFlow()

    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails = _productDetails.asStateFlow()

    private val _purchaseStatus = MutableStateFlow<String?>(null)
    val purchaseStatus = _purchaseStatus.asStateFlow()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    companion object {
        const val PRODUCT_STARTER = "lifetime_starter"
        const val PRODUCT_GOLD = "lifetime_gold"
        const val PRODUCT_ULTIMATE = "lifetime_ultimate"
    }

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _billingConnectionState.value = true
                    queryProductDetails()
                    restorePurchases()
                } else {
                    Log.e("BillingRepository", "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingConnectionState.value = false
                // Retry logic could be implemented here
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_STARTER)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_GOLD)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ULTIMATE)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = productDetailsList
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _purchaseStatus.value = "Cancelled"
        } else {
            _purchaseStatus.value = "Error: ${billingResult.debugMessage}"
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _purchaseStatus.value = "Success"
                        checkEntitlements(listOf(purchase))
                    }
                }
            } else {
                checkEntitlements(listOf(purchase))
            }
        }
    }

    fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        
        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                 checkEntitlements(purchasesList)
            }
        }
    }

    private fun checkEntitlements(purchases: List<Purchase>) {
        var highestTier = SubscriptionTier.FREE
        
        for (purchase in purchases) {
            if (purchase.products.contains(PRODUCT_ULTIMATE)) {
                highestTier = SubscriptionTier.ULTIMATE
            } else if (purchase.products.contains(PRODUCT_GOLD) && highestTier.rank < SubscriptionTier.GOLD.rank) {
                highestTier = SubscriptionTier.GOLD
            } else if (purchase.products.contains(PRODUCT_STARTER) && highestTier.rank < SubscriptionTier.STARTER.rank) {
                highestTier = SubscriptionTier.STARTER
            }
        }
        
        if (highestTier != SubscriptionTier.FREE) {
            onTierUpgrade(highestTier)
        }
    }

    fun clearStatus() {
        _purchaseStatus.value = null
    }
}
