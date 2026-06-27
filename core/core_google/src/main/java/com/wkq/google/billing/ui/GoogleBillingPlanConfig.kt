package com.wkq.google.billing.ui

import com.wkq.google.billing.GoogleProductType
import java.io.Serializable

data class GoogleBillingPlanConfig(
    val planId: String,
    val productId: String,
    val productType: String,
    val title: String,
    val description: String,
    val basePlanId: String = "",
    val badge: String = "",
    val fallbackPrice: String = ""
) : Serializable {
    val billingProductId: String
        get() = if (productType == GoogleProductType.SUBS && basePlanId.isNotBlank()) {
            "$productId:$basePlanId"
        } else {
            productId
        }
}

data class GoogleBillingPageConfig(
    val plans: List<GoogleBillingPlanConfig>,
    val statusTitle: String = "",
    val statusDescription: String = "",
    val productsTitle: String = "",
    val productsDescription: String = "",
    val emptyText: String = "",
    val purchaseButtonText: String = "",
    val restoreButtonText: String = "",
    val redeemButtonText: String = "",
    val openRedeemPageOnClick: Boolean = false
) : Serializable
