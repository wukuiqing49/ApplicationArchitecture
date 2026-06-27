package com.wkq.util.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import android.util.Log

class AMapAppLocationProvider(
    context: Context
) : AppLocationProvider {

    private val appContext = context.applicationContext
    private var locationClient: AMapLocationClient? = null
    private var continuousListener: AMapLocationListener? = null

    override var lastLocation: Location? = null
        private set

    @SuppressLint("MissingPermission")
    override fun start(
        onLocationChanged: (Location) -> Unit,
        onLocationUnavailable: () -> Unit
    ) {
        if (!hasAnyLocationPermission()) {
            Log.d(TAG, "start: no location permission")
            onLocationUnavailable()
            return
        }
        if (!hasApiKey()) {
            Log.d(TAG, "start: missing amap api key")
            onLocationUnavailable()
            return
        }
        if (continuousListener != null) {
            Log.d(TAG, "start: listener already active")
            return
        }
        val client = obtainClient() ?: run {
            onLocationUnavailable()
            return
        }
        val listener = AMapLocationListener { location ->
            handleLocationResult(location, onLocationChanged, onLocationUnavailable)
        }
        continuousListener = listener
        client.setLocationListener(listener)
        client.setLocationOption(
            AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isNeedAddress = false
                interval = 1_000L
                isMockEnable = false
                isLocationCacheEnable = true
                httpTimeOut = 8_000L
            }
        )
        runCatching {
            client.startLocation()
            Log.d(TAG, "start: continuous location started")
        }.onFailure {
            Log.e(TAG, "start: failed ${it.message.orEmpty()}")
            continuousListener = null
            onLocationUnavailable()
        }
    }

    override fun stop() {
        Log.d(TAG, "stop")
        val listener = continuousListener
        if (listener != null) {
            locationClient?.unRegisterLocationListener(listener)
            continuousListener = null
        }
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        locationClient = null
    }

    @SuppressLint("MissingPermission")
    override fun requestSingleUpdate(
        onLocationChanged: (Location) -> Unit,
        onLocationUnavailable: () -> Unit
    ) {
        if (!hasAnyLocationPermission()) {
            Log.d(TAG, "requestSingleUpdate: no location permission")
            onLocationUnavailable()
            return
        }
        if (!hasApiKey()) {
            Log.d(TAG, "requestSingleUpdate: missing amap api key")
            onLocationUnavailable()
            return
        }
        val client = obtainClient() ?: run {
            onLocationUnavailable()
            return
        }
        val listener = object : AMapLocationListener {
            override fun onLocationChanged(location: AMapLocation?) {
                client.unRegisterLocationListener(this)
                handleLocationResult(location, onLocationChanged, onLocationUnavailable)
            }
        }
        client.setLocationListener(listener)
        client.setLocationOption(
            AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isOnceLocation = true
                isOnceLocationLatest = true
                isNeedAddress = false
                isMockEnable = false
                isLocationCacheEnable = true
                httpTimeOut = 8_000L
            }
        )
        runCatching {
            client.startLocation()
            Log.d(TAG, "requestSingleUpdate: single location started")
        }.onFailure {
            client.unRegisterLocationListener(listener)
            Log.e(TAG, "requestSingleUpdate: failed ${it.message.orEmpty()}")
            onLocationUnavailable()
        }
    }

    private fun obtainClient(): AMapLocationClient? {
        locationClient?.let { return it }
        return runCatching {
            AMapLocationClient.updatePrivacyShow(appContext, true, true)
            AMapLocationClient.updatePrivacyAgree(appContext, true)
            readApiKey()?.let(AMapLocationClient::setApiKey)
            AMapLocationClient(appContext).also { locationClient = it }
        }.onFailure {
            Log.e(TAG, "obtainClient: init failed ${it.message.orEmpty()}")
        }.getOrNull()
    }

    private fun handleLocationResult(
        location: AMapLocation?,
        onLocationChanged: (Location) -> Unit,
        onLocationUnavailable: () -> Unit
    ) {
        if (location == null) {
            Log.d(TAG, "handleLocationResult: location null")
            onLocationUnavailable()
            return
        }
        if (location.errorCode != 0) {
            Log.e(
                TAG,
                "handleLocationResult: errorCode=${location.errorCode}, info=${location.errorInfo.orEmpty()}"
            )
            onLocationUnavailable()
            return
        }
        val androidLocation = location.toAndroidLocation()
        lastLocation = androidLocation
        Log.d(
            TAG,
            "handleLocationResult: provider=${androidLocation.provider}, lat=${androidLocation.latitude}, lon=${androidLocation.longitude}, acc=${if (androidLocation.hasAccuracy()) androidLocation.accuracy else -1f}"
        )
        onLocationChanged(androidLocation)
    }

    private fun AMapLocation.toAndroidLocation(): Location {
        return Location(provider ?: "amap").apply {
            latitude = this@toAndroidLocation.latitude
            longitude = this@toAndroidLocation.longitude
            if (this@toAndroidLocation.hasAccuracy()) {
                accuracy = this@toAndroidLocation.accuracy
            }
            if (this@toAndroidLocation.altitude != 0.0) {
                altitude = this@toAndroidLocation.altitude
            }
            if (this@toAndroidLocation.bearing != 0f) {
                bearing = this@toAndroidLocation.bearing
            }
            if (this@toAndroidLocation.speed != 0f) {
                speed = this@toAndroidLocation.speed
            }
            time = this@toAndroidLocation.time.takeIf { it > 0L } ?: System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            extras = Bundle().apply {
                putInt("amap_error_code", this@toAndroidLocation.errorCode)
                putInt("amap_location_type", this@toAndroidLocation.locationType)
            }
        }
    }

    private fun hasAnyLocationPermission(): Boolean {
        return hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(appContext, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasApiKey(): Boolean {
        return !readApiKey().isNullOrBlank()
    }

    private fun readApiKey(): String? {
        return runCatching {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.packageManager.getApplicationInfo(
                    appContext.packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getApplicationInfo(
                    appContext.packageName,
                    PackageManager.GET_META_DATA
                )
            }
            val key = appInfo.metaData?.getString(AMAP_API_KEY_META)?.trim()
            Log.d(TAG, "readApiKey: key length = ${key?.length ?: 0}, value = $key")
            key?.takeIf { it.isNotBlank() }
        }.onFailure {
            Log.e(TAG, "readApiKey: failed to read amap key ${it.message.orEmpty()}")
        }.getOrNull()
    }

    private companion object {
        private const val TAG = "FieldLocationAMap"
        private const val AMAP_API_KEY_META = "com.amap.api.v2.apikey"
    }
}
