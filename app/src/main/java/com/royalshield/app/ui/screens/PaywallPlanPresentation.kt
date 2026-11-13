package com.royalshield.app.ui.screens

import com.royalshield.app.managers.BillingManager
import com.royalshield.app.models.ProductInfo

enum class PaywallPlanStyle {
    STARTER,
    GOLDEN,
    ULTIMATE,
}

data class PaywallPlanPresentation(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val purchaseLabel: String = "one-time purchase",
    val style: PaywallPlanStyle,
    val mostPopular: Boolean = false,
)

fun toPaywallPlanPresentations(products: List<ProductInfo>): List<PaywallPlanPresentation> {
    val byId = products.associateBy(ProductInfo::id)
    return listOfNotNull(
        byId[BillingManager.PRODUCT_STARTER]?.let {
            PaywallPlanPresentation(
                productId = it.id,
                title = "STARTER",
                description = "Essential protection",
                formattedPrice = it.price,
                style = PaywallPlanStyle.STARTER,
            )
        },
        byId[BillingManager.PRODUCT_GOLD]?.let {
            PaywallPlanPresentation(
                productId = it.id,
                title = "GOLDEN",
                description = "Advanced protection",
                formattedPrice = it.price,
                style = PaywallPlanStyle.GOLDEN,
                mostPopular = true,
            )
        },
        byId[BillingManager.PRODUCT_ULTIMATE]?.let {
            PaywallPlanPresentation(
                productId = it.id,
                title = "ULTIMATE",
                description = "Complete protection",
                formattedPrice = it.price,
                style = PaywallPlanStyle.ULTIMATE,
            )
        },
    )
}

fun isPaywallContinueEnabled(
    selectedProductId: String?,
    purchasing: Boolean,
    plans: List<PaywallPlanPresentation>,
): Boolean = !purchasing && plans.any { it.productId == selectedProductId }
