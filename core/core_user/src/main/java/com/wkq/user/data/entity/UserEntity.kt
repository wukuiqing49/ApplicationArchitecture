package com.wkq.user.data.entity

import androidx.room.*

/**
 * UserEntity：用户账号实体类
 */
@Entity(tableName = "user_accounts")
data class UserEntity(
    @PrimaryKey
    val userId: String, // 用户 ID (唯一标识)
    
    val userName: String?="", // 用户名
    
    val avatar: String? = "", // 头像地址
    
    /**
     * 是否为当前激活状态的账号
     */
    val isCurrent: Boolean = false,
    
    /**
     * 扩展字段 (JSON 字符串)，用于存储非结构化的额外信息
     */
    val extraJson: String? = null,
    
    /**
     * 账号过期时间（毫秒时间戳）
     */
    val expireTime: Long? = null,
    
    /**
     * 最后一次活跃时间（毫秒时间戳）
     */
    val lastActiveTime: Long? = System.currentTimeMillis()
)
