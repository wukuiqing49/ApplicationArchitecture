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
}
