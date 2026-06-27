package com.wkq.iptc.camera.camera

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.net.URLConnection

/**
 * 系统相册照片保存器。
 *
 * 将临时拍照文件写入 MediaStore，兼容 Android 10+ 分区存储。
 *
 * @param context 上下文，用于访问 ContentResolver。
 */
class MediaStorePhotoSaver(private val context: Context) {

    /**
     * 保存照片到系统相册。
     *
     * @param sourceFile 待保存的源文件。
     * @param deleteSourceOnSuccess 保存成功后是否删除源文件。
     * @return 保存成功时返回 MediaStore Uri。
     */
    fun savePhoto(sourceFile: File, deleteSourceOnSuccess: Boolean = true): Result<Uri> {
        return runCatching {
            val resolver = context.contentResolver
            val fileName = sourceFile.name
            val mimeType = URLConnection.guessContentTypeFromName(fileName) ?: "image/jpeg"
            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val subDir = com.wkq.util.SpUtils.getString("photo_save_dir", "SiteReport")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$subDir")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = requireNotNull(resolver.insert(collection, values))
            try {
                resolver.openOutputStream(uri)?.use { output ->
                    sourceFile.inputStream().use { input -> input.copyTo(output) }
                } ?: error("Failed to open MediaStore output stream")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                } else {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(sourceFile.absolutePath),
                        arrayOf(mimeType),
                        null
                    )
                }
                if (deleteSourceOnSuccess) sourceFile.delete()
                uri
            } catch (t: Throwable) {
                resolver.delete(uri, null, null)
                throw t
            }
        }
    }

    /**
     * 从系统相册删除照片。
     *
     * @param uri MediaStore 图片 Uri。
     * @return 删除结果。
     */
    fun deletePhoto(uri: Uri): Result<Unit> = runCatching {
        context.contentResolver.delete(uri, null, null)
        Unit
    }
}
