package com.wkq.user.manager

import android.content.Context
import com.google.gson.Gson
import com.wkq.user.cache.UserCache
import com.wkq.user.data.db.UserDatabase
import com.wkq.user.data.entity.UserEntity
import com.wkq.user.repository.UserRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * UserManager：用户管理核心单例类
 * 
 * 职责：
 * 1. 维护当前登录用户和所有用户的响应式状态 (StateFlow)。
 * 2. 提供线程安全的同步/异步接口进行用户登录、切换、退出。
 * 3. 自动同步底层数据库变化到内存内存 Flow。
 * 4. 支持用户自定义扩展数据的 JSON 解析与缓存。
 * 
 * 优化点：
 * - 线程安全：使用 ConcurrentHashMap 处理扩展数据缓存。
 * - 性能优化：通过 stateIn 将冷流转成热流，并在内存中共享数据。
 * - 并发保护：使用 Mutex 解决冷启动时的“缓存击穿”读库问题。
 */
class UserManager private constructor(context: Context) {

    // ------------------ 内部依赖 ------------------

    private val database = UserDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val userCache = UserCache.getInstance()
    private val repository = UserRepository(userDao, userCache)
    
    private val gson = Gson()
    
    /** 并发锁：确保多线程下 getSafeCurrentUser() 仅触发一次查库 */
    private val mutex = Mutex()

    /** 全局协程处理器，防止异常导致 Scope 崩溃 */
    private val handler = CoroutineExceptionHandler { _, throwable -> throwable.printStackTrace() }
    
    /** 单例作用域：使用 SupervisorJob 确保子协程互不影响 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)

    /** 扩展数据并发缓存：Clazz Name -> 解析后的实体 */
    private val extraDataCache = ConcurrentHashMap<String, Any?>()

    // ------------------ 对外公开 Flow ------------------

    /** 
     * 当前用户 Flow (StateFlow)
     * 每次用户更新时会自动清空扩展数据缓存
     */
    val currentUserFlow: StateFlow<UserEntity?> = repository.getCurrentUserFlow()
        .onEach { extraDataCache.clear() } // 用户变更，清理对应的扩展缓存
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** 所有用户列表 Flow (StateFlow) */
    val allUsersFlow: StateFlow<List<UserEntity>> = repository.getAllUsersFlow()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    // ------------------ 核心功能接口 ------------------

    /** 
     * 挂起函数：获取当前用户（带并发保护）
     * 优先从内存 Flow 获取，若无则持有锁进行数据库加载
     */
    suspend fun getSafeCurrentUser(): UserEntity? {
        currentUserFlow.value?.let { return it }
        return mutex.withLock {
            // 双重校验，解决“并发缓存击穿”问题
            currentUserFlow.value ?: repository.loadCurrentUser()
        }
    }

    /** 
     * 挂起函数：获取所有用户列表
     * 优先从内存 Flow 获取，若为空则从数据库初次加载
     */
    suspend fun getAllUsers(): List<UserEntity> =
        allUsersFlow.value.takeIf { it.isNotEmpty() } ?: repository.getAllUsersFlow().first()

    /** 回调函数：获取当前用户（主线程安全回调） */
    fun getUserAsync(onResult: (UserEntity?) -> Unit) =
        runAsyncMain({ getSafeCurrentUser() }, onResult)

    /** 回调函数：获取用户列表（主线程安全回调） */
    fun getAllUsersAsync(onResult: (List<UserEntity>) -> Unit) =
        runAsyncMain({ getAllUsers() }, onResult)

    /** 保存或更新用户（异步执行） */
    fun saveUser(user: UserEntity) {
        scope.launch(Dispatchers.IO) { repository.saveUser(user) }
    }

    /** 切换账号（异步执行） */
    fun switchAccount(userId: String) {
        scope.launch(Dispatchers.IO) { repository.switchAccount(userId) }
    }

    /** 退出登录（异步执行） */
    fun logout(userId: String) {
        scope.launch(Dispatchers.IO) { repository.logout(userId) }
    }

    // ------------------ 扩展数据接口 ------------------

    /**
     * 获取当前用户扩展信息（泛型解析 + 内存缓存）
     * @param clazz 目标解析类型
     * 注意：当前用户变更时会自动清理该缓存
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getExtraData(clazz: Class<T>): T? {
        val key = clazz.name
        // 1. 命中内存缓存直接返回
        extraDataCache[key]?.let { return it as T? }

        // 2. 无缓存则从 UserEntity.extraJson 进行解析
        val json = currentUserFlow.value?.extraJson ?: return null
        val parsed = try { gson.fromJson(json, clazz) } catch (e: Exception) { null }
        
        // 3. 写入缓存提高下次读取效率
        if (parsed != null) {
            extraDataCache[key] = parsed
        }
        return parsed
    }

    /** 刷新内存缓存（手动从数据库同步一次最新状态） */
    fun refreshCache() {
        scope.launch(Dispatchers.IO) { repository.refreshCache() }
    }

    /** 清理资源（通常仅在 Application 结束时调用） */
    fun clear() {
        scope.cancel()
    }

    // ------------------ 内部工具方法 ------------------

    /** 辅助方法：将协程任务结果分发到主线程 */
    private fun <T> runAsyncMain(block: suspend () -> T, onResult: (T) -> Unit) {
        scope.launch {
            val result = block()
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    // ------------------ 单例管理 ------------------

    companion object {
        @Volatile
        private var INSTANCE: UserManager? = null

        /** 模块初始化（应在 Application.onCreate 中调用） */
        fun init(context: Context): UserManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserManager(context).also { INSTANCE = it }
            }

        /** 获取单例对象 */
        fun getInstance(): UserManager =
            INSTANCE ?: throw IllegalStateException(
                "UserManager must be initialized first. Call init(context) in Application."
            )
    }
}