
package com.royalshield.app.managers

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.royalshield.app.billing.BillingVerificationResult
import com.royalshield.app.billing.BillingVerificationDecision
import com.royalshield.app.billing.BillingVerifier
import com.royalshield.app.billing.RenderBillingVerifier
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager for Google Play Billing
 * Handles the $0.99 paywall for Security Screen
 */
class BillingManager(
    private val context: Context,
    private val billingVerifier: BillingVerifier = RenderBillingVerifier(),
) : PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _hasPremiumAccess = MutableStateFlow(false)
    val hasPremiumAccess: StateFlow<Boolean> = _hasPremiumAccess.asStateFlow()

    private val _currentProduct = MutableStateFlow<String?>(null)
    val currentProduct: StateFlow<String?> = _currentProduct.asStateFlow()

    companion object {
        private const val TAG = "BillingManager"

        // Product IDs (must match Google Play Console)
        const val PRODUCT_SECURITY_ACCESS = "security_access_099"
        const val PRODUCT_STARTER = "lifetime_starter" // Unified ID
        const val PRODUCT_GOLD = "lifetime_gold"       // Unified ID
        const val PRODUCT_ULTIMATE = "lifetime_ultimate" // Unified ID

    }

    sealed class PurchaseState {
        object Idle : PurchaseState()
        object Loading : PurchaseState()
        data class Success(val productId: String) : PurchaseState()
        data class Error(val message: String) : PurchaseState()
        object Cancelled : PurchaseState()
    }

    /**
     * Initializes the billing client
     */
    fun initialize(onReady: () -> Unit = {}) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected")
                    // Check existing purchases
                    queryPurchases()
                    onReady()
                } else {
                    Log.e(TAG, "Error connecting billing: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                // Retry connection
            }
        })
    }

    /**
     * Checks if the user has premium access (purchase or Duo Shield)
     * DEV MODE: Always returns true for full access
     */
    suspend fun checkPremiumAccess(): Boolean {
        return _hasPremiumAccess.value
    }

    /**
     * Queries available products
     */
    suspend fun queryProducts(): List<com.royalshield.app.models.ProductInfo> {
        return try {
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

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val result = billingClient?.queryProductDetails(params)

            if (result?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                result.productDetailsList?.map { detail ->
                    val offer = detail.oneTimePurchaseOfferDetails
                    com.royalshield.app.models.ProductInfo(
                        id = detail.productId,
                        title = detail.name,
                        description = detail.description,
                        price = offer?.formattedPrice ?: "N/A",
                        originalDetails = detail
                    )
                } ?: emptyList()
            } else {
                Log.e(TAG, "Error querying products: ${result?.billingResult?.debugMessage}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying products", e)
            emptyList()
        }
    }

    /**
     * Launches the purchase flow
     */
    /**
     * Launches the purchase flow
     */
    fun launchPurchaseFlow(activity: Activity, productInfo: com.royalshield.app.models.ProductInfo) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _purchaseState.value = PurchaseState.Error("Authentication required")
            return
        }
        val productDetails = productInfo.originalDetails
        if (productDetails == null) {
            _purchaseState.value = PurchaseState.Error("Product details not available")
            return
        }

        _purchaseState.value = PurchaseState.Loading

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .setObfuscatedAccountId(BillingVerificationDecision.obfuscatedAccountId(uid))
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Callback when a purchase is updated
     */
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Cancelled
                Log.d(TAG, "Purchase cancelled by the user")
            }
            else -> {
                _purchaseState.value = PurchaseState.Error(
                    billingResult.debugMessage ?: "Unknown error"
                )
                Log.e(TAG, "Purchase error: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Processes a purchase
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            _purchaseState.value = PurchaseState.Error("Purchase is pending")
            return
        }

        billingVerifier.verify(purchase) { result ->
            when (result) {
                is BillingVerificationResult.Rejected -> {
                    _hasPremiumAccess.value = false
                    _currentProduct.value = null
                    _purchaseState.value = PurchaseState.Error(result.message)
                }
                is BillingVerificationResult.Verified -> {
                    val productId = purchase.products.singleOrNull()
                    if (productId == null) {
                        _purchaseState.value = PurchaseState.Error("Invalid product")
                        return@verify
                    }
                    if (!purchase.isAcknowledged && !result.acknowledged) {
                        acknowledgePurchase(purchase) { acknowledged ->
                            if (acknowledged) grantVerifiedPurchase(productId)
                            else _purchaseState.value = PurchaseState.Error("Purchase verified but acknowledgement failed")
                        }
                    } else {
                        grantVerifiedPurchase(productId)
                    }
                }
            }
        }
    }

    private fun grantVerifiedPurchase(productId: String) {
        _currentProduct.value = productId
        _hasPremiumAccess.value = true
        _purchaseState.value = PurchaseState.Success(productId)
    }

    /**
     * Acknowledges the purchase in Google Play
     */
    private fun acknowledgePurchase(purchase: Purchase, onResult: (Boolean) -> Unit) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully")
            }
            onResult(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
        }
    }

    /**
     * Queries existing user purchases
     */
    private fun queryPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _hasPremiumAccess.value = false
                _currentProduct.value = null
                purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    .forEach(::handlePurchase)
                Log.d(TAG, "Existing purchases: ${purchases.size}")
            }
        }
    }

    /**
     * Releases resources
     */
    fun release() {
        billingClient?.endConnection()
        billingClient = null
    }
}
