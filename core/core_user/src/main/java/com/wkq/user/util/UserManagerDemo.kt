package com.wkq.user.util

import android.util.Log
import com.wkq.user.data.entity.UserEntity
import com.wkq.user.manager.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * UserManager 调用示例
 * 
 * 展示了如何在不同场景下调用 UserManager 的公开 API。
 */
object UserManagerDemo {
    private const val TAG = "UserManagerDemo"
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * 1. 响应式监听当前用户变化 (推荐)
     */
    fun observeUser() {
        scope.launch {
            UserManager.getInstance().currentUserFlow.collectLatest { user ->
                if (user != null) {
                    Log.d(TAG, "当前用户更新: ${user.userName}")
                } else {
                    Log.d(TAG, "当前未登录")
                }
            }
        }
    }

    /**
     * 2. 在协程中同步安全地获取当前用户 (挂起函数)
     */
    suspend fun fetchUser() {
        val user = UserManager.getInstance().getSafeCurrentUser()
        Log.d(TAG, "获取到最新用户: ${user?.userName}")
    }

    /**
     * 3. 使用回调方式获取当前用户 (传统异步方式)
     */
    fun getUserAsync() {
        UserManager.getInstance().getUserAsync { user ->
            Log.d(TAG, "异步回调获取用户: ${user?.userName}")
        }
    }

    /**
     * 4. 获取用户自定义扩展数据 (自动解析 JSON 并缓存)
     */
    fun showExtraData() {
        // 假设 UserEntity 中的 extraJson 存储了 UserSettings 的 JSON
        data class UserSettings(val theme: String = "dark", val language: String = "zh")
        
        val settings = UserManager.getInstance().getExtraData(UserSettings::class.java)
        Log.d(TAG, "用户设置: 主题=${settings?.theme}, 语言=${settings?.language}")
    }

    /**
     * 5. 执行账号操作 (保存、切换、退出)
     */
    fun accountOperations() {
        val userManager = UserManager.getInstance()
        
        // 保存/更新用户
        val newUser = UserEntity(userId = "1001", userName = "张三", isCurrent = true)
        userManager.saveUser(newUser)
        
        // 切换账号
        // userManager.switchAccount("1002")
        
        // 退出登录 (清理指定用户状态)
        // userManager.logout("1001")
    }
}
