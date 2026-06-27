package com.wkq.util.location

import android.content.Context

object AppLocationProviderFactory {

    fun create(context: Context): AppLocationProvider {
        // Debug 版本优先使用高德定位，失败后回退到 Google 和系统原生定位。
        return FallbackAppLocationProvider(
            primary = AMapAppLocationProvider(context),
            fallback = FallbackAppLocationProvider(
                primary = GoogleAppLocationProvider(context),
                fallback = SystemAppLocationProvider(context)
            )
        )
    }
}
