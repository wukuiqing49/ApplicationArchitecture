package com.wkq.router.annotation

/**
 * 路由注解，用于标记 Activity、Fragment 或自定义页面。
 *
 * @param path 路由路径，例如 "/user/login"
 * @param enterAnim 进入动画资源名
 * @param exitAnim 退出动画资源名
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Route(
    val path: String,
    val enterAnim: String = "",
    val exitAnim: String = ""
)
