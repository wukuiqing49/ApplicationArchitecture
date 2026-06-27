package com.wkq.iptc.camera.camera

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 拍照临时文件工厂。
 */
object PhotoFileFactory {
    /**
     * 创建用于 CameraX 输出的临时照片文件。
     *
     * @param context 上下文，用于获取应用外部图片目录。
     * @return 新建的 JPEG 文件。
     */
    fun createPhotoFile(context: Context): File {
        val baseDir = context.getExternalFilesDir("press_iptc") ?: context.filesDir
        val photoDir = File(baseDir, "photos")
        if (!photoDir.exists()) {
            photoDir.mkdirs()
        }
        val fileName = buildString {
            append("IPTC_")
            append(SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()))
            append('_')
            append(UUID.randomUUID().toString().take(8))
            append(".jpg")
        }
        return File(photoDir, fileName)
    }
}
