package com.wkq.router.api

/**
 *
 * @ Author: wkq
 *
 * @ Time: 2026/3/24 9:21
 *
 * @ Desc:

 */
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object RouteTable {

    val routes = ConcurrentHashMap<String, RouteMeta>()
    val groups = ConcurrentHashMap<String, Class<out IRouteGroup>>()
    val services = ConcurrentHashMap<Class<*>, Any>()
    val interceptors = CopyOnWriteArrayList<com.wkq.router.annotation.InterceptorMeta>()

    fun registerGroup(groupName: String, clazz: Class<out IRouteGroup>) {
        groups[groupName] = clazz
    }

    fun register(path: String, clazz: Class<*>, enterAnim: Int = 0, exitAnim: Int = 0) {
        routes[path] = RouteMeta(clazz, enterAnim, exitAnim)
    }

    fun registerService(api: Class<*>, impl: Any) {
        services[api] = impl
    }

    fun registerInterceptor(priority: Int, interceptor: IInterceptor) {
        interceptors.add(com.wkq.router.annotation.InterceptorMeta(priority, interceptor))
    }
}

data class RouteMeta(
    val clazz: Class<*>,
    val enterAnim: Int,
    val exitAnim: Int
)