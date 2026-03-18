package com.wkq.net.core

/**
 * 使用 Kotlin 协程的安全网络响应密封类。
 * 允许使用带有 when 语句的全面处理成功数据或错误，而无需抛出异常。
 */
sealed class ApiResponse<out T> {

    /**
     * 表示成功的网络响应，且服务器业务逻辑也指示成功。
     * @param data 服务器返回的有效负载，可以为 null。
     */
    data class Success<out T>(val data: T?) : ApiResponse<T>()

    /**
     * 表示失败的网络响应，可能是由于 HTTP 错误、连接超时或服务器业务逻辑错误。
     * @param code 标准化或 HTTP 错误代码。
     * @param message 用户友好的错误消息。
     */
    data class Error(val code: Int, val message: String) : ApiResponse<Nothing>()
}

/**
 * 如果 [ApiResponse] 表示成功，则运行代码块的扩展函数。
 * 返回当前 [ApiResponse] 实例以便链式调用，例如 `.onSuccess {}.onError {}`。
 */
inline fun <T> ApiResponse<T>.onSuccess(action: (T?) -> Unit): ApiResponse<T> {
    if (this is ApiResponse.Success) {
        action(data)
    }
    return this
}

/**
 * 如果 [ApiResponse] 表示错误，则运行代码块的扩展函数。
 * 返回当前 [ApiResponse] 实例以便链式调用。
 */
inline fun <T> ApiResponse<T>.onError(action: (Int, String) -> Unit): ApiResponse<T> {
    if (this is ApiResponse.Error) {
        action(code, message)
    }
    return this
}
