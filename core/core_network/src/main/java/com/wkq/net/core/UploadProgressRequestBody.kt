package com.wkq.net.core

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import java.io.IOException

/**
 * 上传进度信息。
 *
 * @param percent 上传百分比；当无法获取总大小时为 -1。
 * @param currentLength 当前已写入字节数。
 * @param totalLength 总字节数；当无法获取时为 -1。
 */
data class UploadProgress(
    val percent: Int,
    val currentLength: Long,
    val totalLength: Long
)

/**
 * 支持上传进度回调的 RequestBody 包装类。
 *
 * @param delegate 原始 RequestBody。
 * @param onProgress 进度回调，范围 0-100；如果无法获取总大小则不回调。
 */
class UploadProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (Int) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = try {
        delegate.contentLength()
    } catch (e: IOException) {
        -1L
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink)
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
        private var bytesWritten = 0L
        private val totalBytes = contentLength()
        private var lastPercent = -1

        @Throws(IOException::class)
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount

            if (totalBytes > 0) {
                val percent = (bytesWritten * 100 / totalBytes).toInt().coerceIn(0, 100)
                if (percent != lastPercent) {
                    lastPercent = percent
                    onProgress(percent)
                }
            }
        }
    }
}

/**
 * 支持详细上传进度回调的 RequestBody 包装类。
 *
 * 单独保留这个类是为了不改变 [UploadProgressRequestBody] 已有构造方法，避免影响旧调用方。
 */
class UploadProgressDetailRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (UploadProgress) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = try {
        delegate.contentLength()
    } catch (e: IOException) {
        -1L
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink)
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
        private var bytesWritten = 0L
        private val totalBytes = contentLength()
        private var lastPercent = Int.MIN_VALUE

        @Throws(IOException::class)
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount

            val percent = if (totalBytes > 0) {
                (bytesWritten * 100 / totalBytes).toInt().coerceIn(0, 100)
            } else {
                -1
            }
            if (percent != lastPercent || totalBytes <= 0) {
                lastPercent = percent
                onProgress(UploadProgress(percent, bytesWritten, totalBytes))
            }
        }
    }
}
