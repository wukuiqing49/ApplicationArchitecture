package com.wkq.net.config

/**
 *
 * @ Author: wkq
 *
 * @ Time: 2026/3/23 10:09
 *
 * @ Desc:  为了处理 BaseUrl的基础类

 */
object BaseUrlManager {

    private val baseUrlMap = mutableMapOf<String, String>()

    // 默认BaseUrl
    private var defaultBaseUrl: String? = null

    /**
     * 添加 BaseUrl
     */
    fun add(key: String, url: String) {
        baseUrlMap[key] = formatUrl(url)
    }

    /**
     * 批量添加
     */
    fun addAll(map: Map<String, String>) {
        map.forEach { (k, v) ->
            add(k, v)
        }
    }

    /**
     * 获取 BaseUrl
     */
    fun get(key: String?): String? {
        if (key.isNullOrEmpty()) return defaultBaseUrl
        return baseUrlMap[key] ?: defaultBaseUrl
    }

    /**
     * 设置默认 BaseUrl
     */
    fun setDefault(url: String) {
        defaultBaseUrl = formatUrl(url)
    }

    /**
     * 删除
     */
    fun remove(key: String) {
        baseUrlMap.remove(key)
    }

    /**
     * 清空
     */
    fun clear() {
        baseUrlMap.clear()
    }

    /**
     * 统一格式
     */
    private fun formatUrl(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }
}