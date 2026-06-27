package com.wkq.google.billing

import com.wkq.google.GoogleKit

object GoogleBillingStatusHelper {

    suspend fun queryEntitlement(): GoogleBillingEntitlement {
        return GoogleKit.billing.queryEntitlement()
    }

    suspend fun hasActiveProduct(productId: String): Boolean {
        if (productId.isBlank()) return false
        val entitlement = queryEntitlement()
        val cleanProductId = productId.substringBefore(":")
        return entitlement.activeSubscriptionIds.contains(cleanProductId) ||
            entitlement.ownedOneTimeProductIds.contains(cleanProductId)
    }

    suspend fun queryActiveSubscriptionIds(): List<String> {
        return queryEntitlement().activeSubscriptionIds
    }

    suspend fun queryOwnedOneTimeProductIds(): List<String> {
        return queryEntitlement().ownedOneTimeProductIds
    }
}
