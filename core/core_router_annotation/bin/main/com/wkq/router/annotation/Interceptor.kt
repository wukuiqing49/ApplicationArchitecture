package com.wkq.router.annotation

/**
 * 拦截器注解，用于自动注册全局拦截器。
 *
 * @param priority 优先级，数值越大越先执行
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Interceptor(
    val priority: Int = 0
)
