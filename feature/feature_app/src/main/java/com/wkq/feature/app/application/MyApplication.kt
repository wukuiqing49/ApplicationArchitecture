package com.wkq.feature.app.application

import android.app.Application
import com.wkq.util.SpUtils
import com.wkq.util.ThemeManager
import com.wkq.util.coil.CacheManager


/**
 * 全局 Application
 * 负责初始化全局组件（如 SpUtils）
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        // 初始化 MMKV 本地存储
        SpUtils.init(this)
        // 初始化图片加载缓存
        CacheManager.init(this)
        // 初始化主题
        ThemeManager.init(this)
        
        // 初始化路由 (自动生成的 RouterInit 位于 app 模块)
        try {
            val clazz = Class.forName("com.qianrun.voice.core.router.RouterInit")
            val instance = clazz.getField("INSTANCE").get(null)
            val method = clazz.getMethod("registerAll")
            method.invoke(instance)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        lateinit var instance: MyApplication
            private set
    }
}
