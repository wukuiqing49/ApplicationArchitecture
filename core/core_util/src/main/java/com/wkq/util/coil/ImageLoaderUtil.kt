package com.wkq.util.coil

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.request.transformations
import coil3.size.Scale
import coil3.load
import coil3.request.Disposable
import coil3.asDrawable
import coil3.transform.CircleCropTransformation
import coil3.transform.RoundedCornersTransformation
import coil3.transform.Transformation
import java.io.File

/**
 * ImageLoaderUtil
 *
 * 功能：
 * 1. 图片加载工具类，支持 URL / File / resId
 * 2. 支持普通加载和永久缓存加载
 * 3. 支持 GIF 动图加载，避免 OOM
 * 4. 自动按 ImageView 尺寸生成 Bitmap，避免第一次加载模糊
 * 5. 支持圆角、圆形、灰度等变换
 * 6. 提供 ImageView 扩展函数，使用更便捷
 */
object ImageLoaderUtil {

    /**
     * 基础加载方法
     * @return Disposable 可用于取消请求
     */
    fun load(
        imageView: ImageView,
        data: Any?,
        placeholder: Any? = null,
        error: Any? = null,
        isCircle: Boolean = false,
        isGrayscale: Boolean = false,
        radius: Float = 0f,
        scale: Scale = Scale.FILL,
        memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
        diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
        onSuccess: ((Drawable) -> Unit)? = null,
        onSuccessBitmap: ((Bitmap) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
        builder: ImageRequest.Builder.() -> Unit = {}
    ): Disposable? {
        if (data == null) return null
        
        // 使用 Coil 的单例 ImageLoader，它带有了正确的解码组件（GIF 等）
        val imageLoader = CacheManager.getImageLoader()
        
        // 直接使用扩展函数，它会自动处理 ImageView 的生命周期并设置加载结果
        return imageView.load(data, imageLoader) {
            (placeholder as? Int)?.let { placeholder(it) }
            (placeholder as? Drawable)?.let { placeholder(it) }
            (error as? Int)?.let { error(it) }
            (error as? Drawable)?.let { error(it) }
            
            scale(scale)
            crossfade(true)
            memoryCachePolicy(memoryCachePolicy)
            diskCachePolicy(diskCachePolicy)
            // 圆形圆角处理
            val transformationsList = mutableListOf<Transformation>()
            if (isCircle) {
                transformationsList.add(CircleCropTransformation())
            } else if (radius > 0) {
                transformationsList.add(RoundedCornersTransformation(radius))
            }
            // 判断灰度处理
            if (isGrayscale) {
                transformationsList.add(GrayscaleTransformation())
            }
            if (transformationsList.isNotEmpty()) {
                transformations(transformationsList)
            }

            // 这里监听加载结果，用于回调扩展
            listener(
                onSuccess = { _, result ->
                    // 从 result 中直接获取 Image，不转换 ImageView 的当前状态（解决 200B 问题）
                    val bitmap = result.image.asDrawable(imageView.resources).toBitmap()
                    
                    val currentDrawable = imageView.drawable
                    if (currentDrawable is android.graphics.drawable.Animatable) {
                        currentDrawable.start()
                    }
                    
                    // 回调图片
                    onSuccessBitmap?.invoke(bitmap)
                    
                    currentDrawable?.let { onSuccess?.invoke(it) } ?: run {
                        onSuccess?.invoke(result.image.asDrawable(imageView.resources))
                    }
                },
                onError = { _, result ->
                    onError?.invoke(result.throwable)
                }
            )
            
            apply(builder)
        }
    }

    /**
     * 永久缓存加载图片（内存 + 磁盘）
     * 适合头像、礼物图标等小尺寸但高频显示的静态资源。
     *
     * ⚠ 注意：不要传入 GIF 数据源。
     * GIF 加载成功后 toBitmap() 只会取第一帧存入缓存，
     * 下次命中缓存将展示静态图而非动画，请使用 loadGif() 代替。
     */
    fun loadPinned(
        imageView: ImageView,
        data: Any,
        placeholder: Any? = null,
        error: Any? = null,
        isCircle: Boolean = false,
        isGrayscale: Boolean = false,
        radius: Float = 0f,
        builder: ImageRequest.Builder.() -> Unit = {}
    ): Disposable? {
        val key = when (data) {
            is String -> data
            is File -> data.absolutePath
            is Int -> "res_$data"
            else -> data.toString()
        } + "_c${isCircle}_r${radius}_g${isGrayscale}"

        // 1. 先检查内存缓存
        CacheManager.getPinnedMemory(key)?.let { bitmap ->
            imageView.setImageBitmap(bitmap)
            return null
        }

        // 2. 检查磁盘缓存（返回已解码的 Bitmap，避免 Snapshot 关闭后文件路径失效）
        CacheManager.getPinnedDisk(key)?.let { bitmap ->
            imageView.setImageBitmap(bitmap)
            CacheManager.putPinnedMemory(key, bitmap) // 回写内存缓存，下次直接命中
            return null
        }

        // 3. 网络 / 本地加载，成功后写入双缓存
        return load(
            imageView = imageView,
            data = data,
            placeholder = placeholder,
            error = error,
            isCircle = isCircle,
            isGrayscale = isGrayscale,
            radius = radius,
            onSuccessBitmap = { bmp ->
                CacheManager.putPinnedMemory(key, bmp)
                if (data is String || data is File) {
                    CacheManager.putPinnedDisk(key, bmp)
                }
            },
            builder = builder
        )
    }

    /**
     * GIF 动图加载（内存缓存默认关闭，防 OOM）
     */
    fun loadGif(
        imageView: ImageView,
        data: Any,
        placeholder: Any? = null,
        error: Any? = null,
        isCircle: Boolean = false,
        radius: Float = 0f,
        scale: Scale = Scale.FILL,
        diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
        onSuccess: ((Drawable) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
        builder: ImageRequest.Builder.() -> Unit = {}
    ): Disposable? {
        return load(
            imageView = imageView,
            data = data,
            placeholder = placeholder,
            error = error,
            isCircle = isCircle,
            radius = radius,
            scale = scale,
            memoryCachePolicy = CachePolicy.DISABLED,
            diskCachePolicy = diskCachePolicy,
            onSuccess = onSuccess,
            onError = onError,
            builder = builder
        )
    }

    /**
     * 预加载图片
     */
    fun preload(
        context: android.content.Context,
        data: Any?,
        builder: ImageRequest.Builder.() -> Unit = {}
    ): Disposable? {
        if (data == null) return null
        val request = ImageRequest.Builder(context)
            .data(data)
            .apply(builder)
            .build()
        return CacheManager.getImageLoader().enqueue(request)
    }

    /**
     * 灰度变换支持
     */
    private class GrayscaleTransformation : Transformation() {
        override val cacheKey: String = "GrayscaleTransformation"
        override suspend fun transform(input: android.graphics.Bitmap, size: coil3.size.Size): android.graphics.Bitmap {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
            val colorMatrix = android.graphics.ColorMatrix()
            colorMatrix.setSaturation(0f)
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)

            // Hardware Bitmap 不能直接用 Canvas 绘制，需转为软件 Bitmap
            // HARDWARE config 仅 API 26+ 存在，低版本不会出现，直接用原始 config 即可
            val safeConfig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && input.config == Bitmap.Config.HARDWARE) {
                Bitmap.Config.ARGB_8888
            } else {
                input.config ?: Bitmap.Config.ARGB_8888
            }
            val output = android.graphics.Bitmap.createBitmap(input.width, input.height, safeConfig)
            val canvas = android.graphics.Canvas(output)
            canvas.drawBitmap(input, 0f, 0f, paint)
            return output
        }
    }
}

