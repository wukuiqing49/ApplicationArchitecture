package com.wkq.router.api

import android.util.Log

/**
 * 路由运行时配置。
 */
object RouterConfig {
    var debug: Boolean = false
    var throwExceptionWhenRouteNotFound: Boolean = false
    var logger: Logger = AndroidLogger()

    interface Logger {
        fun d(message: String)
        fun e(message: String, throwable: Throwable? = null)
    }

    private class AndroidLogger : Logger {
        override fun d(message: String) {
            if (debug) {
                Log.d(TAG, message)
            }
        }

        override fun e(message: String, throwable: Throwable?) {
            if (throwable == null) {
                Log.e(TAG, message)
            } else {
                Log.e(TAG, message, throwable)
            }
        }
    }

    private const val TAG = "WkqRouter"
}
