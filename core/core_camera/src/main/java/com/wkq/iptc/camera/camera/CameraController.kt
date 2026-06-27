package com.wkq.iptc.camera.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.Executor

/**
 * CameraX 相机控制器。
 *
 * 负责绑定预览、拍照、闪光灯和资源释放，调用方需要在页面销毁时调用 [release]。
 *
 * @param context 上下文，内部会用于获取 CameraProvider 和主线程 Executor。
 */
class CameraController(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    /**
     * 绑定相机预览和拍照用例。
     *
     * @param lifecycleOwner 生命周期宿主，通常为 Activity 或 Fragment。
     * @param previewView CameraX 预览控件。
     * @param lensFacing 镜头方向，默认后置。
     * @param flashMode 拍照闪光灯模式。
     * @param onBound 绑定成功回调。
     * @param onError 绑定失败回调。
     */
    fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        flashMode: Int = ImageCapture.FLASH_MODE_OFF,
        onBound: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(flashMode)
                    .build()
                val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                onBound?.invoke()
            } catch (t: Throwable) {
                onError?.invoke(t)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 拍摄一张照片并保存到指定文件。
     *
     * @param outputFile 输出文件。
     * @param executor 图片保存回调执行线程。
     * @param onSuccess 保存成功回调，返回实际输出文件。
     * @param onError 拍照或保存失败回调。
     */
    fun takePhoto(
        outputFile: File,
        executor: Executor,
        onSuccess: (File) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError(IllegalStateException("Camera is not ready yet"))
            return
        }
        val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        capture.takePicture(
            options,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onSuccess(outputFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    /**
     * 动态设置拍照闪光灯模式。
     *
     * @param flashMode CameraX ImageCapture 闪光灯模式。
     */
    fun setFlashMode(flashMode: Int) {
        imageCapture?.flashMode = flashMode
    }

    /**
     * 开启或关闭手电筒补光。
     *
     * @param enabled true 表示开启，false 表示关闭。
     */
    fun setTorchEnabled(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    /**
     * 判断当前镜头是否支持闪光灯/手电筒。
     *
     * @return true 表示可用。
     */
    fun isTorchAvailable(): Boolean {
        return camera?.cameraInfo?.hasFlashUnit() == true
    }

    /**
     * 释放相机资源。
     *
     * 页面销毁时必须调用，避免 CameraX 继续持有 Surface 或 Lifecycle。
     */
    fun release() {
        imageCapture = null
        camera = null
        cameraProvider?.unbindAll()
        cameraProvider = null
    }
}
