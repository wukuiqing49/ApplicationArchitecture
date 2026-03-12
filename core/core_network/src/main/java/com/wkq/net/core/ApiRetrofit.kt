package com.wkq.net.core

import com.wkq.net.BaseResponse
import com.wkq.net.exception.ExceptionHelper
import com.wkq.net.https.HttpsUtils
import com.wkq.net.interceptor.LoggingInterceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.awaitResponse
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 处理一般 JSON API 交互的单例客户端。
 * 必须先调用 [NetManager.init] 获取配置后才能使用。
 */
object ApiRetrofit {

    private val retrofit: Retrofit by lazy {
        val config = NetManager.getConfig()

        val okHttpClientBuilder = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeout, TimeUnit.SECONDS)
            .readTimeout(config.readTimeout, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeout, TimeUnit.SECONDS)
            .addInterceptor(NetManager.headerInterceptor)
            .addInterceptor(LoggingInterceptor.create(config.isDebugLogsEnabled))

        // Configure HTTPS
        val sslSocketFactory = HttpsUtils.createSSLSocketFactory()
        okHttpClientBuilder.sslSocketFactory(sslSocketFactory, HttpsUtils.UnSafeTrustManager())
        okHttpClientBuilder.hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier())

        Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 动态创建 API Service 实例
     */
    fun <T> create(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}

/**
 * 全局内联辅助函数，用于安全地执行 Retrofit 挂起函数，并将异常映射为安全的 [ApiResponse]。
 * 用法: val response = safeApiCall { apiService.getMusicList() }
 */
suspend inline fun <T> safeApiCall(crossinline apiCall: suspend () -> BaseResponse<T>): ApiResponse<T> {
    return try {
        val response = apiCall()
        if (response.isSuccess()) {
            ApiResponse.Success(response.data)
        } else {
            ApiResponse.Error(response.code, response.message ?: "Server logic error: ${response.code}")
        }
    } catch (e: Exception) {
        val (code, msg) = ExceptionHelper.handleException(e)
        ApiResponse.Error(code, msg)
    }
}

/**
 * 扩展方法：将标准的 Retrofit [Call] 作为挂起协程块执行。
 * 模仿了旧版的 `.request(Callback)` 风格，但使用协程进行线性、同步的代码书写。
 *
 * 用法: apiService.getMusicList(1).awaitResult().onSuccess { ... }.onError { ... }
 */
suspend fun <T> Call<BaseResponse<T>>.awaitResult(): ApiResponse<T> {
    return try {
        val response = this.awaitResponse()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.isSuccess()) {
                ApiResponse.Success(body.data)
            } else {
                ApiResponse.Error(body?.code ?: response.code(), body?.message ?: "Server error: ${body?.code}")
            }
        } else {
            ApiResponse.Error(response.code(), response.message().ifEmpty { "HTTP Error ${response.code()}" })
        }
    } catch (e: Exception) {
        val (code, msg) = ExceptionHelper.handleException(e)
        ApiResponse.Error(code, msg)
    }
}
