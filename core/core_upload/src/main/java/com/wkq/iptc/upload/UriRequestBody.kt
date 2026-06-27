package com.wkq.iptc.upload

import android.content.Context
import android.net.Uri
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException

/**
 * 鑷畾涔?RequestBody锛岀洿鎺ヨ鍙?Context 涓殑 Uri 鍐呭骞舵祦寮忓啓鍏?okhttp銆?
 * 鏀寔鍦ㄥ啓鍏ヨ繃绋嬩腑鍥炶皟杩涘害銆?
 */
class UriRequestBody(
    private val context: Context,
    private val uri: Uri,
    private val contentType: MediaType?,
    private val contentLength: Long,
    private val skipOffset: Long = 0L,
    private val onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)? = null
) : RequestBody() {

    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = contentLength - skipOffset

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open input stream for URI: $uri")
        inputStream.use { input ->
            if (skipOffset > 0L) {
                input.safeSkip(skipOffset)
            }
            val buffer = ByteArray(8192)
            var bytesWritten = skipOffset
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                bytesWritten += read
                onProgress?.invoke(bytesWritten, contentLength)
            }
        }
    }
}

