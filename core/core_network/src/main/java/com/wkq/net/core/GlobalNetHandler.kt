package com.wkq.net.core

/**
 * 全局网络业务代码处理器接口。
 * 用于集中处理特定的服务器响应代码（例如：登录过期/Token失效）。
 */
interface GlobalNetHandler {
    /**
     * 处理全局业务代码。
     * @param code 来自 BaseResponse 的业务代码。
     * @param message 来自 BaseResponse 的消息。
     * @return 如果 code 被全局处理（例如跳转登录页），则返回 true；
     *         返回 false 则由具体的调用业务方按常规错误处理。
     */
    fun onHandleBusinessCode(code: Int, message: String?): Boolean
}
