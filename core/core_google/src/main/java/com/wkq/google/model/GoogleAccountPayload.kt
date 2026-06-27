package com.wkq.google.model

data class GoogleAccountPayload(
    val userId: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String,
    val idToken: String,
    val subject: String?
)
