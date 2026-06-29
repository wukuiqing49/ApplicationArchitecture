package com.wkq.net.core

import com.wkq.net.exception.ExceptionHelper
import com.wkq.net.https.HttpsUtils
import com.wkq.net.interceptor.LoggingInterceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * 文件下载状态。
 *
 * percent 为 -1 时表示服务端未返回 Content-Length，只能使用字节数展示进度。
 */
sealed class DownloadState {
    data class Progress(val percent: Int, val currentLength: Long, val totalLength: Long) : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Error(val code: Int, val message: String) : DownloadState()
}

/**
 * 文件下载 Retrofit 客户端。
 */
object DownloadRetrofit {

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
            .readTimeout(120L, TimeUnit.SECONDS)
            .writeTimeout(120L, TimeUnit.SECONDS)
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
 * 将 ResponseBody 下载到文件，并通过 Flow 发出进度。
 */
fun ResponseBody.downloadFileFlow(destFile: File): Flow<DownloadState> = flow {
    val totalLength = contentLength()
    try {
        destFile.parentFile?.mkdirs()
        emit(DownloadState.Progress(if (totalLength > 0L) 0 else -1, 0L, totalLength))

        byteStream().use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var currentLength = 0L
                var readSize: Int

                while (inputStream.read(buffer).also { readSize = it } != -1) {
                    outputStream.write(buffer, 0, readSize)
                    currentLength += readSize
                    val progress = if (totalLength > 0L) {
                        (currentLength * 100 / totalLength).toInt()
                    } else {
                        -1
                    }
                    emit(DownloadState.Progress(progress, currentLength, totalLength))
                }
                outputStream.flush()
            }
        }
        emit(DownloadState.Success(destFile))
    } catch (e: CancellationException) {
        deletePartialFile(destFile, totalLength)
        throw e
    } catch (e: Exception) {
        deletePartialFile(destFile, totalLength)
        val error = ExceptionHelper.handleToError(e)
        emit(DownloadState.Error(error.code, NetMessages.provider().fileProcessError(error.message)))
    }
}.flowOn(Dispatchers.IO)

private fun deletePartialFile(destFile: File, totalLength: Long) {
    if (!destFile.exists()) return
    if (totalLength <= 0L || destFile.length() < totalLength) {
        destFile.delete()
    }
}
