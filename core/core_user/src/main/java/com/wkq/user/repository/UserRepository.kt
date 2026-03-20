package com.wkq.user.repository

import com.wkq.user.cache.UserCache
import com.wkq.user.data.dao.UserDao
import com.wkq.user.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * UserRepository：用户数据仓库
 * 
 * 职责：
 * 1. 封装底层数据源 (Room DAO) 和内存缓存 (UserCache)。
 * 2. 确保数据库更新时，内存缓存能同步得到刷新。
 * 3. 提供统一的业务接口供上层 UserManager 调用。
 */
internal class UserRepository(
    private val userDao: UserDao, 
    private val userCache: UserCache
) {

    /**
     * 获取当前用户的 Flow
     * 内部使用 onEach 观察数据流变化，自动同步更新到 UserCache
     */
    fun getCurrentUserFlow(): Flow<UserEntity?> {
        return userDao.getCurrentUserFlow().onEach { user ->
            userCache.updateCache(user)
        }
    }

    /**
     * 获取所有账户信息的 Flow (直接来自数据库)
     */
    fun getAllUsersFlow(): Flow<List<UserEntity>> = userDao.getAllUsersFlow()

    /**
     * 登录或更新用户信息
     * @param user 用户实体，若 isCurrent 为 true，则会清除数据库中其他账号的当前激活状态
     */
    suspend fun saveUser(user: UserEntity) {
        if (user.isCurrent) {
            // 如果设置为当前用户，先通过 DAO 清除其他用户的激活标志
            userDao.clearCurrentStatus()
        }
        userDao.insertOrUpdate(user)
        
        // 同步更新内存缓存
        if (user.isCurrent) {
            userCache.updateCache(user)
        } else if (userCache.getCurrentUser()?.userId == user.userId) {
            // 如果原本是当前用户但现在被设为非激活，则清除缓存
            userCache.clear()
        }
    }

    /**
     * 切换账户
     * @param userId 目标用户 ID
     */
    suspend fun switchAccount(userId: String) {
        userDao.switchAccount(userId)
        val user = userDao.getUserById(userId)
        userCache.updateCache(user)
    }

    /**
     * 手动刷新内存缓存（从数据库拉取最新数据）
     */
    suspend fun refreshCache() {
        val user = userDao.getCurrentUser()
        userCache.updateCache(user)
    }

    /**
     * 退出登录
     * @param userId 退出登录的用户 ID，将 mark 为非激活状态
     */
    suspend fun logout(userId: String) {
        val user = userDao.getUserById(userId)
        if (user != null) {
            userDao.insertOrUpdate(user.copy(isCurrent = false))
            // 如果退出的正好是当前缓存的用户，则同步清理内存
            if (userCache.getCurrentUser()?.userId == userId) {
                userCache.clear()
            }
        }
    }
    
    /**
     * 同步加载当前用户（查库）并刷新缓存
     */
    suspend fun loadCurrentUser(): UserEntity? {
        val user = userDao.getCurrentUser()
        userCache.updateCache(user)
        return user
    }

}
