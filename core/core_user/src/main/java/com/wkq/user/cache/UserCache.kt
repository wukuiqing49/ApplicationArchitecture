package com.wkq.user.cache

import com.wkq.user.data.entity.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UserCache：用户内存缓存类
 * 
 * 功能：
 * 使用 StateFlow 在内存中持有当前登录用户的实时状态，供 UI 层进行响应式监听。
 */
internal class UserCache {
    /**
     * 当前登录用户的 MutableStateFlow (私有)
     */
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    
    /**
     * 对外公开的只读 StateFlow
     */
    val currentUserFlow: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    /**
     * 更新缓存值
     */
    fun updateCache(user: UserEntity?) {
        _currentUser.value = user
    }

    /**
     * 同步获取缓存中的当前用户对象
     */
    fun getCurrentUser(): UserEntity? {
        return _currentUser.value
    }
    
    /**
     * 清空当前用户缓存
     */
    fun clear() {
        _currentUser.value = null
    }

    companion object {
        @Volatile
        private var INSTANCE: UserCache? = null

        /** 传统的双重校验锁单例获取方式 */
        fun getInstance(): UserCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserCache().also { INSTANCE = it }
            }
        }
    }
}
