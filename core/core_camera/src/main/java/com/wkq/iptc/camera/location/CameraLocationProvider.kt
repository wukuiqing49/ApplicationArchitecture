package com.wkq.iptc.camera.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.wkq.util.location.AppLocationProvider
import com.wkq.util.location.AppLocationProviderFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 相机定位提供器。
 *
 * 封装业务层定位能力，为拍照流程获取最近一次位置或单次定位结果。
 *
 * @param context 上下文，内部使用 applicationContext 创建定位提供器。
 */
class CameraLocationProvider(context: Context) {

    private val locationProvider: AppLocationProvider =
        AppLocationProviderFactory.create(context.applicationContext)

    /**
     * 获取最近一次定位。
     *
     * 最多等待 5 秒，超时或定位不可用时返回 null。
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location? = kotlinx.coroutines.withTimeoutOrNull(5000) {
        suspendCancellableCoroutine { continuation ->
            locationProvider.lastLocation?.let {
                if (continuation.isActive) continuation.resume(it)
                return@suspendCancellableCoroutine
            }
            continuation.invokeOnCancellation {
                locationProvider.stop()
            }
            locationProvider.requestSingleUpdate(
                onLocationChanged = { if (continuation.isActive) continuation.resume(it) },
                onLocationUnavailable = { if (continuation.isActive) continuation.resume(null) }
            )
        }
    }
}
