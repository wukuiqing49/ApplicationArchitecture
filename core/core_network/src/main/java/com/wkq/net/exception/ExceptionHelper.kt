package com.wkq.net.exception

import android.net.ParseException
import com.google.gson.JsonParseException
import org.json.JSONException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.CancellationException

/**
 * 异常处理辅助对象，用于将网络和 JSON 解析异常映射为清晰的错误代码和消息。
 */
object ExceptionHelper {
    
    // 自定义错误代码，大致对应常见问题
    const val ERROR_NETWORK_TIMEOUT = 1001      // 网络超时
    const val ERROR_NETWORK_CONNECTION = 1002   // 连接失败
    const val ERROR_NETWORK_UNKNOWN_HOST = 1003 // 未知主机（无网）
    const val ERROR_CANCELED = 1004             // 请求取消
    const val ERROR_JSON_PARSING = 2001         // JSON 解析错误
    const val ERROR_SSL = 3001                  // SSL 证书错误
    const val ERROR_UNKNOWN = -1                // 未知错误

    /**
     * 将泛型 Throwable 映射为结构化的代码和消息对。
     * @param e 请求期间抛出的异常。
     * @return 包含映射后的 (errorCode, errorMessage) 的 Pair。
     */
    fun handleException(e: Throwable): Pair<Int, String> {
        return when (e) {
            is SocketTimeoutException -> {
                Pair(ERROR_NETWORK_TIMEOUT, "请求超时，请稍后重试: ${e.message}")
            }
            is ConnectException -> {
                Pair(ERROR_NETWORK_CONNECTION, "无法连接到服务器，请检查网络: ${e.message}")
            }
            is UnknownHostException -> {
                Pair(ERROR_NETWORK_UNKNOWN_HOST, "无法识别主机，请检查网络连接: ${e.message}")
            }
            is JsonParseException, is JSONException, is ParseException -> {
                Pair(ERROR_JSON_PARSING, "数据解析错误，服务器返回了错误的 JSON 格式。")
            }
            is SSLHandshakeException -> {
                Pair(ERROR_SSL, "SSL 证书验证失败: ${e.message}")
            }
            is HttpException -> {
                val code = e.code()
                val msg = e.message()
                Pair(code, "HTTP 错误 $code: $msg")
            }
            is CancellationException -> {
                Pair(ERROR_CANCELED, "请求已取消")
            }
            else -> {
                Pair(ERROR_UNKNOWN, e.message ?: "发生未知错误")
            }
        }
    }
}
