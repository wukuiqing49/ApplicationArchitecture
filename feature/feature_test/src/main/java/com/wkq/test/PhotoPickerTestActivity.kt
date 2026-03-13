package com.wkq.test

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.wkq.base.activity.BaseActivity
import com.wkq.test.databinding.ActivityPhotoPickerTestBinding
import com.wkq.util.MediaSortType
import com.wkq.util.PickMediaType
import com.wkq.util.PhotoPickerHelper
import com.wkq.util.ImageCompressUtil
import com.wkq.util.coil.loadFile
import kotlinx.coroutines.launch
import java.io.File

/**
 * PhotoPickerHelper 功能测试页面
 */
class PhotoPickerTestActivity : BaseActivity<ActivityPhotoPickerTestBinding>() {

    // 标记当前是否处于压缩模式测试
    private var isCompressMode = false

    private val photoPicker = PhotoPickerHelper.with(this).register(maxItems = 5) { uris ->
        if (isCompressMode) {
            handleCompressTest(uris)
        } else {
            handleSelectedUris(uris)
        }
    }

    override fun initView() {
        // 1. 单选图片
        binding.btnPickImage.setOnClickListener {
            isCompressMode = false
            photoPicker.setFilter(Long.MAX_VALUE) // 重置过滤器
            photoPicker.launch(type = PickMediaType.IMAGE_ONLY, isMultiple = false)
        }

        // 2. 多选图片及视频，并限制大小为 10MB
        binding.btnPickMulti.setOnClickListener {
            isCompressMode = false
            photoPicker.setFilter(maxSize = 10 * 1024 * 1024L, autoFilter = true)
            photoPicker.launch(type = PickMediaType.IMAGE_AND_VIDEO, isMultiple = true)
        }

        // 3. 压缩测试按钮
        binding.btnCompress.setOnClickListener {
            isCompressMode = true
            photoPicker.launch(type = PickMediaType.IMAGE_ONLY, isMultiple = false)
        }

        // 4. 拍照
        binding.btnTakePhoto.setOnClickListener {
            isCompressMode = false
            photoPicker.launchCamera()
        }

        // 5.录像
        binding.btnRecordVideo.setOnClickListener {
            isCompressMode = false
            photoPicker.launchVideoCamera()
        }

        // 6. 按大小排序查询 MediaStore
        binding.btnQuerySize.setOnClickListener {
            lifecycleScope.launch {
                val list = photoPicker.queryMediaList(
                    type = PickMediaType.IMAGE_AND_VIDEO,
                    sortBy = MediaSortType.SIZE_DESC
                )
                showQueryResult("按大小降序查询结果", list.take(5).map { "${it.name} (${it.size / 1024} KB)" })
            }
        }

        // 7. 按时长排序查询 MediaStore
        binding.btnQueryDuration.setOnClickListener {
            lifecycleScope.launch {
                val list = photoPicker.queryMediaList(
                    type = PickMediaType.VIDEO_ONLY,
                    sortBy = MediaSortType.DURATION_DESC
                )
                showQueryResult("按时长降序查询结果", list.take(5).map { "${it.name} (${it.duration / 1000}s)" })
            }
        }
    }

    private fun handleSelectedUris(uris: List<Uri>) {
        if (uris.isEmpty()) {
            binding.tvResult.text = "未选择任何媒体文件"
            binding.ivPreview.isVisible = false
            return
        }

        val firstUri = uris.first()
        val sb = StringBuilder("【普通模式】已选结果 (${uris.size}):\n")
        uris.forEach {
            sb.append("Uri: $it\n")
            sb.append("Size: ${photoPicker.getFileSize(it) / 1024} KB\n\n")
        }

        // --- 核心测试：Uri 转 File 并使用 Coil 加载 ---
        val file = photoPicker.uriToFile(firstUri)
        if (file != null && file.exists()) {
            sb.append("【File 转换成功】\nPath: ${file.absolutePath}\n")
            binding.ivPreview.isVisible = true
            binding.ivPreview.loadFile(file, isCircle = false, radius = 20f)
        } else {
            sb.append("【File 转换失败】\n")
            binding.ivPreview.isVisible = false
        }

        binding.tvResult.text = sb.toString()
        Toast.makeText(this, "成功获取 ${uris.size} 个文件", Toast.LENGTH_SHORT).show()
    }

    /**
     * 测试 Luban 压缩
     */
    private fun handleCompressTest(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val uri = uris.first()
        
        lifecycleScope.launch {
            val originalSize = photoPicker.getFileSize(uri) / 1024
            
            // 调用 Luban 压缩 (core_util 中的新工具)
            val result = ImageCompressUtil.compress(this@PhotoPickerTestActivity, uri, targetSizeKb = 100)
            
            result.onSuccess { compressedFile ->
                val compressedSize = compressedFile.length() / 1024
                val sb = StringBuilder("【压缩模式测试】\n")
                sb.append("原始大小: ${originalSize} KB\n")
                sb.append("压缩后大小: ${compressedSize} KB\n")
                sb.append("压缩后路径: ${compressedFile.absolutePath}\n")
                
                binding.tvResult.text = sb.toString()
                binding.ivPreview.isVisible = true
                binding.ivPreview.loadFile(compressedFile, radius = 20f)
                
                Toast.makeText(this@PhotoPickerTestActivity, "压缩成功！", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                binding.tvResult.text = "压缩失败: ${e.message}"
                binding.ivPreview.isVisible = false
                Toast.makeText(this@PhotoPickerTestActivity, "压缩失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showQueryResult(title: String, results: List<String>) {
        val sb = StringBuilder("$title:\n")
        if (results.isEmpty()) {
            sb.append("无匹配数据")
        } else {
            results.forEachIndexed { index, s ->
                sb.append("${index + 1}. $s\n")
            }
        }
        binding.tvResult.text = sb.toString()
        binding.ivPreview.isVisible = false
    }

    override fun initData() {
    }
}
