package com.wkq.net.interceptor

import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Custom OkHttp Logging Interceptor wrapper.
 * Provides a cleaner output and allows toggling logs entirely based on the environment.
 */
object LoggingInterceptor {

    private const val TAG = "NetLog"

    /**
     * Creates an HttpLoggingInterceptor tailored for the current environment status.
     * @param isDebug boolean flag instructing if logs should be deeply printed.
     */
    fun create(isDebug: Boolean): HttpLoggingInterceptor {
        val logger = HttpLoggingInterceptor.Logger { message ->
            if (isDebug) {
                // You can add further formatting here if necessary
                Log.d(TAG, message)
            }
        }

        return HttpLoggingInterceptor(logger).apply {
            level = if (isDebug) {
                HttpLoggingInterceptor.Level.BODY // Full request/response body during development
            } else {
                HttpLoggingInterceptor.Level.NONE // Silent in production
            }
        }
    }
}
