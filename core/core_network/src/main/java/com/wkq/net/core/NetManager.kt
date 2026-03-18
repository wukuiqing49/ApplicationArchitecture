package com.wkq.net.core

import com.wkq.net.config.NetConfig
import com.wkq.net.interceptor.HeaderInterceptor

/**
 * 高级网络框架的入口点和配置持有者。
 * 应用必须在初始化时调用 NetManager.init() 以正确配置框架。
 */
object NetManager {
    
    // 网络配置对象
    private var config: NetConfig? = null
    
    // 全局请求头拦截器
    lateinit var headerInterceptor: HeaderInterceptor
        private set

    /**
     * 使用自定义 NetConfig 配置初始化网络框架。
     * 如果多次调用，则记录日志并返回（防止重复初始化）。
     */
    fun init(netConfig: NetConfig) {
        if (config != null) {
            // 已初始化。跳过或抛出错误。
            return
        }
        this.config = netConfig
        
        // 基于默认配置初始化全局 HeaderInterceptor
        headerInterceptor = HeaderInterceptor(netConfig.defaultHeaders).apply {
            // 如果需要，可在此处添加进一步逻辑
        }
    }

    /**
     * 获取当前活动的配置。
     * 如果在 init() 之前使用，将抛出错误。
     */
    fun getConfig(): NetConfig {
        return config ?: throw IllegalStateException("在 Application 中使用网络框架前，必须先调用 NetManager.init() 初始化配置。")
    }
}
