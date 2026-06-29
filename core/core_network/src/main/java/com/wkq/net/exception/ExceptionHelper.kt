package com.wkq.net.exception

import android.net.ParseException
import com.google.gson.JsonParseException
import com.wkq.net.core.ApiResponse
import com.wkq.net.core.ErrorType
import com.wkq.net.core.NetMessages
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
        val error = handleToError(e)
        return Pair(error.code, error.message)
    }

    /**
     * 将异常转换为稳定的 ApiResponse.Error。
     */
    fun handleToError(e: Throwable): ApiResponse.Error {
        val messages = NetMessages.provider()
        return when (e) {
            is SocketTimeoutException -> {
                ApiResponse.Error(ERROR_NETWORK_TIMEOUT, messages.requestTimeout(e.message), ErrorType.TIMEOUT, e)
            }
            is ConnectException -> {
                ApiResponse.Error(ERROR_NETWORK_CONNECTION, messages.connectionFailed(e.message), ErrorType.NETWORK, e)
            }
            is UnknownHostException -> {
                ApiResponse.Error(ERROR_NETWORK_UNKNOWN_HOST, messages.unknownHost(e.message), ErrorType.NETWORK, e)
            }
            is JsonParseException, is JSONException, is ParseException -> {
                ApiResponse.Error(ERROR_JSON_PARSING, messages.jsonParseError(), ErrorType.PARSE, e)
            }
            is SSLHandshakeException -> {
                ApiResponse.Error(ERROR_SSL, messages.sslError(e.message), ErrorType.SSL, e)
            }
            is HttpException -> {
                val code = e.code()
                val msg = e.message()
                ApiResponse.Error(code, messages.httpError(code, msg), ErrorType.HTTP, e)
            }
            is CancellationException -> {
                ApiResponse.Error(ERROR_CANCELED, messages.canceled(), ErrorType.CANCELED, e)
            }
            else -> {
                ApiResponse.Error(ERROR_UNKNOWN, messages.unknownError(e.message), ErrorType.UNKNOWN, e)
            }
        }
    }
}
