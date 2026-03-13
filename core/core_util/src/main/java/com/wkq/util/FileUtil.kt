package com.wkq.util

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * 文件及 URI 处理工具类
 */
object FileUtil {

    /**
     * 将本地 [File] 转换为安全的 [Uri]，用于跨应用分享（如相机、相册）
     * 在 Android 7.0 (API 24+) 以上自动使用 FileProvider。
     * 注意：须在 AndroidManifest 注册 ${applicationId}.fileprovider
     *
     * @param context Application 上下文
     * @param file 目标文件
     * @return content:// URI (API 24+) 或 file:// URI (API < 24)
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, file)
        } else {
            Uri.fromFile(file)
        }
    }

    /**
     * 将字符串保存到本地文件中
     */
    fun saveStringToFile(content: String, file: File): Boolean {
        return try {
            file.parentFile?.let {
                if (!it.exists()) it.mkdirs()
            }
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray())
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 从本地文件中读取字符串
     */
    fun readStringFromFile(file: File): String? {
        if (!file.exists()) return null
        return try {
            FileInputStream(file).use { fis ->
                fis.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 将 Uri 转换为本地 File 文件
     * 适配 Android 10+ 作用域存储，通过将内容流复制到指定目录实现
     * 
     * @param destPath 可选，指定存储文件夹路径。若为 null，默认存储在内部存储的 picker 目录下。
     */
    fun uriToFile(context: Context, uri: Uri, destPath: String? = null): File? {
        return when (uri.scheme) {
            "file" -> uri.path?.let { File(it) }
            "content" -> {
                val contentResolver = context.contentResolver
                val displayName = queryDisplayName(context, uri) ?: "temp_${System.currentTimeMillis()}"
                val targetDir = if (!destPath.isNullOrEmpty()) File(destPath) else context.getDir("picker", Context.MODE_PRIVATE)
                if (!targetDir.exists()) targetDir.mkdirs()
                
                val tempFile = File(targetDir, displayName)
                try {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        FileOutputStream(tempFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    tempFile
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            else -> null
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                    if (idx != -1) cursor.getString(idx) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
