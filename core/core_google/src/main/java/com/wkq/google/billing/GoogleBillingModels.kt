package com.wkq.google.billing

data class GoogleProduct(
    val productId: String,
    val productType: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val offerToken: String = ""
)

data class GooglePurchase(
    val products: List<String>,
    val purchaseToken: String,
    val purchaseTimeMillis: Long,
    val isAcknowledged: Boolean,
    val purchaseState: Int
)

data class GoogleBillingResponse(
    val isSuccess: Boolean,
    val responseCode: Int,
    val message: String = ""
)
