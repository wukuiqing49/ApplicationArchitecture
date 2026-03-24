package com.wkq.router.api

import android.content.Context

/**
 * 路由降级服务接口
 * 当遇到 Route not found 等异常时，由该服务进行统一拦截和兜底处理
 */
interface IDegradationService {

    /**
     * 路由丢失时触发
     * @param context 当前上下文
     * @param postcard 原始路由请求载体
     */
    fun onLost(context: Context, postcard: Postcard)
}
