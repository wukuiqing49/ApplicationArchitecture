package com.wkq.net.config

import java.util.concurrent.ConcurrentHashMap

/**
 * 旧版动态 BaseUrl 管理入口。
 *
 * 新代码优先使用 [NetConfig.Builder.putBaseUrl] 统一配置，这个类仅用于兼容旧调用。
 */
@Deprecated(
    message = "请使用 NetConfig.Builder.putBaseUrl() 统一配置动态 BaseUrl。",
    replaceWith = ReplaceWith("NetConfig.Builder().putBaseUrl(key, url)")
)
object BaseUrlManager {

    private val baseUrlMap = ConcurrentHashMap<String, String>()

    @Volatile
    private var defaultBaseUrl: String? = null

    fun add(key: String, url: String) {
        baseUrlMap[key.trim()] = formatUrl(url)
    }

    fun addAll(map: Map<String, String>) {
        map.forEach { (key, url) ->
            add(key, url)
        }
    }

    fun get(key: String?): String? {
        if (key.isNullOrBlank()) return defaultBaseUrl
        return baseUrlMap[key.trim()] ?: defaultBaseUrl
    }

    fun setDefault(url: String) {
        defaultBaseUrl = formatUrl(url)
    }

    fun remove(key: String) {
        baseUrlMap.remove(key.trim())
    }

    fun clear() {
        baseUrlMap.clear()
        defaultBaseUrl = null
    }

    private fun formatUrl(url: String): String {
        val trimUrl = url.trim()
        return if (trimUrl.endsWith("/")) trimUrl else "$trimUrl/"
    }
}
