package com.wkq.net.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap

/**
 * 负责全局将请求头注入到每个 OkHttp 请求中的拦截器。
 * 支持在运行时动态添加或移除请求头。
 */
class HeaderInterceptor(defaultHeaders: Map<String, String>) : Interceptor {

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

        return chain.proceed(requestBuilder.build())
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
