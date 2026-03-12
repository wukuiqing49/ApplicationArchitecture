package com.wkq.net

/**
 * Standard HTTP response wrapper class.
 * Ensures that all server responses use a consistent structure.
 */
data class BaseResponse<T>(
    val code: Int = -1,
    val message: String? = null,
    val data: T? = null
) {
    /**
     * Determine whether the request is successful based on the code.
     * Normally, 200 denotes success, but this can be adjusted.
     */
    fun isSuccess(): Boolean = code == 200
}
