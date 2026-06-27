package com.wkq.google.billing

import com.wkq.google.model.SubscriptionStatus

object NoOpGoogleBillingGateway : GoogleBillingGateway {
    override suspend fun queryEntitlement(): GoogleBillingEntitlement {
        return GoogleBillingEntitlement(source = "none")
    }

    override suspend fun querySubscriptionStatus(userId: String): SubscriptionStatus {
        return SubscriptionStatus(source = "none")
    }
}
