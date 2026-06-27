package com.wkq.util.location

import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log

class FallbackAppLocationProvider(
    private val primary: AppLocationProvider,
    private val fallback: AppLocationProvider
) : AppLocationProvider {

    private val mainHandler = Handler(Looper.getMainLooper())

    override val lastLocation: Location?
        get() = primary.lastLocation ?: fallback.lastLocation

    override fun start(
        onLocationChanged: (Location) -> Unit,
        onLocationUnavailable: () -> Unit
    ) {
        Log.d(TAG, "start: primary=${primary.javaClass.simpleName}, fallback=${fallback.javaClass.simpleName}")
        var delivered = false
        var fallbackStarted = false

        fun deliverLocation(location: Location) {
            delivered = true
            mainHandler.removeCallbacksAndMessages(FALLBACK_START_TOKEN)
            Log.d(TAG, "deliverLocation: provider=${location.provider}, lat=${location.latitude}, lon=${location.longitude}, acc=${if (location.hasAccuracy()) location.accuracy else -1f}")
            onLocationChanged(location)
        }

        fun startFallback() {
            if (delivered || fallbackStarted) return
            fallbackStarted = true
            Log.d(TAG, "startFallback: switching to ${fallback.javaClass.simpleName}")
            fallback.start(
                onLocationChanged = ::deliverLocation,
                onLocationUnavailable = {
                    if (!delivered) {
                        Log.d(TAG, "startFallback: fallback unavailable")
                        onLocationUnavailable()
                    }
                }
            )
        }

        Log.d(TAG, "start: schedule fallback timeout=${PRIMARY_TIMEOUT_MS}ms")
        mainHandler.postAtTime({ startFallback() }, FALLBACK_START_TOKEN, System.currentTimeMillis() + PRIMARY_TIMEOUT_MS)

        primary.start(
            onLocationChanged = ::deliverLocation,
            onLocationUnavailable = {
                Log.d(TAG, "start: primary unavailable, trigger fallback")
                startFallback()
            }
        )
    }

    override fun stop() {
        Log.d(TAG, "stop")
        mainHandler.removeCallbacksAndMessages(FALLBACK_START_TOKEN)
        primary.stop()
        fallback.stop()
    }

    override fun requestSingleUpdate(
        onLocationChanged: (Location) -> Unit,
        onLocationUnavailable: () -> Unit
    ) {
        Log.d(TAG, "requestSingleUpdate: primary=${primary.javaClass.simpleName}, fallback=${fallback.javaClass.simpleName}")
        var delivered = false

        fun deliverLocation(location: Location) {
            if (delivered) return
            delivered = true
            mainHandler.removeCallbacksAndMessages(FALLBACK_SINGLE_TOKEN)
            Log.d(TAG, "requestSingleUpdate deliver: provider=${location.provider}, lat=${location.latitude}, lon=${location.longitude}, acc=${if (location.hasAccuracy()) location.accuracy else -1f}")
            onLocationChanged(location)
        }

        fun requestFallback() {
            if (delivered) return
            Log.d(TAG, "requestSingleUpdate: switching to fallback")
            fallback.requestSingleUpdate(
                onLocationChanged = ::deliverLocation,
                onLocationUnavailable = {
                    if (!delivered) {
                        delivered = true
                        Log.d(TAG, "requestSingleUpdate: fallback unavailable")
                        onLocationUnavailable()
                    }
                }
            )
        }

        mainHandler.postAtTime(
            { requestFallback() },
            FALLBACK_SINGLE_TOKEN,
            System.currentTimeMillis() + PRIMARY_TIMEOUT_MS
        )

        primary.requestSingleUpdate(
            onLocationChanged = ::deliverLocation,
            onLocationUnavailable = {
                Log.d(TAG, "requestSingleUpdate: primary unavailable, trigger fallback")
                requestFallback()
            }
        )
    }

    private companion object {
        private const val TAG = "FieldLocationFallback"
        private const val PRIMARY_TIMEOUT_MS = 2_000L
        private val FALLBACK_START_TOKEN = Any()
        private val FALLBACK_SINGLE_TOKEN = Any()
    }
}
