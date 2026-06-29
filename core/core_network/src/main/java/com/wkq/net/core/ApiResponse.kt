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
     * 表示失败的网络响应，可能是由于 HTTP 错误、连接超时、解析错误或服务器业务逻辑错误。
     *
     * @param code 标准化错误码、HTTP 状态码或业务错误码。
     * @param message 用户友好的错误消息。
     * @param type 错误分类，方便调用方做稳定分支处理。
     * @param throwable 原始异常，业务层只应在调试、日志上报时使用。
     */
    data class Error @JvmOverloads constructor(
        val code: Int,
        val message: String,
        val type: ErrorType = ErrorType.UNKNOWN,
        val throwable: Throwable? = null
    ) : ApiResponse<Nothing>()
}

/**
 * 网络错误分类。
 */
enum class ErrorType {
    BUSINESS,
    HTTP,
    NETWORK,
    TIMEOUT,
    PARSE,
    SSL,
    CANCELED,
    UNKNOWN
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

/**
 * 如果 [ApiResponse] 表示错误，则返回包含错误分类的回调。
 */
inline fun <T> ApiResponse<T>.onErrorDetail(action: (ApiResponse.Error) -> Unit): ApiResponse<T> {
    if (this is ApiResponse.Error) {
        action(this)
    }
    return this
}
