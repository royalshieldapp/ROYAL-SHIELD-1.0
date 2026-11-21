package com.royalshield.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.google.firebase.auth.FirebaseAuth
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
    private val billingVerifier: BillingVerifier = RenderBillingVerifier(),
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _purchaseStatus.value = "Authentication required"
            return
        }
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .setObfuscatedAccountId(BillingVerificationDecision.obfuscatedAccountId(uid))
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
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            _purchaseStatus.value = "Pending"
            return
        }

        _purchaseStatus.value = "Verifying"
        billingVerifier.verify(purchase) { result ->
            when (result) {
                is BillingVerificationResult.Rejected -> {
                    _purchaseStatus.value = "Not verified: ${result.message}"
                }
                is BillingVerificationResult.Verified -> {
                    if (!purchase.isAcknowledged && !result.acknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _purchaseStatus.value = "Success"
                                onTierUpgrade(result.tier)
                            } else {
                                _purchaseStatus.value = "Verified, acknowledgement failed"
                    }
                }
            } else {
                        _purchaseStatus.value = "Success"
                        onTierUpgrade(result.tier)
                    }
                }
            }
        }
    }

    fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        
        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchasesList.isEmpty()) {
                    _purchaseStatus.value = "No purchases to restore"
                    onTierUpgrade(SubscriptionTier.FREE)
                } else {
                    purchasesList.forEach(::handlePurchase)
                }
            }
        }
    }

    fun clearStatus() {
        _purchaseStatus.value = null
    }
}
