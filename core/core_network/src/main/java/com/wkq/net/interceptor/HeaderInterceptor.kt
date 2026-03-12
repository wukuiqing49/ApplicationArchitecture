package com.wkq.net.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap

/**
 * Interceptor responsible for globally injecting headers into every OkHttp Request.
 * Supports adding or removing headers dynamically at runtime.
 */
class HeaderInterceptor(defaultHeaders: Map<String, String>) : Interceptor {

    private val dynamicHeaders = ConcurrentHashMap<String, String>()

    init {
        // Initialize with default headers provided by NetConfig
        dynamicHeaders.putAll(defaultHeaders)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // Apply all dynamic headers
        dynamicHeaders.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        return chain.proceed(requestBuilder.build())
    }

    /**
     * Dynamically add a header
     */
    fun addHeader(key: String, value: String) {
        dynamicHeaders[key] = value
    }

    /**
     * Dynamically remove a header
     */
    fun removeHeader(key: String) {
        dynamicHeaders.remove(key)
    }

    /**
     * Clear all headers
     */
    fun clearHeaders() {
        dynamicHeaders.clear()
    }
}
