package com.wkq.user.data.entity

import androidx.room.*

/**
 * UserEntity：用户账号实体类
 */
@Entity(tableName = "user_accounts")
data class UserEntity(

    @PrimaryKey
    val userId: String,

    // 基础
    val userName: String = "",
    val nickName: String = "",
    val avatar: String = "",

    val gender: Int = 0,
    val birthday: Long = 0L,
    val country: String = "",
    val city: String = "",

    // 登录核心
    val token: String = "",
    val refreshToken: String = "",
    val loginType: Int = 0,

    val bindPhone: String = "",
    val bindEmail: String = "",

    // 状态
    val isVip: Boolean = false,
    val vipExpireTime: Long = 0L,

    val level: Int = 0,
    val exp: Long = 0L,

    val status: Int = 0,

    // 统计
    val followCount: Int = 0,
    val fansCount: Int = 0,
    val likeCount: Int = 0,
    val postCount: Int = 0,

    // 当前账号标记
    val isCurrent: Boolean = false,

    // 时间
    val lastActiveTime: Long = 0L,
    val lastLoginTime: Long = 0L,

    // 扩展
    val extraJson: String? = null
)


