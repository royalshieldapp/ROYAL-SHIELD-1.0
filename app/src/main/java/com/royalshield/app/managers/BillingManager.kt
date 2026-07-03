
package com.royalshield.app.managers

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import com.royalshield.app.managers.PreferencesManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Manager for Google Play Billing
 * Handles the $0.99 paywall for Security Screen
 */
class BillingManager(private val context: Context) : PurchasesUpdatedListener {
    
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    // DEV MODE: Set to true to simulate purchases on emulator + FULL PREMIUM ACCESS
    private val isDevMode = false
    
    private var billingClient: BillingClient? = null
    
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()
    
    private val _hasPremiumAccess = MutableStateFlow(false) // FREE MODE
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
        
        private const val COLLECTION_PURCHASES = "purchases"
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
        if (isDevMode) {
            _hasPremiumAccess.value = true
            return true
        }
        
        return try {
            val uid = auth.currentUser?.uid ?: return false
            
            // Verify in Firestore if user has SOLO or DUO plan
            val userDoc = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            
            if (userDoc.exists()) {
                val planTier = userDoc.getString("planTier") ?: "FREE"
                val premiumExpires = userDoc.getLong("premiumExpiresAt")
                
                val hasPlan = planTier == "SOLO" || planTier == "DUO" || planTier == "GOLD" || planTier == "ULTIMATE" || planTier == "STARTER"
                
                val hasPremiumTemp = premiumExpires?.let { 
                    System.currentTimeMillis() < it 
                } ?: false
                
                val hasAccess = hasPlan || hasPremiumTemp
                _hasPremiumAccess.value = hasAccess
                // Also update local cache if we found a specific pack
                val productId = when(planTier) {
                    "STARTER" -> PRODUCT_STARTER
                    "GOLD" -> PRODUCT_GOLD
                    "ULTIMATE" -> PRODUCT_ULTIMATE
                    else -> null
                }
                
                if (productId != null) {
                   PreferencesManager.savePurchasedPack(productId)
                }
                return hasAccess
            }
            // Fallback to local
            val localPack = PreferencesManager.getPurchasedPack()
            if (localPack != null) {
                _hasPremiumAccess.value = true
                return true
            }
            
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying premium access", e)
            false
        }
    }
    
    /**
     * Queries available products
     */
    suspend fun queryProducts(): List<com.royalshield.app.models.ProductInfo> {
        if (isDevMode) {
            kotlinx.coroutines.delay(500) // Simular latencia
            return listOf(
                com.royalshield.app.models.ProductInfo(PRODUCT_STARTER, "Starter Pack", "Essential protection", "$9.99", null),
                com.royalshield.app.models.ProductInfo(PRODUCT_GOLD, "Gold Bundle", "Advanced automation", "$19.99", null),
                com.royalshield.app.models.ProductInfo(PRODUCT_ULTIMATE, "Ultimate Pack", "Lifetime Elite status", "$39.99", null)
            )
        }

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
        if (isDevMode) {
            _purchaseState.value = PurchaseState.Loading
            // Simular proceso de Google Play
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                kotlinx.coroutines.delay(1000)
                // Éxito simulado
                PreferencesManager.savePurchasedPack(productInfo.id)
                _hasPremiumAccess.value = true
                _currentProduct.value = productInfo.id
                _purchaseState.value = PurchaseState.Success(productInfo.id)
            }
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
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
            
            // Identify product
            val productId = purchase.products.firstOrNull() ?: "unknown"
            
            // Save locally
            PreferencesManager.savePurchasedPack(productId)
            
            // Update user access in Firestore
            savePurchaseToFirestore(purchase)
            
            _purchaseState.value = PurchaseState.Success(productId)
            _hasPremiumAccess.value = true
        }
    }
    
    /**
     * Acknowledges the purchase in Google Play
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully")
            }
        }
    }
    
    /**
     * Saves the purchase to Firestore
     */
    private fun savePurchaseToFirestore(purchase: Purchase) {
        val uid = auth.currentUser?.uid ?: return
        val productId = purchase.products.firstOrNull() ?: ""
        
        val purchaseData = mapOf(
            "uid" to uid,
            "productId" to productId,
            "purchaseToken" to purchase.purchaseToken,
            "purchaseTime" to purchase.purchaseTime,
            "orderId" to (purchase.orderId ?: ""),
            "acknowledged" to purchase.isAcknowledged
        )
        
        firestore.collection(COLLECTION_PURCHASES)
            .document("${uid}_${purchase.orderId}")
            .set(purchaseData)
            .addOnSuccessListener {
                Log.d(TAG, "Purchase saved in Firestore")
                
                // Determine Tier from ID
                val planTier = when(productId) {
                    PRODUCT_STARTER -> "STARTER"
                    PRODUCT_GOLD -> "GOLD"
                    PRODUCT_ULTIMATE -> "ULTIMATE"
                    else -> "SOLO"
                }
                
                // Update user plan
                val userUpdates = mapOf(
                    "planTier" to planTier,
                    "lastPurchaseAt" to System.currentTimeMillis()
                )
                
                firestore.collection("users")
                    .document(uid)
                    .update(userUpdates)
                    .addOnSuccessListener {
                        Log.d(TAG, "Plan updated to $planTier")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving purchase", e)
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
                val hasActivePurchase = purchases.any { 
                    it.purchaseState == Purchase.PurchaseState.PURCHASED 
                }
                _hasPremiumAccess.value = if (isDevMode) true else hasActivePurchase
                
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
