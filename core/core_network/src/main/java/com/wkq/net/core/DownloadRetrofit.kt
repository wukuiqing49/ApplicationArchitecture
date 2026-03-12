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
 * Represents the state of a file download operation within a Kotlin Flow.
 */
sealed class DownloadState {
    data class Progress(val percent: Int, val currentLength: Long, val totalLength: Long) : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Error(val code: Int, val message: String) : DownloadState()
}

/**
 * Singleton client designed specifically for downloading files.
 * Customizes timeouts to be much larger and omits JSON Converters.
 */
object DownloadRetrofit {

    // Removed internal executor, we will rely on Coroutines Dispatchers.IO

    private val retrofit: Retrofit by lazy {
        val config = NetManager.getConfig()

        val okHttpClientBuilder = OkHttpClient.Builder()
            // Provide massive timeouts for downloads
            .connectTimeout(config.connectTimeout, TimeUnit.SECONDS)
            .readTimeout(120L, TimeUnit.SECONDS)
            .writeTimeout(120L, TimeUnit.SECONDS)
            // Inherit standard headers
            .addInterceptor(NetManager.headerInterceptor)
            // Keep logs to headers only during downloads to avoid console flooding with bytes
            .addInterceptor(LoggingInterceptor.create(config.isDebugLogsEnabled).apply {
                level = if (config.isDebugLogsEnabled) okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS else okhttp3.logging.HttpLoggingInterceptor.Level.NONE
            })

        // Configure HTTPS
        val sslSocketFactory = HttpsUtils.createSSLSocketFactory()
        okHttpClientBuilder.sslSocketFactory(sslSocketFactory, HttpsUtils.UnSafeTrustManager())
        okHttpClientBuilder.hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier())

        Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClientBuilder.build())
            // Notice: No GsonConverterFactory here.
            .build()
    }

    fun <T> create(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }

}

/**
 * Extension method to cleanly download a file and emit progress through a Kotlin Flow.
 * Operates gracefully on the IO dispatcher.
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
                    // Emit progress state
                    emit(DownloadState.Progress(progress, currentLength, totalLength))
                }
                outputStream.flush()
            }
        }
        emit(DownloadState.Success(destFile))
    } catch (e: kotlinx.coroutines.CancellationException) {
        // Coroutine was cancelled, clean up partial file and re-throw
        if (destFile.exists() && destFile.length() < contentLength()) {
            destFile.delete()
        }
        throw e
    } catch (e: Exception) {
        // Clean up partial file on network error
        if (destFile.exists() && destFile.length() < contentLength()) {
            destFile.delete()
        }
        val (code, msg) = ExceptionHelper.handleException(e)
        emit(DownloadState.Error(code, "File processing error: $msg"))
    }
}.flowOn(Dispatchers.IO)
