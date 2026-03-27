package com.wkq.util.jump

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * URL 处理工具类
 */
object UrlUtils {

    /**
     * 获取重定向后的最终 URL
     * 
     * @param urlString 原始 URL
     * @param maxRedirects 最大重定向次数，防止无限循环
     * @return 最终的 URL
     */
    suspend fun getFinalUrl(urlString: String, maxRedirects: Int = 10): String = withContext(Dispatchers.IO) {
        var currentUrl = urlString
        var redirects = 0
        
        try {
            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                // 模拟浏览器 User-Agent，防止某些网站屏蔽爬虫
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")

                val responseCode = connection.responseCode
                
                // 检查是否为重定向响应 (3xx)
                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    if (location != null) {
                        // 处理相对路径重定向
                        currentUrl = if (location.startsWith("/")) {
                            val base = URL(currentUrl)
                            "${base.protocol}://${base.host}${if (base.port != -1) ":${base.port}" else ""}$location"
                        } else {
                            location
                        }
                        redirects++
                        continue
                    }
                }
                
                // 正常响应 (200) 或非重定向响应，返回当前 URL
                return@withContext currentUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext currentUrl
    }
}
