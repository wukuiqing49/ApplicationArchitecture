package com.wkq.net.core

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.URLConnection

/**
 * 将 File 转换为 MultipartBody.Part
 *
 * @param fieldName 对应服务器接口的参数名，默认为 "file"
 * @param fileName 文件名，默认为 File.name
 */
fun File.toMultipartPart(fieldName: String = "file", fileName: String? = null): MultipartBody.Part {
    val requestFile = this.asRequestBody(getMimeType().toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(fieldName, fileName ?: name, requestFile)
}

/**
 * 将 File 转换为支持进度回调的 MultipartBody.Part
 *
 * @param fieldName 对应服务器接口的参数名，默认为 "file"
 * @param onProgress 进度回调 (0-100)
 */
fun File.toProgressPart(fieldName: String = "file", onProgress: (Int) -> Unit): MultipartBody.Part {
    val requestFile = this.asRequestBody(getMimeType().toMediaTypeOrNull())
    val progressRequestBody = UploadProgressRequestBody(requestFile, onProgress)
    return MultipartBody.Part.createFormData(fieldName, name, progressRequestBody)
}

/**
 * 为 RequestBody 增加进度回调包装
 */
fun RequestBody.asProgressRequestBody(onProgress: (Int) -> Unit): UploadProgressRequestBody {
    return UploadProgressRequestBody(this, onProgress)
}

/**
 * 获取文件的 MIME 类型
 */
fun File.getMimeType(): String {
    return URLConnection.guessContentTypeFromName(name) ?: "application/octet-stream"
}
