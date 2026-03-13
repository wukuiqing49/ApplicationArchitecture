package com.wkq.data.util

import com.wkq.data.model.UserInfo
import com.wkq.data.model.isValid
import com.wkq.util.SpUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 用户信息管理类
 *
 * 设计特点：
 * 1. 双层存储：内存 (MutableStateFlow) + 本地 (MMKV/SpUtils)
 * 2. 响应式：UI 可通过 observeUserInfo() 订阅状态变更
 * 3. 线程安全：使用 volatile 确保多线程内存可见性
 */
object UserManager {

    private const val KEY_USER_INFO = "key_user_info"

    // 内存缓存 (使用 StateFlow 实现响应式)
    private val _userInfoFlow = MutableStateFlow<UserInfo?>(null)

    /**
     * 获取用户信息的可观察流
     */
    fun observeUserInfo(): StateFlow<UserInfo?> = _userInfoFlow.asStateFlow()

    /**
     * 当前内存中的用户信息
     */
    var currentUser: UserInfo?
        get() {
            val cached = _userInfoFlow.value
            // 💡 优化：不仅检查是否为 null，还要校验其核心字段是否有效
            // 防止内存不足或其他异常情况下，对象虽然存在但内容被清空的情况
            if (cached == null || !cached.isValid()) {
                synchronized(this) {
                    val doubleCheck = _userInfoFlow.value
                    if (doubleCheck == null || !doubleCheck.isValid()) {
                        val local = SpUtils.getParcelable<UserInfo>(KEY_USER_INFO)
                        if (local != null && local.isValid()) {
                            _userInfoFlow.value = local
                        } else if (doubleCheck != null) {
                            // 如果缓存的对象无效且本地也无有效数据，清理内存
                            _userInfoFlow.value = null
                        }
                    }
                }
            }
            return _userInfoFlow.value
        }
        private set(value) {
            _userInfoFlow.value = value
        }

    /**
     * 更新用户信息 (保存到内存 + 磁盘)
     */
    fun updateUserInfo(user: UserInfo?) {
        this.currentUser = user
        if (user != null) {
            SpUtils.put(KEY_USER_INFO, user)
        } else {
            SpUtils.remove(KEY_USER_INFO)
        }
    }

    /**
     * 清理用户信息 (退出登录调用)
     */
    fun clear() {
        updateUserInfo(null)
    }

    /**
     * 判断是否已登录
     */
    fun isLogin(): Boolean {
        return currentUser != null && !currentUser?.userId.isNullOrEmpty()
    }

    /**
     * 获取 Token (快速访问示例)
     */
    fun getToken(): String {
        return currentUser?.token ?: ""
    }
}