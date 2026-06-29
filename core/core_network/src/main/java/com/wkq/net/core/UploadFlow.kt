package com.wkq.net.core

import com.wkq.net.BaseResponse
import com.wkq.net.exception.ExceptionHelper
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MultipartBody

/**
 * 文件上传状态。
 */
sealed class UploadState<out T> {
    data class Progress(
        val percent: Int,
        val currentLength: Long,
        val totalLength: Long
    ) : UploadState<Nothing>()

    data class Success<out T>(val data: T?) : UploadState<T>()

    data class Error(
        val code: Int,
        val message: String,
        val type: ErrorType = ErrorType.UNKNOWN,
        val throwable: Throwable? = null
    ) : UploadState<Nothing>()
}

/**
 * 上传单个文件，默认按 BaseResponse<T> 响应壳解析。
 */
fun <T> uploadFlow(
    file: File,
    fieldName: String = "file",
    fileName: String? = null,
    apiCall: suspend (MultipartBody.Part) -> BaseResponse<T>
): Flow<UploadState<T>> = uploadFlow(file, fieldName, fileName, BaseResponseParser(), apiCall)

/**
 * 上传单个文件，并使用自定义响应壳解析器。
 */
fun <R, T> uploadFlow(
    file: File,
    fieldName: String = "file",
    fileName: String? = null,
    parser: NetResponseParser<R, T>,
    apiCall: suspend (MultipartBody.Part) -> R
): Flow<UploadState<T>> = channelFlow {
    try {
        validateUploadFile(file)
        val totalLength = file.length()
        trySend(UploadState.Progress(progressPercent(0L, totalLength), 0L, totalLength))

        val part = file.toProgressPartWithDetail(fieldName, fileName) { progress ->
            trySend(UploadState.Progress(progress.percent, progress.currentLength, progress.totalLength))
        }
        trySend(safeApiCall({ apiCall(part) }, parser).toUploadState())
        close()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        trySend(ExceptionHelper.handleToError(e).toUploadState())
        close()
    }
    awaitClose()
}.flowOn(Dispatchers.IO)

/**
 * 上传单个文件，并使用 NetConfig 中配置的默认响应壳解析器。
 */
fun <R, T> uploadFlowWithDefaultParser(
    file: File,
    fieldName: String = "file",
    fileName: String? = null,
    apiCall: suspend (MultipartBody.Part) -> R
): Flow<UploadState<T>> {
    val parser = NetManager.getConfig().defaultResponseParserFactory.create<R, T>()
    return uploadFlow(file, fieldName, fileName, parser, apiCall)
}

/**
 * 上传单个文件，不解析业务响应壳，直接把 Retrofit 返回体作为成功数据。
 */
fun <T> uploadRawFlow(
    file: File,
    fieldName: String = "file",
    fileName: String? = null,
    apiCall: suspend (MultipartBody.Part) -> T
): Flow<UploadState<T>> = channelFlow {
    try {
        validateUploadFile(file)
        val totalLength = file.length()
        trySend(UploadState.Progress(progressPercent(0L, totalLength), 0L, totalLength))

        val part = file.toProgressPartWithDetail(fieldName, fileName) { progress ->
            trySend(UploadState.Progress(progress.percent, progress.currentLength, progress.totalLength))
        }
        trySend(safeRawApiCall { apiCall(part) }.toUploadState())
        close()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        trySend(ExceptionHelper.handleToError(e).toUploadState())
        close()
    }
    awaitClose()
}.flowOn(Dispatchers.IO)

/**
 * 上传多个文件，默认按 BaseResponse<T> 响应壳解析，并聚合所有文件总进度。
 */
fun <T> uploadFilesFlow(
    files: List<File>,
    fieldName: String = "files",
    apiCall: suspend (List<MultipartBody.Part>) -> BaseResponse<T>
): Flow<UploadState<T>> = uploadFilesFlow(files, fieldName, BaseResponseParser(), apiCall)

/**
 * 上传多个文件，并使用自定义响应壳解析器。
 */
