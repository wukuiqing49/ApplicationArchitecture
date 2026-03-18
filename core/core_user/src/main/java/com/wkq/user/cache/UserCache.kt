package com.wkq.user.cache

import com.wkq.user.data.entity.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 用户内存缓存
 */
class UserCache {
    /**
     * 当前登录用户的 StateFlow
     */
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUserFlow: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    /**
     * 更新缓存
     */
    fun updateCache(user: UserEntity?) {
        _currentUser.value = user
    }

    /**
     * 获取缓存中的当前用户
     */
    fun getCurrentUser(): UserEntity? {
        return _currentUser.value
    }
    
    /**
     * 清空缓存
     */
    fun clear() {
        _currentUser.value = null
    }

    companion object {
        @Volatile
        private var INSTANCE: UserCache? = null

        fun getInstance(): UserCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserCache().also { INSTANCE = it }
            }
        }
    }
}
