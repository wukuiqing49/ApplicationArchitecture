package com.wkq.google.billing

data class GoogleBillingCatalog(
    val subscriptions: List<GoogleProduct> = emptyList(),
    val oneTimeProducts: List<GoogleProduct> = emptyList()
) {
    val isEmpty: Boolean
        get() = subscriptions.isEmpty() && oneTimeProducts.isEmpty()
}

data class GoogleBillingEntitlement(
    val isPro: Boolean = false,
    val hasSubscription: Boolean = false,
    val hasLifetimeUnlock: Boolean = false,
    val activeSubscriptionIds: List<String> = emptyList(),
    val ownedOneTimeProductIds: List<String> = emptyList(),
    val source: String = "google_play"
) {
    val activePlanId: String
        get() = activeSubscriptionIds.firstOrNull()
            ?: ownedOneTimeProductIds.firstOrNull()
            .orEmpty()
}
