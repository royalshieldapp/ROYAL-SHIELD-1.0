package com.royalshield.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.models.ProductInfo
import com.royalshield.app.managers.BillingManager
import com.royalshield.app.managers.PreferencesManager
import com.royalshield.app.billing.BillingCatalogPolicy
import com.royalshield.app.billing.BillingCatalogState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PaywallUiState {
    object Loading : PaywallUiState()
    data class ProductsReady(
        val starter: ProductInfo?,
        val gold: ProductInfo?,
        val ultimate: ProductInfo?
    ) : PaywallUiState()
    object Purchasing : PaywallUiState()
    data class Success(val message: String) : PaywallUiState()
    data class Error(val message: String) : PaywallUiState()
}

class PaywallViewModel(
    private val billingManager: BillingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PaywallUiState>(PaywallUiState.Loading)
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    private val _purchasedPack = MutableStateFlow<String?>(null)
    val purchasedPack: StateFlow<String?> = _purchasedPack.asStateFlow()

    private val _selectedProductId = MutableStateFlow<String?>(null)
    val selectedProductId: StateFlow<String?> = _selectedProductId.asStateFlow()

    // Cache to hold real details if available
    private var _cachedProducts: List<ProductInfo>? = null

    init {
        loadData()
        
        viewModelScope.launch {
            billingManager.purchaseState.collect { billingState ->
                when(billingState) {
                    is BillingManager.PurchaseState.Loading -> _uiState.value = PaywallUiState.Purchasing
                    is BillingManager.PurchaseState.Success -> {
                        _uiState.value = PaywallUiState.Success("Purchase Successful!")
                        _purchasedPack.value = billingState.productId
                    }
                    is BillingManager.PurchaseState.Error -> {
                        _uiState.value = PaywallUiState.Error(billingState.message)
                    }
                    is BillingManager.PurchaseState.Cancelled -> {
                         // Reset to products if cancelled
                         loadData() 
                    }
                    else -> {}
                }
            }
        }
        
        // Load current pack from prefs
        _purchasedPack.value = PreferencesManager.getPurchasedPack()
    }
    private fun loadData() {
        _uiState.value = PaywallUiState.Loading
        viewModelScope.launch {
            try {
                 val products = billingManager.queryProducts()
                 when (
                     val catalogState = BillingCatalogPolicy.evaluate(
                         isBillingReady = billingManager.billingConnectionReady.value,
                         returnedProductIds = products.mapTo(mutableSetOf()) { it.id },
                     )
                 ) {
                     BillingCatalogState.Ready -> {
                         _cachedProducts = products
                         val starter = products.find { it.id == BillingManager.PRODUCT_STARTER }
                         val gold = products.find { it.id == BillingManager.PRODUCT_GOLD }
                         val ultimate = products.find { it.id == BillingManager.PRODUCT_ULTIMATE }

                         _uiState.value = PaywallUiState.ProductsReady(
                             starter,
                             gold,
                             ultimate,
                         )
                         _selectedProductId.value = BillingManager.PRODUCT_GOLD
                     }
                     BillingCatalogState.WaitingForPlay -> {
                         _cachedProducts = null
                         _uiState.value = PaywallUiState.Error(
                             "Google Play Billing is not ready. Check Play Store and try again.",
                         )
                     }
                     is BillingCatalogState.Unavailable -> {
                         _cachedProducts = null
                         _uiState.value = PaywallUiState.Error(catalogState.message)
                     }
                 }
                 
            } catch (e: Exception) {
                _cachedProducts = null
                _uiState.value = PaywallUiState.Error(
                    "Google Play plans could not be loaded. Check Play Store and try again.",
                )
            }
        }
    }

    fun retryLoading() {
        loadData()
    }
    fun selectProduct(productId: String) {
        _selectedProductId.value = productId
    }

    fun purchaseSelected(activity: android.app.Activity) {
        val selectedId = _selectedProductId.value ?: return
        val details = _cachedProducts?.find { it.id == selectedId }
        
        if (details != null) {
            billingManager.launchPurchaseFlow(activity, details)
        } else {
             _uiState.value = PaywallUiState.Error(
                 "This plan was not returned by Google Play. Install the internal-testing build and try again.",
             )
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
             _uiState.value = PaywallUiState.Loading
             val result = billingManager.checkPremiumAccess()
             if (result) {
                 val pack = PreferencesManager.getPurchasedPack()
                 _purchasedPack.value = pack
                 _uiState.value = PaywallUiState.Success("Purchases Restored: $pack")
             } else {
                 _uiState.value = PaywallUiState.Error("No active purchases found")
                 loadData() // Go back to show products
             }
        }
    }
}
