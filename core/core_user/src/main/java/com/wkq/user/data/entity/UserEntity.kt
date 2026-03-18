package com.wkq.user.data.entity

import androidx.room.*

/**
 * 用户实体类
 */
@Entity(tableName = "user_accounts")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    
    val userName: String?="",
    
    val avatar: String? = "",
    
    /**
     * 是否为当前激活账号
     */
    val isCurrent: Boolean = false,
    
    /**
     * 扩展字段（JSON 格式）
     */
    val extraJson: String? = null,
    
    /**
     * 过期时间（毫秒）
     */
    val expireTime: Long? = null,
    
    /**
     * 最后活跃时间（毫秒）
     */
    val lastActiveTime: Long? = System.currentTimeMillis()
)
