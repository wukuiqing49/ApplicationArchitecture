package com.wkq.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 用户信息实体类
 */
@Parcelize
data class UserInfo(
    val userId: String = "",
    val userName: String = "",
    val avatar: String = "",
    val phone: String = "",
    val gender: Int = 0, // 0: 未知, 1: 男, 2: 女
    val signature: String = "",
    val token: String = "",
    val isVip: Boolean = false,
    val level: Int = 1
) : Parcelable

/**
 * 判断用户信息是否有效 (核心字段校验)
 */
fun UserInfo.isValid(): Boolean {
    return userId.isNotEmpty()
}
