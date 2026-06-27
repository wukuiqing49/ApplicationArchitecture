package com.wkq.router.annotation

import kotlin.reflect.KClass

/**
 * 服务提供注解，用于跨模块服务发现。
 *
 * @param api 服务接口类型
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ProvideService(
    val api: KClass<*>
)
