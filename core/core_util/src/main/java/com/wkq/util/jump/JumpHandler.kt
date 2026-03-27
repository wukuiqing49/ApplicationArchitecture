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
}
