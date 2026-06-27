package com.wkq.google.model

import com.google.gson.annotations.SerializedName

/**
 * Google ID Token 的 Payload 部分所对应的 Claims 模型，用于 Gson 反序列化。
 */
data class GoogleIdTokenClaims(
    @SerializedName("sub") val sub: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("picture") val picture: String? = null
) {
    val subject: String?
        get() = sub
}
