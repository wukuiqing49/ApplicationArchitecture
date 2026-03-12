package com.wkq.net.config

import java.util.concurrent.ConcurrentHashMap

/**
 * Configuration class for the Advanced Network Framework.
 * Intended to be passed during initialization from the Application class.
 */
class NetConfig private constructor(
    val baseUrl: String,
    val connectTimeout: Long,
    val readTimeout: Long,
    val writeTimeout: Long,
    val defaultHeaders: Map<String, String>,
    val isDebugLogsEnabled: Boolean
) {
    class Builder {
        private var baseUrl: String = ""
        private var connectTimeout: Long = 15L
        private var readTimeout: Long = 20L
        private var writeTimeout: Long = 20L
        private var defaultHeaders = ConcurrentHashMap<String, String>()
        private var isDebugLogsEnabled: Boolean = false

        fun setBaseUrl(url: String) = apply { this.baseUrl = url }
        fun setConnectTimeout(seconds: Long) = apply { this.connectTimeout = seconds }
        fun setReadTimeout(seconds: Long) = apply { this.readTimeout = seconds }
        fun setWriteTimeout(seconds: Long) = apply { this.writeTimeout = seconds }
        fun addDefaultHeader(key: String, value: String) = apply { this.defaultHeaders[key] = value }
        fun setDebugLogsEnabled(enabled: Boolean) = apply { this.isDebugLogsEnabled = enabled }

        fun build(): NetConfig {
            require(baseUrl.isNotEmpty()) { "Base URL cannot be empty in NetConfig" }
            return NetConfig(
                baseUrl = baseUrl,
                connectTimeout = connectTimeout,
                readTimeout = readTimeout,
                writeTimeout = writeTimeout,
                defaultHeaders = defaultHeaders,
                isDebugLogsEnabled = isDebugLogsEnabled
            )
        }
    }
}
