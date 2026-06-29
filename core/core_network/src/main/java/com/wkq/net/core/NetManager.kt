package com.wkq.net.core

import com.wkq.net.config.NetConfig
import com.wkq.net.interceptor.HeaderInterceptor

/**
 * 高级网络框架的入口点和配置持有者。
 * 应用必须在初始化时调用 NetManager.init() 以正确配置框架。
 */
object NetManager {
    
    // 网络配置对象
    @Volatile
    private var config: NetConfig? = null
    
    @Volatile
    private var headerInterceptorInstance: HeaderInterceptor? = null

    /**
     * 全局请求头拦截器。
     */
    val headerInterceptor: HeaderInterceptor
        get() = headerInterceptorInstance
            ?: throw IllegalStateException(NetMessages.provider().headerInitRequired())

    /**
     * 使用自定义 NetConfig 配置初始化网络框架。
     * 如果多次调用，则记录日志并返回（防止重复初始化）。
     */
    @Synchronized
    fun init(netConfig: NetConfig) {
        tryInit(netConfig)
    }

    /**
     * 尝试初始化网络框架。
     *
     * @return true 表示本次完成初始化，false 表示此前已初始化。
     */
    @Synchronized
    fun tryInit(netConfig: NetConfig): Boolean {
        if (config != null) return false
        this.config = netConfig
        
        // 基于默认配置初始化全局 HeaderInterceptor
        headerInterceptorInstance = HeaderInterceptor(netConfig.defaultHeaders).apply {
            // 如果需要，可在此处添加进一步逻辑
        }
        return true
    }

    /**
     * 当前网络库是否已完成初始化。
     */
    fun isInitialized(): Boolean {
        return config != null
    }

    /**
     * 获取当前活动的配置。
     * 如果在 init() 之前使用，将抛出错误。
     */
    fun getConfig(): NetConfig {
        return config ?: throw IllegalStateException(NetMessages.provider().initRequired())
    }

    /**
     * 仅用于单元测试、Demo 环境切换或调试工具，不建议在线上业务流程中调用。
     */
    @Synchronized
    fun resetForTest() {
        config = null
        headerInterceptorInstance = null
        ApiRetrofit.reset()
        DownloadRetrofit.reset()
    }
}
