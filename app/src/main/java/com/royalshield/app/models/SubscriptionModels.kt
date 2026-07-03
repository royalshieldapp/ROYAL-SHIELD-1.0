package com.royalshield.app.models

import com.android.billingclient.api.ProductDetails

data class ProductInfo(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val originalDetails: ProductDetails? = null
)
