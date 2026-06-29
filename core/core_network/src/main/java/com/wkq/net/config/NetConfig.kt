package com.wkq.net.config

import com.wkq.net.core.GlobalNetHandler
import com.wkq.net.core.BaseResponseParserFactory
import com.wkq.net.core.DefaultNetMessageProvider
import com.wkq.net.core.NetMessageProvider
import com.wkq.net.core.NetResponseParserFactory
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
    val baseUrls: Map<String, String>,
    val isDebugLogsEnabled: Boolean,
    val allowUnsafeSsl: Boolean,
    val defaultResponseParserFactory: NetResponseParserFactory,
    val messageProvider: NetMessageProvider,
    val globalHandler: GlobalNetHandler? = null
) {
    class Builder {
        private var baseUrl: String = ""
        private var connectTimeout: Long = 15L
        private var readTimeout: Long = 20L
        private var writeTimeout: Long = 20L
        private var defaultHeaders = ConcurrentHashMap<String, String>()
        private var baseUrls = ConcurrentHashMap<String, String>()
        private var isDebugLogsEnabled: Boolean = false
        private var allowUnsafeSsl: Boolean = false
        private var defaultResponseParserFactory: NetResponseParserFactory = BaseResponseParserFactory
        private var messageProvider: NetMessageProvider = DefaultNetMessageProvider
        private var globalHandler: GlobalNetHandler? = null

        fun setBaseUrl(url: String) = apply { this.baseUrl = formatBaseUrl(url) }
        fun setConnectTimeout(seconds: Long) = apply { this.connectTimeout = seconds }
        fun setReadTimeout(seconds: Long) = apply { this.readTimeout = seconds }
        fun setWriteTimeout(seconds: Long) = apply { this.writeTimeout = seconds }
        fun addDefaultHeader(key: String, value: String) = apply { this.defaultHeaders[key] = value }
        fun putBaseUrl(key: String, url: String) = apply { this.baseUrls[key.trim()] = formatBaseUrl(url) }
        fun setDebugLogsEnabled(enabled: Boolean) = apply { this.isDebugLogsEnabled = enabled }
        fun setAllowUnsafeSsl(enabled: Boolean) = apply { this.allowUnsafeSsl = enabled }
        fun setDefaultResponseParserFactory(factory: NetResponseParserFactory) = apply {
            this.defaultResponseParserFactory = factory
        }
        fun setMessageProvider(provider: NetMessageProvider) = apply { this.messageProvider = provider }
        fun setGlobalHandler(handler: GlobalNetHandler) = apply { this.globalHandler = handler }

        fun build(): NetConfig {
            require(baseUrl.isNotEmpty()) { "NetConfig 中的 Base URL 不能为空" }
            return NetConfig(
                baseUrl = baseUrl,
                connectTimeout = connectTimeout,
                readTimeout = readTimeout,
                writeTimeout = writeTimeout,
                defaultHeaders = defaultHeaders.toMap(),
                baseUrls = baseUrls.toMap(),
                isDebugLogsEnabled = isDebugLogsEnabled,
                allowUnsafeSsl = allowUnsafeSsl,
                defaultResponseParserFactory = defaultResponseParserFactory,
                messageProvider = messageProvider,
                globalHandler = globalHandler
            )
        }

        private fun formatBaseUrl(url: String): String {
            val trimUrl = url.trim()
            require(trimUrl.startsWith("http://") || trimUrl.startsWith("https://")) {
                "Base URL 必须以 http:// 或 https:// 开头"
            }
            return if (trimUrl.endsWith("/")) trimUrl else "$trimUrl/"
        }
    }
}
