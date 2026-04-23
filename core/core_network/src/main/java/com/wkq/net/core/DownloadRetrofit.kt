package com.wkq.net.core

import com.wkq.net.exception.ExceptionHelper
import com.wkq.net.https.HttpsUtils
import com.wkq.net.interceptor.LoggingInterceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * 表示 Kotlin Flow 中文件下载操作的状态。
 */
sealed class DownloadState {
    // 下载进度：百分比、当前长度、总长度
    data class Progress(val percent: Int, val currentLength: Long, val totalLength: Long) : DownloadState()
    // 下载成功：返回下载的文件对象
    data class Success(val file: File) : DownloadState()
    // 下载失败：错误码和错误消息
    data class Error(val code: Int, val message: String) : DownloadState()
}

/**
 * 专门用于下载文件的单例客户端。
 * 将超时时间配置得更大，并去除了 JSON 转换器以提高效率。
 */
object DownloadRetrofit {

    private val retrofit: Retrofit by lazy {
        val config = NetManager.getConfig()

        val okHttpClientBuilder = OkHttpClient.Builder()
            // 为下载提供极大的超时时间
            .connectTimeout(config.connectTimeout, TimeUnit.SECONDS)
            .readTimeout(120L, TimeUnit.SECONDS)
            .writeTimeout(120L, TimeUnit.SECONDS)
            // 继承标准请求头
            .addInterceptor(NetManager.headerInterceptor)
            // 下载过程中仅保留请求头日志，避免控制台被大量字节数据刷新
            .addInterceptor(LoggingInterceptor.create(config.isDebugLogsEnabled))

        // 配置 HTTPS
        val sslSocketFactory = HttpsUtils.createSSLSocketFactory()
        okHttpClientBuilder.sslSocketFactory(sslSocketFactory, HttpsUtils.UnSafeTrustManager())
        okHttpClientBuilder.hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier())

        Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClientBuilder.build())
            // 注意：此处没有 GsonConverterFactory。
            .build()
    }

    /**
     * 创建下载服务接口实例
     */
    fun <T> create(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}

/**
 * 扩展方法：通过 Kotlin Flow 清晰地下载文件并发出进度。
 * 在 IO 调度器上优雅运行。
 */
fun ResponseBody.downloadFileFlow(destFile: File): Flow<DownloadState> = flow {
    try {
        val totalLength = contentLength()
        emit(DownloadState.Progress(0, 0L, totalLength))

        byteStream().use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                val buffer = ByteArray(4096)
                var currentLength = 0L
                var readSize: Int

                while (inputStream.read(buffer).also { readSize = it } != -1) {
                    outputStream.write(buffer, 0, readSize)
                    currentLength += readSize
                    val progress = (currentLength * 100 / totalLength).toInt()
                    // 发出进度状态
                    emit(DownloadState.Progress(progress, currentLength, totalLength))
                }
                outputStream.flush()
            }
        }
        emit(DownloadState.Success(destFile))
    } catch (e: kotlinx.coroutines.CancellationException) {
        // 协程被取消，清理部分文件并重新抛出
        if (destFile.exists() && destFile.length() < contentLength()) {
            destFile.delete()
        }
        throw e
    } catch (e: Exception) {
        // 网络错误时清理部分下载的文件
        if (destFile.exists() && destFile.length() < contentLength()) {
            destFile.delete()
        }
        val (code, msg) = ExceptionHelper.handleException(e)
        emit(DownloadState.Error(code, "文件处理错误: $msg"))
    }
}.flowOn(Dispatchers.IO)
