package com.wkq.router.annotation

/**
 * 拦截器元数据，保存拦截器实例及其优先级。
 */
data class InterceptorMeta(
    val priority: Int,
    val interceptor: Any
)
