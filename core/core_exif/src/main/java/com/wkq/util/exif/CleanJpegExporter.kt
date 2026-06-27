package com.wkq.util.exif

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.os.Build
import java.io.File

object CleanJpegExporter {

    fun isJpeg(file: File): Boolean {
        if (!file.isFile || file.length() < JPEG_HEADER_SIZE) return false
        return file.inputStream().use { input ->
            input.read() == JPEG_SOI_FIRST && input.read() == JPEG_SOI_SECOND
        }
    }

    fun exportCleanJpeg(sourceFile: File, outputFile: File): Result<File> = runCatching {
        val bitmap = decodeBitmap(sourceFile)
            ?: throw IllegalArgumentException("Unsupported image format")
        try {
            val jpegBitmap = bitmap.withOpaqueWhiteBackground()
            try {
                outputFile.outputStream().buffered().use { output ->
                    if (!jpegBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                        throw IllegalStateException("Failed to encode JPEG")
                    }
                }
            } finally {
                if (jpegBitmap !== bitmap) {
                    jpegBitmap.recycle()
                }
            }
        } finally {
            bitmap.recycle()
        }
        outputFile
    }

    /**
     * 解码图片为 Bitmap。
     *
     * - API 28+：使用 [ImageDecoder]，去掉 ALLOCATOR_SOFTWARE 限制，允许系统自动选择
     *   硬件解码器（GPU/DSP），HEIC 等格式速度大幅提升，内存占用更低。
     * - API < 28：使用 [BitmapFactory]，首次尝试全量解码；若发生 OOM 则自动以
     *   inSampleSize = 2 / 4 重试，保证低端机不崩溃，同时尽量保留分辨率。
     */
    private fun decodeBitmap(file: File): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // API 28+：让系统自动选择最优分配器（硬件解码 HEIC/WebP 速度最快）
            runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, _, _ ->
                    decoder.isMutableRequired = false
                    // 不再强制 ALLOCATOR_SOFTWARE，允许硬件解码器介入
                }
            }.getOrNull()
        } else {
            // API < 28：分级 OOM 防护，优先全量解码，失败则降采样
            decodeBitmapCompat(file)
        }
    }

    /**
     * 兼容低版本的解码方案，最多尝试 3 次降采样（1x → 2x → 4x）。
     * 降采样只在 OOM 时才触发，正常情况下输出全分辨率 Bitmap。
     */
    private fun decodeBitmapCompat(file: File): Bitmap? {
        val sampleSizes = intArrayOf(1, 2, 4)
        for (sampleSize in sampleSizes) {
            try {
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    // 优先 ARGB_8888 保留完整色深；低版本不支持 RGB_565 alpha
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                return BitmapFactory.decodeFile(file.absolutePath, opts) ?: continue
            } catch (oom: OutOfMemoryError) {
                // 当前 sampleSize 仍 OOM，尝试下一级降采样
                System.gc()
            }
        }
        return null // 所有级别均失败
    }

    private fun Bitmap.withOpaqueWhiteBackground(): Bitmap {
        if (!hasAlpha()) return this
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(output).apply {
            drawColor(Color.WHITE)
            drawBitmap(this@withOpaqueWhiteBackground, 0f, 0f, null)
        }
        return output
    }

    private const val JPEG_QUALITY = 95
    private const val JPEG_HEADER_SIZE = 2L
    private const val JPEG_SOI_FIRST = 0xFF
    private const val JPEG_SOI_SECOND = 0xD8
}
