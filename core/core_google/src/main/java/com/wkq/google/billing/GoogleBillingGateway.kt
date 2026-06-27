package com.wkq.google.billing

import com.wkq.google.model.SubscriptionStatus

interface GoogleBillingGateway {
    suspend fun queryEntitlement(): GoogleBillingEntitlement

    suspend fun querySubscriptionStatus(userId: String): SubscriptionStatus
}
