package com.wkq.net.core

/**
 * A sealed class representing a safe network response utilizing Kotlin Coroutines.
 * Allows exhaustive `when` statements for handling successful data or errors without throwing exceptions.
 */
sealed class ApiResponse<out T> {

    /**
     * Represents a successful network response where the server logic also indicated success.
     * @param data The payload returned by the server, can be null.
     */
    data class Success<out T>(val data: T?) : ApiResponse<T>()

    /**
     * Represents a failed network response, either due to HTTP errors, connection timeouts, or server logic errors.
     * @param code The standardized or HTTP error code.
     * @param message The user-friendly error message.
     */
    data class Error(val code: Int, val message: String) : ApiResponse<Nothing>()
}

/**
 * Extension to run a block of code if the [ApiResponse] represents a success.
 * Returns the current [ApiResponse] instance to allow chaining, e.g. `.onSuccess {}.onError {}`.
 */
inline fun <T> ApiResponse<T>.onSuccess(action: (T?) -> Unit): ApiResponse<T> {
    if (this is ApiResponse.Success) {
        action(data)
    }
    return this
}

/**
 * Extension to run a block of code if the [ApiResponse] represents an error.
 * Returns the current [ApiResponse] instance to allow chaining.
 */
inline fun <T> ApiResponse<T>.onError(action: (Int, String) -> Unit): ApiResponse<T> {
    if (this is ApiResponse.Error) {
        action(code, message)
    }
    return this
}
