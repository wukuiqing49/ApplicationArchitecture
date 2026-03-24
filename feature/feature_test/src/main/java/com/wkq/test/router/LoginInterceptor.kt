package com.wkq.test.router

import android.util.Log
import com.wkq.router.api.IInterceptor
import com.wkq.router.annotation.Interceptor
import com.wkq.router.api.InterceptorCallback
import com.wkq.router.api.Postcard

/**
 * 模拟登录拦截器
 */
@Interceptor(priority = 8)
class LoginInterceptor : IInterceptor {
    override fun process(postcard: Postcard, callback: InterceptorCallback) {
        Log.d("Router", "拦截器执行: ${postcard.path}")
        
        if (postcard.path == "/test/need_login") {
            Log.d("Router", "检测到需要登录，重定向到登录页")
            // 模拟重定向 (实际项目中可以修改 postcard.path 或弹出登录框)
            // 这里简单打印日志，演示它拦截到了。
            callback.onContinue(postcard)
        } else {
            callback.onContinue(postcard)
        }
    }
}
