package com.wkq.net.interceptor

import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor

/**
 * 自定义的 OkHttp 协议日志拦截器包装类。
 * 提供更简洁的输出，并允许根据环境彻底关闭日志。
 */
object LoggingInterceptor {

    private const val TAG = "NetLog"

    /**
     * 为当前环境创建定制的 HttpLoggingInterceptor。
     * @param isDebug 布尔标志，指示是否应深度打印日志。
     */
    fun create(isDebug: Boolean): HttpLoggingInterceptor {
        val logger = HttpLoggingInterceptor.Logger { message ->
            if (isDebug) {
                // 如果需要，可在此处添加进一步的格式化逻辑
                Log.d(TAG, message)
            }
        }

        return HttpLoggingInterceptor(logger).apply {
            level = if (isDebug) {
                HttpLoggingInterceptor.Level.BODY // 开发期间打印完整请求/响应体
            } else {
                HttpLoggingInterceptor.Level.NONE // 生产环境保持静默
            }
        }
    }
}
