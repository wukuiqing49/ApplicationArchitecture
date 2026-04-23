package com.wkq.router.annotation

/**
 * 自动注入参数注解
 * @param name 参数名，如果不填则默认使用变量名
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
annotation class Param(
    val name: String = ""
)
