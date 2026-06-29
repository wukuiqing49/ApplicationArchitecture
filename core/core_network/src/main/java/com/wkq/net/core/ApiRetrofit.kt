package com.wkq.net.core

import com.wkq.net.BaseResponse
import com.wkq.net.exception.ExceptionHelper
import com.wkq.net.https.HttpsUtils
import com.wkq.net.interceptor.LoggingInterceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.awaitResponse
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

/**
 * 处理普通 JSON API 的 Retrofit 单例客户端。
 */
object ApiRetrofit {

    @Volatile
    private var retrofit: Retrofit? = null

    private val lock = Any()

    private fun getRetrofit(): Retrofit {
        return retrofit ?: synchronized(lock) {
            retrofit ?: buildRetrofit().also { retrofit = it }
        }
    }

    private fun buildRetrofit(): Retrofit {
        val config = NetManager.getConfig()

        val okHttpClientBuilder = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeout, TimeUnit.SECONDS)
            .readTimeout(config.readTimeout, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeout, TimeUnit.SECONDS)
            .addInterceptor(NetManager.headerInterceptor)
            .addInterceptor(LoggingInterceptor.create(config.isDebugLogsEnabled))

        if (config.allowUnsafeSsl) {
            val trustManager = HttpsUtils.UnSafeTrustManager()
            val sslSocketFactory = HttpsUtils.createSSLSocketFactory(trustManager)
            okHttpClientBuilder.sslSocketFactory(sslSocketFactory, trustManager)
            okHttpClientBuilder.hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier())
        }

        return Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> create(serviceClass: Class<T>): T {
        return getRetrofit().create(serviceClass)
    }

    internal fun reset() {
        synchronized(lock) {
            retrofit = null
        }
    }
}

/**
 * 默认 code/message/data 响应壳请求入口，兼容旧用法。
 */
suspend inline fun <T> safeApiCall(crossinline apiCall: suspend () -> BaseResponse<T>): ApiResponse<T> {
    return safeApiCall(apiCall, BaseResponseParser())
}

/**
 * 使用自定义响应壳解析器执行请求，适配 status/msg/result 等非固定后台格式。
 */
suspend inline fun <R, T> safeApiCall(
    crossinline apiCall: suspend () -> R,
    parser: NetResponseParser<R, T>
): ApiResponse<T> {
    return try {
        val response = apiCall()
        if (parser.isSuccess(response)) {
            ApiResponse.Success(parser.data(response))
        } else {
            val code = parser.code(response)
            val message = parser.message(response)
            val handled = NetManager.getConfig().globalHandler?.onHandleBusinessCode(code, message) ?: false
            if (handled) {
                // 全局处理器已经完成具体交互，这里仍返回 Error 让调用方感知。
            }
            ApiResponse.Error(code, message ?: NetMessages.provider().businessError(code), ErrorType.BUSINESS)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ExceptionHelper.handleToError(e)
    }
}

/**
 * 使用 NetConfig 中配置的默认响应壳解析器执行请求。
 *
 * 如果接口本身不带业务响应壳，请使用 safeRawApiCall，避免把原始 JSON 当成业务壳解析。
 */
suspend inline fun <R, T> safeApiCallWithDefaultParser(
    crossinline apiCall: suspend () -> R
): ApiResponse<T> {
    val parser = NetManager.getConfig().defaultResponseParserFactory.create<R, T>()
    return safeApiCall(apiCall, parser)
}

/**
 * 针对非业务响应壳接口，直接把返回内容映射为 Success。
 */
suspend inline fun <T> safeRawApiCall(crossinline apiCall: suspend () -> T): ApiResponse<T> {
    return try {
        val response = apiCall()
        ApiResponse.Success(response)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ExceptionHelper.handleToError(e)
    }
}

/**
 * 默认 code/message/data Call 请求入口，兼容旧用法。
 */
suspend fun <T> Call<BaseResponse<T>>.awaitResult(): ApiResponse<T> {
    return awaitResult(BaseResponseParser())
}

/**
 * 将 Retrofit Call 作为协程执行，并使用自定义响应壳解析器适配非固定后台格式。
 */
suspend fun <R, T> Call<R>.awaitResult(parser: NetResponseParser<R, T>): ApiResponse<T> {
    return try {
        val response = this.awaitResponse()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && parser.isSuccess(body)) {
                ApiResponse.Success(parser.data(body))
            } else {
                val code = body?.let { parser.code(it) } ?: response.code()
                val message = body?.let { parser.message(it) } ?: NetMessages.provider().serverError(code)
                NetManager.getConfig().globalHandler?.onHandleBusinessCode(code, message)
                ApiResponse.Error(code, message, ErrorType.BUSINESS)
            }
        } else {
            ApiResponse.Error(
                code = response.code(),
                message = NetMessages.provider().httpError(
                    response.code(),
                    response.message().ifEmpty { response.code().toString() }
                ),
                type = ErrorType.HTTP
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ExceptionHelper.handleToError(e)
    }
}

/**
 * 使用 NetConfig 中配置的默认响应壳解析器执行 Call 请求。
 */
suspend fun <R, T> Call<R>.awaitResultWithDefaultParser(): ApiResponse<T> {
    val parser = NetManager.getConfig().defaultResponseParserFactory.create<R, T>()
    return awaitResult(parser)
}

/**
 * 针对非业务响应壳的 Call，直接将 body 映射为 Success。
 */
suspend fun <T> Call<T>.awaitRawResult(): ApiResponse<T> {
    return try {
        val response = this.awaitResponse()
        if (response.isSuccessful) {
            ApiResponse.Success(response.body())
        } else {
            ApiResponse.Error(
                code = response.code(),
                message = NetMessages.provider().httpError(
                    response.code(),
                    response.message().ifEmpty { response.code().toString() }
                ),
                type = ErrorType.HTTP
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ExceptionHelper.handleToError(e)
    }
}
