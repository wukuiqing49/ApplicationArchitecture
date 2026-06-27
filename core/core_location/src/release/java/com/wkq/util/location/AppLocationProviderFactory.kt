package com.wkq.util.location

import android.content.Context

object AppLocationProviderFactory {

    fun create(context: Context): AppLocationProvider {
        // Release 版本完全不启用高德定位，只使用 Google 和系统原生定位。
        return FallbackAppLocationProvider(
            primary = GoogleAppLocationProvider(context),
            fallback = SystemAppLocationProvider(context)
        )
    }
}
