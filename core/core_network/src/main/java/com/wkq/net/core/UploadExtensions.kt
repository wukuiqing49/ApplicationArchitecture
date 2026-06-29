package com.wkq.net.core

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.URLConnection

/**
 * 将 File 转换为 MultipartBody.Part。
 *
 * @param fieldName 对应服务端接口的参数名，默认是 "file"。
 * @param fileName 文件名，默认使用 File.name。
 */
fun File.toMultipartPart(fieldName: String = "file", fileName: String? = null): MultipartBody.Part {
    val requestFile = this.asRequestBody(getMimeType().toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(fieldName, fileName ?: name, requestFile)
}

/**
 * 将 File 转换为支持进度回调的 MultipartBody.Part。
 *
 * @param fieldName 对应服务端接口的参数名，默认是 "file"。
 * @param onProgress 上传进度回调，范围 0-100。
 */
fun File.toProgressPart(fieldName: String = "file", onProgress: (Int) -> Unit): MultipartBody.Part {
    val requestFile = this.asRequestBody(getMimeType().toMediaTypeOrNull())
    val progressRequestBody = UploadProgressRequestBody(requestFile, onProgress)
    return MultipartBody.Part.createFormData(fieldName, name, progressRequestBody)
}

/**
 * 将 File 转换为支持详细进度回调的 MultipartBody.Part。
 *
 * @param fieldName 对应服务端接口的参数名，默认是 "file"。
 * @param fileName 文件名，默认使用 File.name。
 * @param onProgress 上传进度回调，包含百分比、已上传字节数和总字节数。
 */
fun File.toProgressPartWithDetail(
    fieldName: String = "file",
    fileName: String? = null,
    onProgress: (UploadProgress) -> Unit
): MultipartBody.Part {
    val requestFile = this.asRequestBody(getMimeType().toMediaTypeOrNull())
    val progressRequestBody = UploadProgressDetailRequestBody(requestFile, onProgress)
    return MultipartBody.Part.createFormData(fieldName, fileName ?: name, progressRequestBody)
}

/**
 * 将 File 转换为支持进度回调的 MultipartBody.Part，并把进度回调切回主线程。
 */
fun File.toProgressPartOnMain(fieldName: String = "file", onProgress: (Int) -> Unit): MultipartBody.Part {
    val mainHandler = Handler(Looper.getMainLooper())
    return toProgressPart(fieldName) { percent ->
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onProgress(percent)
        } else {
            mainHandler.post { onProgress(percent) }
        }
    }
}

/**
 * 将 File 转换为支持详细进度回调的 MultipartBody.Part，并把进度回调切回主线程。
 */
fun File.toProgressPartWithDetailOnMain(
    fieldName: String = "file",
    fileName: String? = null,
    onProgress: (UploadProgress) -> Unit
): MultipartBody.Part {
    val mainHandler = Handler(Looper.getMainLooper())
    return toProgressPartWithDetail(fieldName, fileName) { progress ->
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onProgress(progress)
        } else {
            mainHandler.post { onProgress(progress) }
        }
    }
}

/**
 * 为 RequestBody 增加进度回调包装。
 */
fun RequestBody.asProgressRequestBody(onProgress: (Int) -> Unit): UploadProgressRequestBody {
    return UploadProgressRequestBody(this, onProgress)
}

/**
 * 为 RequestBody 增加详细进度回调包装。
 */
fun RequestBody.asProgressRequestBodyWithDetail(
    onProgress: (UploadProgress) -> Unit
): UploadProgressDetailRequestBody {
    return UploadProgressDetailRequestBody(this, onProgress)
}

/**
 * 为 RequestBody 增加进度回调包装，并把进度回调切回主线程。
 */
fun RequestBody.asProgressRequestBodyOnMain(onProgress: (Int) -> Unit): UploadProgressRequestBody {
    val mainHandler = Handler(Looper.getMainLooper())
    return asProgressRequestBody { percent ->
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onProgress(percent)
        } else {
            mainHandler.post { onProgress(percent) }
        }
    }
}

/**
 * 为 RequestBody 增加详细进度回调包装，并把进度回调切回主线程。
 */
fun RequestBody.asProgressRequestBodyWithDetailOnMain(
    onProgress: (UploadProgress) -> Unit
): UploadProgressDetailRequestBody {
    val mainHandler = Handler(Looper.getMainLooper())
    return asProgressRequestBodyWithDetail { progress ->
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onProgress(progress)
        } else {
            mainHandler.post { onProgress(progress) }
        }
    }
}

/**
 * 获取文件 MIME 类型。
 */
fun File.getMimeType(): String {
    return URLConnection.guessContentTypeFromName(name) ?: "application/octet-stream"
}
