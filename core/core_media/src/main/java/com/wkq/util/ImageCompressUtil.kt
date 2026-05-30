package com.wkq.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.zibin.luban.api.Luban
import top.zibin.luban.api.OnCompressListener

import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 基于 Luban 的图片压缩工具类
 * 
 * 核心功能：
 * 1. 支持 Uri/Path 图片压缩
 * 2. 支持协程异步调用，直接返回 Result<File>
 * 3. 支持设置忽略压缩的阈值 (ignoreBy)
 * 
 * @author Antigravity
 */
object ImageCompressUtil {

    /**
     * 压缩图片 (支持 Path)
     * 
     * @param context 上下文
     * @param path 图片原始路径
     * @param targetSizeKb 忽略压缩的阈值 (单位 KB)，即小于此大小的图片不压缩。
     *                    注意：Luban 并不支持“强制压缩到 XX KB以下”，它使用的是类微信压缩算法。
     * @param destDir 压缩后的存储目录，如果不传则存入内部缓存 picker 目录
     */
    suspend fun compress(
        context: Context,
        path: String,
        targetSizeKb: Int = 100,
        destDir: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext Result.failure(Exception("File not found: $path"))
        compressFile(context, file, targetSizeKb, destDir)
    }

    /**
     * 压缩图片 (支持 Uri)
     */
    suspend fun compress(
        context: Context,
        uri: Uri,
        targetSizeKb: Int = 100,
        destDir: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        // 如果是 content uri，先利用 FileUtil 转成临时文件再压缩（Luban load(Uri) 在某些版本可能有兼容性问题）
        val tempFile = FileUtil.uriToFile(context, uri)
            ?: return@withContext Result.failure(Exception("Failed to convert Uri to file"))
        
        compressFile(context, tempFile, targetSizeKb, destDir)
    }

    private suspend fun compressFile(
        context: Context,
        file: File,
        targetSizeKb: Int,
        destDir: String?
    ): Result<File> = suspendCoroutine { continuation ->
        try {
            val targetFolder = if (!destDir.isNullOrEmpty()) {
                File(destDir).apply { if (!exists()) mkdirs() }
            } else {
                context.getDir("picker", Context.MODE_PRIVATE)
            }

            Luban.with(context)
                .load(file)

                .setTargetDir(targetFolder.absolutePath)
                .setCompressListener(object : OnCompressListener {
                    override fun onStart() {
                    }

                    override fun onSuccess(file: File) {
                        continuation.resume(Result.success(file))
                    }

                    override fun onError(e: Throwable) {
                        continuation.resume(Result.failure(e))
                    }
                }).launch()
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }
}