fun <R, T> uploadFilesFlow(
    files: List<File>,
    fieldName: String = "files",
    parser: NetResponseParser<R, T>,
    apiCall: suspend (List<MultipartBody.Part>) -> R
): Flow<UploadState<T>> = channelFlow {
    try {
        validateUploadFiles(files)
        val totalLength = files.sumOf { it.length() }
        val currentLengths = LongArray(files.size)
        val progressLock = Any()
        var lastPercent = Int.MIN_VALUE

        fun sendProgress() {
            val currentLength = currentLengths.sum()
            val percent = progressPercent(currentLength, totalLength)
            if (percent != lastPercent || totalLength <= 0L) {
                lastPercent = percent
                trySend(UploadState.Progress(percent, currentLength, totalLength))
            }
        }

        trySend(UploadState.Progress(progressPercent(0L, totalLength), 0L, totalLength))
        val parts = files.mapIndexed { index, uploadFile ->
            uploadFile.toProgressPartWithDetail(fieldName) { progress ->
                synchronized(progressLock) {
                    currentLengths[index] = progress.currentLength
                    sendProgress()
                }
            }
        }
        trySend(safeApiCall({ apiCall(parts) }, parser).toUploadState())
        close()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        trySend(ExceptionHelper.handleToError(e).toUploadState())
        close()
    }
    awaitClose()
}.flowOn(Dispatchers.IO)

/**
 * 上传多个文件，并使用 NetConfig 中配置的默认响应壳解析器。
 */
fun <R, T> uploadFilesFlowWithDefaultParser(
    files: List<File>,
    fieldName: String = "files",
    apiCall: suspend (List<MultipartBody.Part>) -> R
): Flow<UploadState<T>> {
    val parser = NetManager.getConfig().defaultResponseParserFactory.create<R, T>()
    return uploadFilesFlow(files, fieldName, parser, apiCall)
}

/**
 * 上传多个文件，不解析业务响应壳，直接把 Retrofit 返回体作为成功数据。
 */
fun <T> uploadRawFilesFlow(
    files: List<File>,
    fieldName: String = "files",
    apiCall: suspend (List<MultipartBody.Part>) -> T
): Flow<UploadState<T>> = channelFlow {
    try {
        validateUploadFiles(files)
        val totalLength = files.sumOf { it.length() }
        val currentLengths = LongArray(files.size)
        val progressLock = Any()
        var lastPercent = Int.MIN_VALUE

        fun sendProgress() {
            val currentLength = currentLengths.sum()
            val percent = progressPercent(currentLength, totalLength)
            if (percent != lastPercent || totalLength <= 0L) {
                lastPercent = percent
                trySend(UploadState.Progress(percent, currentLength, totalLength))
            }
        }

        trySend(UploadState.Progress(progressPercent(0L, totalLength), 0L, totalLength))
        val parts = files.mapIndexed { index, uploadFile ->
            uploadFile.toProgressPartWithDetail(fieldName) { progress ->
                synchronized(progressLock) {
                    currentLengths[index] = progress.currentLength
                    sendProgress()
                }
            }
        }
        trySend(safeRawApiCall { apiCall(parts) }.toUploadState())
        close()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        trySend(ExceptionHelper.handleToError(e).toUploadState())
        close()
    }
    awaitClose()
}.flowOn(Dispatchers.IO)

private fun <T> ApiResponse<T>.toUploadState(): UploadState<T> {
    return when (this) {
        is ApiResponse.Success -> UploadState.Success(data)
        is ApiResponse.Error -> UploadState.Error(code, message, type, throwable)
    }
}

private fun ApiResponse.Error.toUploadState(): UploadState.Error {
    return UploadState.Error(code, message, type, throwable)
}

private fun validateUploadFile(file: File) {
    if (!file.exists()) {
        throw IllegalArgumentException(NetMessages.provider().fileProcessError("file does not exist: ${file.path}"))
    }
    if (!file.isFile) {
        throw IllegalArgumentException(NetMessages.provider().fileProcessError("path is not a file: ${file.path}"))
    }
    if (!file.canRead()) {
        throw IllegalArgumentException(NetMessages.provider().fileProcessError("file is not readable: ${file.path}"))
    }
}

private fun validateUploadFiles(files: List<File>) {
    require(files.isNotEmpty()) {
        NetMessages.provider().fileProcessError("file list is empty")
    }
    files.forEach(::validateUploadFile)
}

private fun progressPercent(currentLength: Long, totalLength: Long): Int {
    return if (totalLength > 0L) {
        (currentLength * 100 / totalLength).toInt().coerceIn(0, 100)
    } else {
        -1
    }
}
