package com.wkq.user.manager

import android.content.Context
import com.wkq.user.cache.UserCache
import com.wkq.user.data.db.UserDatabase
import com.wkq.user.data.entity.UserEntity
import com.wkq.user.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 用户管理单例类（外部访问唯一入口）
 */
class UserManager private constructor(context: Context) {

    private val database = UserDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val userCache = UserCache.getInstance()
    private val repository = UserRepository(userDao, userCache)
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 当前用户的 StateFlow (内存缓存)
     */
    val currentUserFlow: StateFlow<UserEntity?> = userCache.currentUserFlow

    /**
     * 快速获取当前内存中的用户
     */
    val currentUser: UserEntity? get() = userCache.getCurrentUser()

    /**
     * 获取所有账户列表 Flow
     */
    val allUsersFlow: Flow<List<UserEntity>> = repository.getAllUsersFlow()

    init {
        // 初始化时从数据库加载当前用户到缓存
        scope.launch {
            repository.refreshCache()
        }
    }

    /**
     * 保存或更新用户
     */
    fun saveUser(user: UserEntity) {
        scope.launch {
            repository.saveUser(user)
        }
    }

    /**
     * 切换账号
     */
    fun switchAccount(userId: String) {
        scope.launch {
            repository.switchAccount(userId)
        }
    }

    /**
     * 退出登录
     */
    fun logout(userId: String) {
        scope.launch {
            repository.logout(userId)
        }
    }

    private val gson = com.google.gson.Gson()

    /**
     * 获取用户扩展信息 (示例代码)
     */
    fun <T> getExtraData(clazz: Class<T>): T? {
        val json = currentUser?.extraJson ?: return null
        return try {
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: UserManager? = null

        /**
         * 初始化接口（建议在 Application 中调用）
         */
        fun init(context: Context): UserManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserManager(context).also { INSTANCE = it }
            }
        }

        /**
         * 获取单例
         */
        fun getInstance(): UserManager {
            return INSTANCE ?: throw IllegalStateException("UserManager must be initialized first. Call init(context) in Application.")
        }
    }
}
