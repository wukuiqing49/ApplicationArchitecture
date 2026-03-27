package com.wkq.util.jump

import android.net.Uri

/**
 * 智能跳转处理器接口
 */
interface JumpHandler {

    /**
     * 判断当前 Handler 是否能处理该 URL
     */
    fun canHandle(url: String): Boolean

    /**
     * 将 URL 转换为该平台特有的 Scheme
     */
    fun convertToScheme(url: String): String?

    /**
     * 优先级，数字越大优先级越高
     */
    fun getPriority(): Int = 0
}

/**
 * 基础处理器，提供常用辅助方法
 */
abstract class BaseJumpHandler : JumpHandler {

    protected fun getParam(url: String, name: String): String? {
        return try {
            val uri = Uri.parse(url)
            uri.getQueryParameter(name)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 通用 URL 清洗器：尝试从风控页、中间跳转页中提取真实的目标 URL
     * 适用于解决京东 RiskHandler、淘宝拦截页等问题
     */
    protected fun cleanUrl(url: String): String {
        // 常见的中间跳转参数名
        val redirectParams = listOf(
            "returnurl", "returnUrl", "url", "target", "targetUrl",
            "biz_url", "origin_url", "redirect_url", "jumpUrl"
        )

        for (paramName in redirectParams) {
            val extracted = getParam(url, paramName)
            // 校验提取出的内容：必须是非空的 http/https 链接
            if (extracted != null && extracted.startsWith("http") && extracted.length > 10) {
                // 递归清洗，防止多层嵌套
                return cleanUrl(extracted)
            }
        }
        return url
    }
}
