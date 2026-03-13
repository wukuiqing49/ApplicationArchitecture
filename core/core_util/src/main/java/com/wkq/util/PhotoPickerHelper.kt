package com.wkq.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 媒体资源实体类
 */
data class MediaResource(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val dateAdded: Long,
    val duration: Long = 0
)

/**
 * 排序类型枚举
 */
enum class MediaSortType {
    DATE_DESC, // 最新优先
    DATE_ASC,  // 最旧优先
    SIZE_DESC, // 最大优先
    SIZE_ASC,  // 最小优先
    NAME_ASC,  // 名称 A-Z
    DURATION_DESC, // 时长最长优先
    DURATION_ASC,  // 时长最短优先
    MODIFIED_DESC, // 修改时间最新优先
    MODIFIED_ASC   // 修改时间最旧优先
}

/**
 * 媒体选择类型枚举
 */
enum class PickMediaType {
    IMAGE_ONLY,
    VIDEO_ONLY,
    IMAGE_AND_VIDEO
}

/**
 * Android Photo Picker 及媒体查询工具类
 * 
 * 核心功能：
 * 1. 封装系统级 Photo Picker (支持 API 24-36)
 * 2. 提供基于 MediaStore 的媒体库查询 (支持自定义筛选大小、排序)
 * 3. 结果后期验证 (大小限制等)
 *
 * @author Antigravity
 */
