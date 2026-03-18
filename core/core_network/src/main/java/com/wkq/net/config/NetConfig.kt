package com.wkq.net.config

import com.wkq.net.core.GlobalNetHandler
import java.util.concurrent.ConcurrentHashMap

/**
 * 高级网络框架配置类。
 * 旨在由 Application 类在初始化期间传递。
 */
class NetConfig private constructor(
    val baseUrl: String,
    val connectTimeout: Long,
    val readTimeout: Long,
    val writeTimeout: Long,
    val defaultHeaders: Map<String, String>,
    val isDebugLogsEnabled: Boolean,
    val globalHandler: GlobalNetHandler? = null
) {
    class Builder {
        private var baseUrl: String = ""
        private var connectTimeout: Long = 15L
        private var readTimeout: Long = 20L
        private var writeTimeout: Long = 20L
        private var defaultHeaders = ConcurrentHashMap<String, String>()
        private var isDebugLogsEnabled: Boolean = false
        private var globalHandler: GlobalNetHandler? = null

        fun setBaseUrl(url: String) = apply { this.baseUrl = url }
        fun setConnectTimeout(seconds: Long) = apply { this.connectTimeout = seconds }
        fun setReadTimeout(seconds: Long) = apply { this.readTimeout = seconds }
        fun setWriteTimeout(seconds: Long) = apply { this.writeTimeout = seconds }
        fun addDefaultHeader(key: String, value: String) = apply { this.defaultHeaders[key] = value }
        fun setDebugLogsEnabled(enabled: Boolean) = apply { this.isDebugLogsEnabled = enabled }
        fun setGlobalHandler(handler: GlobalNetHandler) = apply { this.globalHandler = handler }

        fun build(): NetConfig {
            require(baseUrl.isNotEmpty()) { "NetConfig 中的 Base URL 不能为空" }
            return NetConfig(
                baseUrl = baseUrl,
                connectTimeout = connectTimeout,
                readTimeout = readTimeout,
                writeTimeout = writeTimeout,
                defaultHeaders = defaultHeaders,
                isDebugLogsEnabled = isDebugLogsEnabled,
                globalHandler = globalHandler
            )
        }
    }
}
