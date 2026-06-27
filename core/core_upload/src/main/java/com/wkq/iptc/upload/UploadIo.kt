package com.wkq.iptc.upload

import android.content.Context
import android.net.Uri
import android.os.Build
import java.io.File
import java.util.Locale

fun copyUriToTempFile(
    context: Context,
    recordUri: String,
    fileName: String
): File {
    val sourceUri = Uri.parse(recordUri)
    val safeSuffix = buildTempFileSuffix(fileName)
    val tempFile = File.createTempFile("upload_", safeSuffix, context.cacheDir)
    val resolvedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
        sourceUri.scheme == "content" && 
        sourceUri.authority == android.provider.MediaStore.AUTHORITY) {
        try {
            android.provider.MediaStore.setRequireOriginal(sourceUri)
        } catch (e: Exception) {
            sourceUri
        }
    } else {
        sourceUri
    }
    val inputStream = runCatching {
        if (resolvedUri.scheme == "file") {
            val path = resolvedUri.path ?: error("Invalid file uri")
            java.io.File(path).inputStream()
        } else {
            context.contentResolver.openInputStream(resolvedUri)
        }
    }.recoverCatching {
        if (sourceUri.scheme == "file") {
            val path = sourceUri.path ?: error("Invalid file uri")
            java.io.File(path).inputStream()
        } else {
            context.contentResolver.openInputStream(sourceUri)
        }
    }.getOrNull() ?: error("Unable to open upload source")

    inputStream.use { input ->
        tempFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return tempFile
}

internal fun buildRemotePath(remoteDir: String, fileName: String): String {
    val normalizedDir = remoteDir.trim().trimEnd('/')
    return if (normalizedDir.isBlank()) {
        "/$fileName"
    } else {
        "$normalizedDir/$fileName"
    }
}

private fun buildTempFileSuffix(fileName: String): String {
    val sanitized = fileName
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .takeLast(32)
        .ifBlank { "file" }
    val normalized = if (sanitized.startsWith(".")) {
        "_${sanitized.lowercase(Locale.US)}"
    } else {
        sanitized.lowercase(Locale.US)
    }
    return "_$normalized"
}

fun getUriLength(context: Context, uri: Uri): Long {
    if (uri.scheme == "file") {
        return uri.path?.let { File(it).length() } ?: -1L
    }
    var size = -1L
    runCatching {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
            size = it.length
        }
    }
    if (size >= 0) return size

    runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
    }
    return size
}

class ProgressInputStream(
    private val delegate: java.io.InputStream,
    private val initialOffset: Long = 0L,
    private val onProgress: (bytesWritten: Long) -> Unit
) : java.io.InputStream() {
    private var bytesWritten = initialOffset

    override fun read(): Int {
        val b = delegate.read()
        if (b != -1) {
            bytesWritten++
            onProgress(bytesWritten)
        }
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val read = delegate.read(b, off, len)
        if (read != -1) {
            bytesWritten += read
            onProgress(bytesWritten)
        }
        return read
    }

    override fun close() {
        delegate.close()
    }

    override fun available(): Int = delegate.available()
    override fun skip(n: Long): Long = delegate.skip(n)
}

internal fun copyStreamWithProgress(
    input: java.io.InputStream,
    out: java.io.OutputStream,
    contentLength: Long,
    onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)?
) {
    val buffer = ByteArray(8192)
    var bytesWritten = 0L
    var read: Int
    while (input.read(buffer).also { read = it } != -1) {
        out.write(buffer, 0, read)
        bytesWritten += read
        onProgress?.invoke(bytesWritten, contentLength)
    }
}

/**
 * 确保 InputStream 安全跳过 n 字节的扩展函数。
 * 解决原生 skip() 可能返回 0 导致无限死循环或跳过不足的问题。
 */
fun java.io.InputStream.safeSkip(n: Long): Long {
    var remaining = n
    while (remaining > 0L) {
        val skipped = skip(remaining)
        if (skipped > 0L) {
            remaining -= skipped
        } else if (skipped == 0L) {
            // skip() 返回 0 时尝试读取单字节强制推进，以防死循环
            if (read() == -1) {
                break // 到达流末端
            }
            remaining--
        } else {
            // skip 返回负数，说明流发生异常或到达末尾，提前退出
            break
        }
    }
    return n - remaining
}


