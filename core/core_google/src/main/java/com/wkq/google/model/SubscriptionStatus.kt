package com.wkq.google.model

data class SubscriptionStatus(
    val isActive: Boolean = false,
    val expireTimeMillis: Long = 0L,
    val planId: String = "",
    val source: String = ""
)
