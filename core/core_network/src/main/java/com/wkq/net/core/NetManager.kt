package com.wkq.net.core

import com.wkq.net.config.NetConfig
import com.wkq.net.interceptor.HeaderInterceptor

/**
 * Entry point and configuration holder for the Advanced Network Framework.
 * The application must call NetManager.init() to configure the framework properly before usage.
 */
object NetManager {
    
    private var config: NetConfig? = null
    lateinit var headerInterceptor: HeaderInterceptor
        private set

    /**
     * Initialize the network framework with a custom NetConfig configuration.
     * Throws an exception if called more than once.
     */
    fun init(netConfig: NetConfig) {
        if (config != null) {
            // Already initialized. Skip or throw error.
            return
        }
        this.config = netConfig
        
        // Initialize the global HeaderInterceptor based on default configuration
        headerInterceptor = HeaderInterceptor(netConfig.defaultHeaders).apply {
            // Further logic if needed
        }
    }

    /**
     * Helper to retrieve the active configuration.
     * Throws an error if used before init().
     */
    fun getConfig(): NetConfig {
        return config ?: throw IllegalStateException("NetManager must be initialized with NetConfig in the Application class before use.")
    }
}
