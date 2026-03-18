package com.wkq.user.repository

import com.wkq.user.cache.UserCache
import com.wkq.user.data.dao.UserDao
import com.wkq.user.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * 用户数据仓库
 */
class UserRepository(private val userDao: UserDao, private val userCache: UserCache) {

    /**
     * 获取当前用户的 Flow，同步更新缓存
     */
    fun getCurrentUserFlow(): Flow<UserEntity?> {
        return userDao.getCurrentUserFlow().onEach { user ->
            userCache.updateCache(user)
        }
    }

    /**
     * 获取所有账户
     */
    fun getAllUsersFlow(): Flow<List<UserEntity>> = userDao.getAllUsersFlow()

    /**
     * 登录或更新用户信息
     */
    suspend fun saveUser(user: UserEntity) {
        if (user.isCurrent) {
            // 如果设置为当前用户，先清除其他用户的当前状态
            userDao.clearCurrentStatus()
        }
        userDao.insertOrUpdate(user)
        // 同步内存缓存
        if (user.isCurrent) {
            userCache.updateCache(user)
        } else if (userCache.getCurrentUser()?.userId == user.userId) {
            // 如果原本是当前用户但现在被设为非当前，清除缓存
            userCache.clear()
        }
    }

    /**
     * 切换账户
     */
    suspend fun switchAccount(userId: String) {
        userDao.switchAccount(userId)
        val user = userDao.getUserById(userId)
        userCache.updateCache(user)
    }

    /**
     * 刷新内存缓存（从数据库同步）
     */
    suspend fun refreshCache() {
        val user = userDao.getCurrentUser()
        userCache.updateCache(user)
    }

    /**
     * 退出登录（删除用户或清除 isCurrent 状态）
     */
    suspend fun logout(userId: String) {
        val user = userDao.getUserById(userId)
        if (user != null) {
            userDao.insertOrUpdate(user.copy(isCurrent = false))
            if (userCache.getCurrentUser()?.userId == userId) {
                userCache.clear()
            }
        }
    }
}
