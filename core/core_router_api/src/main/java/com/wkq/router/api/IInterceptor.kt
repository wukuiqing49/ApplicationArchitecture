package com.wkq.router.api

/**
 * 路由拦截器接口。
 */
interface IInterceptor {
    fun process(postcard: Postcard, callback: InterceptorCallback)
}

/**
 * 拦截器回调。
 */
interface InterceptorCallback {
    /**
     * 继续路由。
     */
    fun onContinue(postcard: Postcard)

    /**
     * 中断路由。
     *
     * @param exception 中断原因
     */
    fun onInterrupt(exception: Throwable? = null)
}
