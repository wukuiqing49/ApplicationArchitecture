package com.wkq.router.api

/**
 * 路由按需加载的分组接口
 */
interface IRouteGroup {
    /**
     * 将本组的所有路由注册到内存路由表
     */
    fun load()
}
