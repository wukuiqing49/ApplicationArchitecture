package com.wkq.router.annotation

/**
 * 路由注解，用于标记 Activity, Fragment 或 Compose 页面
 * @param path 路由路径，例如 "/user/login"
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Route(
    val path: String,
    val enterAnim: String = "",
    val exitAnim: String = ""
)
