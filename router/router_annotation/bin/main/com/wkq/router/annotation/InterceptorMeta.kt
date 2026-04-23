package com.wkq.router.annotation

/**
 * 拦截器元数据，存储拦截器实例及其优先级
 */
data class InterceptorMeta(
    val priority: Int,
    val interceptor: Any // 这里使用 Any，实际在 API 层强转为 IInterceptor 避免循环依赖
)
