package com.wkq.util.coil

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import android.widget.ImageView
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.use
import java.io.File
import java.security.MessageDigest

/**
 * CacheManager
 *
 * 功能：
 * 1. 管理 Coil 的 ImageLoader 缓存（内存/磁盘）
 * 2. 提供永久缓存机制（内存 + 磁盘），适合头像、礼物等频繁使用资源
 * 3. 支持 URL / 本地 File / resId
 * 4. 提供 ImageView 测量完成后生成 Bitmap 的工具，避免第一次加载模糊
 */
object CacheManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 永久内存缓存
    private var pinnedMemoryCache: LruCache<String, Bitmap>? = null

    // 永久磁盘缓存 (使用 Coil DiskCache 管理大小)
    private var pinnedDiskCache: DiskCache? = null

    // Coil ImageLoader 实例
    private var imageLoader: ImageLoader? = null

    private var isInitialized = false

    /**
     * 初始化缓存
     * @param context Context
     * @param pinnedMemoryMB 永久内存缓存大小 (MB)
     * @param memoryCachePercent Coil 内存缓存占比
     * @param pinnedDiskMB 永久磁盘缓存大小 (MB)
     * @param diskCacheMB Coil 磁盘缓存大小 (MB)
     */
    fun init(context: Context, pinnedMemoryMB: Int = 20, memoryCachePercent: Double = 0.25, pinnedDiskMB: Int = 200, diskCacheMB: Int = 200) {
        if (isInitialized) return

        val appContext = context.applicationContext
        // -------------------------------
        // 永久内存缓存，LruCache 按 Bitmap.byteCount 计算大小
        pinnedMemoryCache = object : LruCache<String, Bitmap>(pinnedMemoryMB * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        }
        // -------------------------------
        // 永久磁盘缓存 (LRU 管理)
        pinnedDiskCache = DiskCache.Builder()
            .directory(File(appContext.cacheDir, "pinned_disk_cache"))
            .maxSizeBytes(pinnedDiskMB * 1024L * 1024L)
            .build()
        // -------------------------------
        // Coil 缓存
        val coilDiskCache = DiskCache.Builder()
            .directory(File(appContext.cacheDir, "coil_image_cache"))
            .maxSizeBytes(diskCacheMB * 1024L * 1024L)
            .build()
        val loader = ImageLoader.Builder(appContext)
            .crossfade(true) // 渐入动画

            .components {
                add(OkHttpNetworkFetcherFactory()) // 注册 OkHttp 网络加载器
                add(coil3.gif.GifDecoder.Factory()) // 注册 GIF 解码器
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(appContext, memoryCachePercent)
                    .strongReferencesEnabled(true) // 启用强引用，提高缓存稳定性
                    .build()
            }
            .diskCache { coilDiskCache }
            .build()
        imageLoader = loader
        // 集成到 Coil 单例加载器
        coil3.SingletonImageLoader.setSafe { loader }
        isInitialized = true
    }

    /** 获取 Coil ImageLoader */
    fun getImageLoader(): ImageLoader = imageLoader ?: throw IllegalStateException("CacheManager not initialized. Call init(context) first.")

    // ===============================
    // 永久内存缓存操作
    fun putPinnedMemory(key: String, bitmap: Bitmap) {
        pinnedMemoryCache?.put(key, bitmap)
    }

    fun getPinnedMemory(key: String): Bitmap? = pinnedMemoryCache?.get(key)
    
    fun clearPinnedMemory() {
        pinnedMemoryCache?.evictAll()
    }
    // 存储到磁盘
    fun putPinnedDisk(key: String, bitmap: Bitmap) {
        val diskCache = pinnedDiskCache ?: return
        scope.launch {
            val editor = diskCache.openEditor(key.md5()) ?: return@launch
            try {
                // Coil 3 使用 Okio FileSystem 管理文件
                val softwareBitmap = bitmap.ensureSoftware()
                var success = false
                diskCache.fileSystem.write(editor.data) {
                    outputStream().use { os ->
                        // 增加宽高判定，以及成功状态判定，防止保存空数据或损坏数据
                        if (softwareBitmap.width > 0 && softwareBitmap.height > 0) {
                            success = if (softwareBitmap.hasAlpha()) {
                                softwareBitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                            } else {
                                softwareBitmap.compress(Bitmap.CompressFormat.JPEG, 85, os)
                            }
                        }
                    }
                }
                if (success) {
                    editor.commit()
                } else {
                    editor.abort()
                }
            } catch (e: Exception) {
                editor.abort()
                e.printStackTrace()
            }
        }
    }

    /** 确保位图是软件位图，以便进行压缩或像素操作 (针对 Coil 3 的 Hardware Bitmap) */
    private fun Bitmap.ensureSoftware(): Bitmap {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE) {
            return copy(Bitmap.Config.ARGB_8888, false)
        }
        return this
    }
    /**
     * 获取磁盘缓存
     * 使用 Coil 3 的 FileSystem 进行读取，确保兼容性
     */
    fun getPinnedDisk(key: String): Bitmap? {
        val diskCache = pinnedDiskCache ?: return null
        val snapshot = diskCache.openSnapshot(key.md5()) ?: return null
        return try {
            diskCache.fileSystem.read(snapshot.data) {
                android.graphics.BitmapFactory.decodeStream(inputStream())
            }
        } catch (e: Exception) {
            null
        } finally {
            snapshot.close()
        }
    }
    /** 清空磁盘缓存 */
    fun clearPinnedDisk() {
        scope.launch {
            pinnedDiskCache?.clear()
        }
    }

    // ===============================
    // Coil 内存/磁盘缓存清理
    fun clearMemoryCache() {
        imageLoader?.memoryCache?.clear()
    }

    fun clearDiskCache() {
        scope.launch {
            imageLoader?.diskCache?.clear()
        }
    }

    /** 清理所有缓存：永久内存/磁盘 + Coil 内存/磁盘 */
    fun clearAllCache() {
        clearPinnedMemory()
        clearPinnedDisk()
        clearMemoryCache()
        clearDiskCache()
    }

    // ===============================
    // 工具方法
    /** String -> MD5，用于生成唯一文件名 */
    private fun String.md5(): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val result = digest.digest(toByteArray())
            result.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            this.hashCode().toString()
        }
    }

    /**
     * 等待 ImageView 测量完成后生成 Bitmap
     * 避免第一次加载模糊
     */
    fun ImageView.postBitmap(drawableBitmap: Bitmap, onReady: (Bitmap) -> Unit) {
        if (width > 0 && height > 0) {
            onReady(drawableBitmap.resize(width, height))
        } else {
            post {
                onReady(drawableBitmap.resize(width.coerceAtLeast(1), height.coerceAtLeast(1)))
            }
        }
    }

    /** 按 ImageView 尺寸缩放 Bitmap */
    private fun Bitmap.resize(targetWidth: Int, targetHeight: Int): Bitmap {
        if (width == targetWidth && height == targetHeight) return this
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }
}
