package com.wkq.net

/**
 * 标准的 HTTP 响应包装类。
 * 确保所有服务器响应都使用一致的结构。
 */
data class BaseResponse<T>(
    val code: Int = -1,
    val message: String? = null,
    val data: T? = null
) {
    /**
     * 根据 code 判断请求是否成功。
     * 通常 200 表示成功，但可以根据具体业务调整。
     */
    fun isSuccess(): Boolean = code == 200
}
