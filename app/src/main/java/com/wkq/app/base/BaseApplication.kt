package com.wkq.app.base

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.wkq.util.log.ALog

/**
 * Application 基类
 * 负责全局组件初始化及页面生命周期监听
 */
open class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // 1. 初始化 ALog
        initALog()
        
        // 2. 注册页面生命周期监听
        registerLifecycleCallbacks()
    }

    /**
     * 初始化日志工具
     */
    private fun initALog() {
        // 根据 ALog.kt 的定义进行初始化
        // 参数依次为：context, isShow (是否打印控制台), showStackInfo, enableFile, logFilePrefix, maxFileSize, cacheDays
        ALog.init(this, isShow = true, showStackInfo = true, enableFile = true)
        ALog.d("BaseApplication", "ALog 初始化成功")
    }

    /**
     * 注册 Activity 生命周期回调
     */
    private fun registerLifecycleCallbacks() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                ALog.d("Lifecycle", "创建: ${activity.javaClass.simpleName}")
            }

            override fun onActivityPostResumed(activity: Activity) {
                super.onActivityPostResumed(activity)
                ALog.d("Lifecycle", "onActivityPostResumed: ${activity.javaClass.simpleName}")
            }


            override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
                super.onActivityPostCreated(activity, savedInstanceState)
                ALog.d("Lifecycle", "onActivityPostCreated: ${activity.javaClass.simpleName}")
            }

            override fun onActivityStarted(activity: Activity) {
                ALog.d("Lifecycle", "onActivityStarted: ${activity.javaClass.simpleName}")
            }

            override fun onActivityResumed(activity: Activity) {
                ALog.d("Lifecycle", "onActivityResumed: ${activity.javaClass.simpleName}")
            }

            override fun onActivityPaused(activity: Activity) {
                ALog.d("Lifecycle", "onActivityPaused: ${activity.javaClass.simpleName}")
            }

            override fun onActivityStopped(activity: Activity) {
                ALog.d("Lifecycle", "onActivityStopped: ${activity.javaClass.simpleName}")
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                ALog.d("Lifecycle", "onActivitySaveInstanceState: ${activity.javaClass.simpleName}")
            }

            override fun onActivityDestroyed(activity: Activity) {
                ALog.d("Lifecycle", "onActivityDestroyed: ${activity.javaClass.simpleName}")
            }
        })
    }
}
