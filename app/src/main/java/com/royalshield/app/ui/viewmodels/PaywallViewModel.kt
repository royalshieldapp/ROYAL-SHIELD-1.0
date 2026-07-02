package com.royalshield.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.models.ProductInfo
import com.royalshield.app.managers.BillingManager
import com.royalshield.app.managers.PreferencesManager
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
    private fun applyDiscountToPriceString(priceStr: String): String {
        val numberRegex = """\d+([.,]\d+)?""".toRegex()
        val match = numberRegex.find(priceStr) ?: return priceStr
        val numericStr = match.value.replace(",", ".")
        val originalPrice = numericStr.toDoubleOrNull() ?: return priceStr
        val discountedPrice = originalPrice * 0.90
        val formattedPrice = String.format(java.util.Locale.US, "%.2f", discountedPrice)
        return priceStr.replace(match.value, formattedPrice)
    }

    private fun applyDiscountIfEligible(product: ProductInfo?): ProductInfo? {
        if (product == null) return null
        if (!PreferencesManager.isDiscountApplied()) return product
        val discountedPrice = applyDiscountToPriceString(product.price)
        return product.copy(price = discountedPrice)
    }

    private fun loadData() {
        _uiState.value = PaywallUiState.Loading
        viewModelScope.launch {
            try {
                 // Use a timeout to prevent hanging on emulator if Billing Store is not responsive
                 val products = kotlinx.coroutines.withTimeout(3000L) {
                     billingManager.queryProducts()
                 }
                 _cachedProducts = products
                 
                 val starter = products?.find { it.id == BillingManager.PRODUCT_STARTER }
                 val gold = products?.find { it.id == BillingManager.PRODUCT_GOLD }
                 val ultimate = products?.find { it.id == BillingManager.PRODUCT_ULTIMATE }
                 
                 _uiState.value = PaywallUiState.ProductsReady(
                     applyDiscountIfEligible(starter),
                     applyDiscountIfEligible(gold),
                     applyDiscountIfEligible(ultimate)
                 )
                 
                 // Default selection ID
                 _selectedProductId.value = BillingManager.PRODUCT_GOLD
                 
            } catch (e: Exception) {
                // Fallback for Emulator/Timeout
                val starter = ProductInfo(BillingManager.PRODUCT_STARTER, "Starter Pack", "Basic Protection", "$9.99")
                val gold = ProductInfo(BillingManager.PRODUCT_GOLD, "Gold Bundle", "Premium Features", "$19.99")
                val ultimate = ProductInfo(BillingManager.PRODUCT_ULTIMATE, "Ultimate Pack", "All Access", "$39.49")
                
                val fallbackList = listOf(starter, gold, ultimate)
                _cachedProducts = fallbackList
                _uiState.value = PaywallUiState.ProductsReady(
                    applyDiscountIfEligible(starter),
                    applyDiscountIfEligible(gold),
                    applyDiscountIfEligible(ultimate)
                )
                _selectedProductId.value = BillingManager.PRODUCT_GOLD
            }
        }
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
             // Mock/Error flow - "Billing not ready or Product not found"
             _uiState.value = PaywallUiState.Error("Product unavailable in Store (Dev Mode)")
              // Optional: In strict dev mode, maybe simulate success?
              // But preventing silent failure is better.
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