class PhotoPickerHelper private constructor(
    private val activity: ComponentActivity? = null,
    private val fragment: Fragment? = null
) {
    private val context: Context
        get() = activity ?: fragment?.requireContext() ?: throw IllegalStateException("Context not available")

    private var singleLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var multipleLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var takePhotoLauncher: ActivityResultLauncher<Uri>? = null
    private var recordVideoLauncher: ActivityResultLauncher<Uri>? = null
    private var onResult: ((List<Uri>) -> Unit)? = null

    // 拍照/录像时的临时存取 Uri
    private var cameraUri: Uri? = null

    // 用于结果过滤的配置
    private var maxFileSize: Long = Long.MAX_VALUE
    private var filterAfterPick: Boolean = false

    companion object {
        fun with(activity: ComponentActivity) = PhotoPickerHelper(activity = activity)
        fun with(fragment: Fragment) = PhotoPickerHelper(fragment = fragment)
    }

    /**
     * 注册选择器，必须在 Activity/Fragment 的初始化阶段调用 (onCreate 或初始化块)
     * 
     * @param maxItems 多选时的最大数量
     * @param onSelected 选择成功的回调
     */
    fun register(maxItems: Int = 9, onSelected: (List<Uri>) -> Unit): PhotoPickerHelper {
        this.onResult = onSelected
        val registry = activity ?: fragment ?: return this

        // 注册单选
        singleLauncher = when (registry) {
            is ComponentActivity -> registry.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                processResult(uri?.let { listOf(it) } ?: emptyList())
            }
            is Fragment -> registry.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                processResult(uri?.let { listOf(it) } ?: emptyList())
            }
            else -> null
        }

        // 注册多选
        multipleLauncher = when (registry) {
            is ComponentActivity -> registry.registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems)) { uris ->
                processResult(uris)
            }
            is Fragment -> registry.registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems)) { uris ->
                processResult(uris)
            }
            else -> null
        }

        // 注册拍照
        takePhotoLauncher = when (registry) {
            is ComponentActivity -> registry.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) cameraUri?.let { processResult(listOf(it)) }
                else onResult?.invoke(emptyList())
            }
            is Fragment -> registry.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) cameraUri?.let { processResult(listOf(it)) }
                else onResult?.invoke(emptyList())
            }
            else -> null
        }

        // 注册录像
        recordVideoLauncher = when (registry) {
            is ComponentActivity -> registry.registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
                if (success) cameraUri?.let { processResult(listOf(it)) }
                else onResult?.invoke(emptyList())
            }
            is Fragment -> registry.registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
                if (success) cameraUri?.let { processResult(listOf(it)) }
                else onResult?.invoke(emptyList())
            }
            else -> null
        }

        return this
    }

    /**
     * 打开摄像机拍照
     * 
     * @param customUri 可选，指定拍照结果保存的 Uri。如果不传则自动在外部缓存目录生成。
     */
    fun launchCamera(customUri: Uri? = null) {
        val uri = customUri ?: createTempUri("IMG_${System.currentTimeMillis()}.jpg")
        this.cameraUri = uri
        takePhotoLauncher?.launch(uri)
    }

    /**
     * 打开摄像机录制视频
     * 
     * @param customUri 可选，指定视频保存的 Uri。如果不传则自动在外部缓存目录生成。
     */
    fun launchVideoCamera(customUri: Uri? = null) {
        val uri = customUri ?: createTempUri("VIDEO_${System.currentTimeMillis()}.mp4")
        this.cameraUri = uri
        recordVideoLauncher?.launch(uri)
    }

    /**
     * 创建临时文件的 Uri
     */
    private fun createTempUri(fileName: String): Uri {
        val pickerDir = context.getDir("picker", Context.MODE_PRIVATE)
        val file = java.io.File(pickerDir, fileName)
        
        // 兼容 Android 7.0+ 的 FileProvider
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val authority = "${context.packageName}.fileprovider"
            androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        } else {
            Uri.fromFile(file)
        }
    }

    /**
     * 设置结果过滤器（系统 Picker UI 无法实时限制大文件，可在此设置后期过滤）
     * 
     * @param maxSize 最大文件大小 (Byte)
     * @param autoFilter 是否在回调前自动过滤不符合条件的文件
     */
    fun setFilter(maxSize: Long, autoFilter: Boolean = true): PhotoPickerHelper {
        this.maxFileSize = maxSize
        this.filterAfterPick = autoFilter
        return this
    }

    /**
     * 启动选择器
     * 
     * @param type 选择类型：图片、视频或两者混选
     * @param isMultiple 是否开启多选
     */
    fun launch(type: PickMediaType = PickMediaType.IMAGE_AND_VIDEO, isMultiple: Boolean = false) {
        val request = PickVisualMediaRequest(
            when (type) {
                PickMediaType.IMAGE_ONLY -> ActivityResultContracts.PickVisualMedia.ImageOnly
                PickMediaType.VIDEO_ONLY -> ActivityResultContracts.PickVisualMedia.VideoOnly
                PickMediaType.IMAGE_AND_VIDEO -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
            }
        )
        if (isMultiple) {
            multipleLauncher?.launch(request)
        } else {
            singleLauncher?.launch(request)
        }
    }

    /**
     * 处理结果，应用后期过滤逻辑
     */
    private fun processResult(uris: List<Uri>) {
        if (uris.isEmpty()) {
            onResult?.invoke(emptyList())
            return
        }
        
        if (filterAfterPick && maxFileSize < Long.MAX_VALUE) {
            val filtered = uris.filter { uri ->
                getFileSize(uri) <= maxFileSize
            }
            onResult?.invoke(filtered)
        } else {
            onResult?.invoke(uris)
        }
    }

    /**
     * 获取文件大小
     */
    fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    if (idx != -1) cursor.getLong(idx) else 0L
                } else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 使用 MediaStore 直接查询媒体库数据
     * 该方法支持：
     * 1. 混合筛选
     * 2. 实时大小限制 (数据库级别筛选)
     * 3. 多种排序模式
     * 
     * 适用于需要自定义选择 UI 的场景
     */
    /**
     * 使用 MediaStore 直接查询媒体库数据
     * 
     * 注意：使用此方法必须已获得存储权限：
     * - API < 33: READ_EXTERNAL_STORAGE
     * - API >= 33: READ_MEDIA_IMAGES, READ_MEDIA_VIDEO
     * 
     * @param type 选择类型
     * @param maxSize 最大文件大小 (Byte)
     * @param minSize 最小文件大小 (Byte)
     * @param sortBy 排序方式
     */
    suspend fun queryMediaList(
        type: PickMediaType = PickMediaType.IMAGE_AND_VIDEO,
        maxSize: Long = Long.MAX_VALUE,
        minSize: Long = 0,
        sortBy: MediaSortType = MediaSortType.DATE_DESC
    ): List<MediaResource> = withContext(Dispatchers.IO) {
        val allMedia = mutableListOf<MediaResource>()

        // 分别查询图片和视频，以保证字段获取的准确性（Files 视图下可能缺失 duration）
        if (type == PickMediaType.IMAGE_ONLY || type == PickMediaType.IMAGE_AND_VIDEO) {
            allMedia.addAll(fetchMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, maxSize, minSize))
        }
        if (type == PickMediaType.VIDEO_ONLY || type == PickMediaType.IMAGE_AND_VIDEO) {
            allMedia.addAll(fetchMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, maxSize, minSize))
        }

        // 在内存中排序
        when (sortBy) {
            MediaSortType.DATE_DESC -> allMedia.sortByDescending { it.dateAdded }
            MediaSortType.DATE_ASC -> allMedia.sortBy { it.dateAdded }
            MediaSortType.SIZE_DESC -> allMedia.sortByDescending { it.size }
            MediaSortType.SIZE_ASC -> allMedia.sortBy { it.size }
            MediaSortType.NAME_ASC -> allMedia.sortBy { it.name }
            MediaSortType.DURATION_DESC -> allMedia.sortByDescending { it.duration }
            MediaSortType.DURATION_ASC -> allMedia.sortBy { it.duration }
            MediaSortType.MODIFIED_DESC -> allMedia.sortByDescending { it.dateAdded } // 简便处理
            MediaSortType.MODIFIED_ASC -> allMedia.sortBy { it.dateAdded }
        }

        allMedia
    }

    private fun fetchMedia(
        contentUri: Uri,
        isVideo: Boolean,
        maxSize: Long,
        minSize: Long
    ): List<MediaResource> {
        val list = mutableListOf<MediaResource>()
        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED
        )
        if (isVideo) projection.add(MediaStore.Video.VideoColumns.DURATION)

        val selection = "${MediaStore.MediaColumns.SIZE} >= ? AND ${MediaStore.MediaColumns.SIZE} <= ?"
        val args = arrayOf(minSize.toString(), maxSize.toString())

        try {
            context.contentResolver.query(
                contentUri,
                projection.toTypedArray(),
                selection,
                args,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val dateIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val durationIdx = if (isVideo) cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION) else -1

                while (cursor.moveToNext()) {
                    val id = if (idIdx != -1) cursor.getLong(idIdx) else -1L
                    if (id == -1L) continue

                    val uri = ContentUris.withAppendedId(contentUri, id)
                    list.add(
                        MediaResource(
                            uri = uri,
                            name = if (nameIdx != -1) cursor.getString(nameIdx) ?: "" else "",
                            size = if (sizeIdx != -1) cursor.getLong(sizeIdx) else 0L,
                            mimeType = if (mimeIdx != -1) cursor.getString(mimeIdx) ?: "" else "",
                            dateAdded = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L,
                            duration = if (durationIdx != -1) cursor.getLong(durationIdx) else 0L
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * 将 Uri 转换为 File (支持相册 Uri、File Uri 等)
     * 内部使用 FileUtil 的通用实现
     * 
     * @param destPath 可选，指定存储文件夹路径。若为 null，默认存储在内部存储的 picker 目录下。
     */
    fun uriToFile(uri: Uri, destPath: String? = null): java.io.File? {
        return FileUtil.uriToFile(context, uri, destPath)
    }
}
