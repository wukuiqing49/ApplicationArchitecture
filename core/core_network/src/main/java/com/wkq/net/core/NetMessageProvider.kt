package com.wkq.net.core

import java.util.Locale

/**
 * 网络库文案提供器。
 *
 * 默认按系统语言返回中文或英文；业务可通过 NetConfig 自定义文案。
 */
interface NetMessageProvider {
    fun businessError(code: Int): String
    fun serverError(code: Int): String
    fun httpError(code: Int, message: String): String
    fun requestTimeout(detail: String?): String
    fun connectionFailed(detail: String?): String
    fun unknownHost(detail: String?): String
    fun jsonParseError(): String
    fun sslError(detail: String?): String
    fun canceled(): String
    fun unknownError(detail: String?): String
    fun fileProcessError(message: String): String
    fun initRequired(): String
    fun headerInitRequired(): String
}

object DefaultNetMessageProvider : NetMessageProvider {
    private val isChinese: Boolean
        get() = Locale.getDefault().language.equals("zh", ignoreCase = true)

    override fun businessError(code: Int): String {
        return if (isChinese) "服务器业务逻辑错误: $code" else "Server business error: $code"
    }

    override fun serverError(code: Int): String {
        return if (isChinese) "服务端错误: $code" else "Server error: $code"
    }

    override fun httpError(code: Int, message: String): String {
        return if (isChinese) "HTTP 错误 $code: $message" else "HTTP error $code: $message"
    }

    override fun requestTimeout(detail: String?): String {
        return if (isChinese) "请求超时，请稍后重试: ${detail.orEmpty()}" else "Request timed out. Please try again later: ${detail.orEmpty()}"
    }

    override fun connectionFailed(detail: String?): String {
        return if (isChinese) "无法连接到服务器，请检查网络: ${detail.orEmpty()}" else "Unable to connect to server. Please check your network: ${detail.orEmpty()}"
    }

    override fun unknownHost(detail: String?): String {
        return if (isChinese) "无法识别主机，请检查网络连接: ${detail.orEmpty()}" else "Unable to resolve host. Please check your network: ${detail.orEmpty()}"
    }

    override fun jsonParseError(): String {
        return if (isChinese) "数据解析错误，服务器返回了错误的 JSON 格式。" else "Data parse error. The server returned invalid JSON."
    }

    override fun sslError(detail: String?): String {
        return if (isChinese) "SSL 证书验证失败: ${detail.orEmpty()}" else "SSL certificate verification failed: ${detail.orEmpty()}"
    }

    override fun canceled(): String {
        return if (isChinese) "请求已取消" else "Request canceled"
    }

    override fun unknownError(detail: String?): String {
        val fallback = if (isChinese) "发生未知错误" else "Unknown error occurred"
        return detail ?: fallback
    }

    override fun fileProcessError(message: String): String {
        return if (isChinese) "文件处理错误: $message" else "File processing error: $message"
    }

    override fun initRequired(): String {
        return if (isChinese) {
            "在 Application 中使用网络框架前，必须先调用 NetManager.init() 初始化配置。"
        } else {
            "NetManager.init() must be called in Application before using the network framework."
        }
    }

    override fun headerInitRequired(): String {
        return if (isChinese) {
            "在使用 HeaderInterceptor 前，必须先调用 NetManager.init() 初始化配置。"
        } else {
            "NetManager.init() must be called before using HeaderInterceptor."
        }
    }
}

object NetMessages {
    fun provider(): NetMessageProvider {
        return if (NetManager.isInitialized()) {
            NetManager.getConfig().messageProvider
        } else {
            DefaultNetMessageProvider
        }
    }
}
