package com.wkq.app

import android.app.Application
import com.wkq.app.BuildConfig
import com.wkq.app.base.BaseApplication
import com.wkq.common.web.util.WebPreLoader
import com.wkq.net.config.NetConfig
import com.wkq.net.core.NetManager
import com.wkq.router.api.Router
import com.wkq.user.manager.UserManager
import com.wkq.util.ProcessUtils
import com.wkq.util.SpUtils
import com.wkq.util.coil.CacheManager

/**
 * 全局 Application
 * 负责初始化全局组件（如 SpUtils）
 */
class MyApplication : BaseApplication() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // --- 所有进程通用初始化 ---
        // 初始化 MMKV 本地存储 (支持多进程)
        SpUtils.init(this)
        // 初始化路由 (确保子进程也能跨模块跳转)
        Router.init(this)
        val config = NetConfig.Builder()
            .setBaseUrl("https://api.example.com/")
            .putBaseUrl("google", "https://google.com/")
            .putBaseUrl("github", "https://api.github.com/")
            .setConnectTimeout(15L)
            .setReadTimeout(20L)
            .setWriteTimeout(20L)
            .setDebugLogsEnabled(true)
            .setAllowUnsafeSsl(BuildConfig.DEBUG)
            .addDefaultHeader("Global-Version", "1.0.0")
            .build()

        NetManager.init(config)
        // --- 仅主进程初始化 ---
        if (ProcessUtils.isMainProcess(this)) {
            // 初始化图片加载缓存 (仅主进程管理)
            CacheManager.init(this)
            // 初始化用户管理模块 (主进程维护状态)
            UserManager.init(this)
            // 只有主进程负责预热子进程，避免递归
            WebPreLoader.preWarm(this)
        }
    }

    companion object {
        lateinit var instance: MyApplication
            private set
    }
}
