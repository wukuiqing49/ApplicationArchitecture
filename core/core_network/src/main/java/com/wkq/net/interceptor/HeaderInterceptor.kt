package com.wkq.net.interceptor

import com.wkq.net.core.NetManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap

/**
 * 负责全局将请求头注入到每个 OkHttp 请求中的拦截器。
 * 支持在运行时动态添加或移除请求头。
 * 同时支持通过 "BaseUrl-Key" 请求头动态切换 BaseUrl。
 */
class HeaderInterceptor(defaultHeaders: Map<String, String>) : Interceptor {

    companion object {
        const val HEADER_BASE_URL_KEY = "BaseUrl-Key"
    }

    private val dynamicHeaders = ConcurrentHashMap<String, String>()

    init {
        // 使用 NetConfig 提供的默认请求头初始化
        dynamicHeaders.putAll(defaultHeaders)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // 应用所有动态请求头
        dynamicHeaders.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        // --- 动态切换 BaseUrl 逻辑 ---
        val domainKey = originalRequest.header(HEADER_BASE_URL_KEY)?.trim()
        if (!domainKey.isNullOrEmpty()) {
            val config = NetManager.getConfig()
            val newBaseUrl = config.baseUrls[domainKey]
            if (!newBaseUrl.isNullOrEmpty()) {
                val newHttpUrl = newBaseUrl.toHttpUrlOrNull()
                if (newHttpUrl != null) {
                    val oldHttpUrl = originalRequest.url
                    val defaultBaseUrl = config.baseUrl.toHttpUrlOrNull()
                    val finalPath = buildSwitchedPath(
                        oldPath = oldHttpUrl.encodedPath,
                        oldBasePath = defaultBaseUrl?.encodedPath,
                        newBasePath = newHttpUrl.encodedPath
                    )
                    val finalUrl = oldHttpUrl.newBuilder()
                        .scheme(newHttpUrl.scheme)
                        .host(newHttpUrl.host)
                        .port(newHttpUrl.port)
                        .encodedPath(finalPath)
                        .build()
                    requestBuilder.url(finalUrl)
                }
            }
            // 移除用于切换的辅助 Header，避免发送到服务端
            requestBuilder.removeHeader(HEADER_BASE_URL_KEY)
        }

        return chain.proceed(requestBuilder.build())
    }

    private fun buildSwitchedPath(oldPath: String, oldBasePath: String?, newBasePath: String): String {
        val relativePath = oldPath.removeBasePath(oldBasePath)
        val cleanNewBasePath = newBasePath.trimEnd('/').takeUnless { it.isBlank() } ?: ""
        return when {
            cleanNewBasePath.isEmpty() && relativePath.isEmpty() -> "/"
            cleanNewBasePath.isEmpty() -> relativePath.ensureStartsWithSlash()
            relativePath.isEmpty() -> cleanNewBasePath.ensureStartsWithSlash()
            else -> "${cleanNewBasePath.ensureStartsWithSlash()}/${relativePath.trimStart('/')}"
        }
    }

    private fun String.removeBasePath(basePath: String?): String {
        val cleanBasePath = basePath?.trimEnd('/').takeUnless { it.isNullOrBlank() || it == "/" } ?: return this
        return when {
            this == cleanBasePath -> ""
            startsWith("$cleanBasePath/") -> removePrefix(cleanBasePath)
            else -> this
        }
    }

    private fun String.ensureStartsWithSlash(): String {
        return if (startsWith("/")) this else "/$this"
    }

    /**
     * 动态添加请求头
     */
    fun addHeader(key: String, value: String) {
        dynamicHeaders[key] = value
    }

    /**
     * 动态移除请求头
     */
    fun removeHeader(key: String) {
        dynamicHeaders.remove(key)
    }

    /**
     * 清空所有请求头
     */
    fun clearHeaders() {
        dynamicHeaders.clear()
    }
}