/**
 * ImageView 扩展函数
 */
fun ImageView.loadUrl(
    url: String?,
    placeholder: Any? = null,
    error: Any? = null,
    isCircle: Boolean = false,
    isGrayscale: Boolean = false,
    radius: Float = 0f,
    memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    onSuccess: ((Drawable) -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    builder: ImageRequest.Builder.() -> Unit = {}
) = ImageLoaderUtil.load(
    this, url, placeholder, error, isCircle, isGrayscale, radius, 
    memoryCachePolicy = memoryCachePolicy, 
    diskCachePolicy = diskCachePolicy, 
    onSuccess = onSuccess, onError = onError, builder = builder
)

fun ImageView.loadRes(
    resId: Int,
    placeholder: Any? = null,
    error: Any? = null,
    isCircle: Boolean = false,
    isGrayscale: Boolean = false,
    radius: Float = 0f,
    memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    onSuccess: ((Drawable) -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    builder: ImageRequest.Builder.() -> Unit = {}
) = ImageLoaderUtil.load(
    this, resId, placeholder, error, isCircle, isGrayscale, radius, 
    memoryCachePolicy = memoryCachePolicy, 
    diskCachePolicy = diskCachePolicy, 
    onSuccess = onSuccess, onError = onError, builder = builder
)

fun ImageView.loadFile(
    file: File?,
    placeholder: Any? = null,
    error: Any? = null,
    isCircle: Boolean = false,
    isGrayscale: Boolean = false,
    radius: Float = 0f,
    memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    onSuccess: ((Drawable) -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    builder: ImageRequest.Builder.() -> Unit = {}
) = ImageLoaderUtil.load(
    this, file, placeholder, error, isCircle, isGrayscale, radius, 
    memoryCachePolicy = memoryCachePolicy, 
    diskCachePolicy = diskCachePolicy, 
    onSuccess = onSuccess, onError = onError, builder = builder
)
