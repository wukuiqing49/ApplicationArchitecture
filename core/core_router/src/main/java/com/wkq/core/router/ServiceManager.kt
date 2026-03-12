package com.wkq.core.router

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Service Provider 接口实现类管理者
 * 用于组件间接口的提供与获取
 */
object ServiceManager {
    // 采用 ConcurrentHashMap 保证多个模块异步注册服务时的线程安全性
    private val services = ConcurrentHashMap<Class<*>, Any>()

    fun <T : Any> register(clazz: Class<T>, impl: T) {
        services[clazz] = impl
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: Class<T>): T? {
        val service = services[clazz] as? T
        if (service == null) {
            Log.e("ServiceManager", "Service not found for: ${clazz.name}")
        }
        return service
    }
}
