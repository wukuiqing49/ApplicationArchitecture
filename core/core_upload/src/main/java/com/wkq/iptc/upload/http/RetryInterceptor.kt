package com.wkq.iptc.upload.http

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import android.util.Log

/**
 * 弱网自动重试拦截器，用于提升上传文件的稳定性。
 */
class RetryInterceptor(private val maxRetry: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null
        var tryCount = 0
        
        while (tryCount < maxRetry) {
            try {
                if (tryCount > 0) {
                    Log.d("RetryInterceptor", "Retrying request (${tryCount + 1}/$maxRetry) to URL: ${request.url}")
                    val backoffDelay = (1000L * tryCount * tryCount).coerceAtMost(5000L)
                    try {
                        Thread.sleep(backoffDelay)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Retry sleep interrupted", ie)
                    }
                }
                
                response?.close()
                response = chain.proceed(request)
                if (response.isSuccessful) {
                    return response
                }
                
                val code = response.code
                if (code in 400..499) {
                    // Client side errors (like 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found) should not be retried.
                    return response
                }
            } catch (e: IOException) {
                exception = e
                response?.close()
                response = null
            }
            tryCount++
        }
        
        if (response != null) return response
        throw exception ?: IOException("Upload failed after $maxRetry retries under weak network conditions")
    }
}
