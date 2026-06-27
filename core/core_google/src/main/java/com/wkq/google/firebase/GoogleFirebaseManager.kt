package com.wkq.google.firebase

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.wkq.google.GoogleKit
import com.wkq.google.GoogleKitConfig

/**
 * Firebase 能力门面。
 *
 * 当前主要封装 Firebase Analytics。宿主 App 没有正确配置 google-services.json 时，
 * 本类会降级为不可用状态，业务调用不会直接崩溃。
 */
object GoogleFirebaseManager {

    @Volatile
    private var analytics: FirebaseAnalytics? = null

    @Volatile
    private var analyticsEnabled: Boolean = true

    @Volatile
    private var lastInitializeError: Throwable? = null

    /**
     * 初始化 Firebase Analytics。
     *
     * 通常由 GoogleKit.initialize 间接调用。宿主 App 需要在 app 模块配置
     * google-services 插件，并放置 google-services.json。
     */
    fun initialize(
        context: Context,
        config: GoogleKitConfig = GoogleKit.currentConfig()
    ): Boolean {
        analyticsEnabled = config.enableFirebaseAnalytics
        if (!analyticsEnabled) {
            analytics = null
            lastInitializeError = null
            return false
        }
        return runCatching {
            analytics = FirebaseAnalytics.getInstance(context.applicationContext)
            lastInitializeError = null
            true
        }.getOrElse { throwable ->
            analytics = null
            lastInitializeError = throwable
            false
        }
    }

    /** 判断 Firebase Analytics 当前是否可用。 */
    fun isAnalyticsAvailable(): Boolean {
        return analyticsEnabled && analytics != null
    }

    /** 获取最近一次 Firebase 初始化失败原因，便于 Debug 排查。 */
    fun getLastInitializeErrorMessage(): String {
        return lastInitializeError?.message.orEmpty()
    }

    /**
     * 上报自定义事件。
     *
     * @param name 事件名，建议使用小写字母、数字和下划线。
     * @param params 事件参数，仅支持 String、数字、Boolean；Boolean 会转成字符串。
     * @return true 表示已交给 Firebase Analytics，false 表示当前不可用或事件名为空。
     */
    fun logEvent(
        name: String,
        params: Map<String, Any?> = emptyMap()
    ): Boolean {
        val firebaseAnalytics = analytics ?: return false
        if (!analyticsEnabled || name.isBlank()) return false
        firebaseAnalytics.logEvent(name, params.toFirebaseBundle())
        return true
    }

    /**
     * 设置当前用户 ID。
     *
     * 登录成功后调用；退出登录时传 null 可清空用户标识。
     */
    fun setUserId(userId: String?): Boolean {
        val firebaseAnalytics = analytics ?: return false
        if (!analyticsEnabled) return false
        firebaseAnalytics.setUserId(userId)
        return true
    }

    /**
     * 设置用户属性。
     *
     * 适合记录会员状态、登录来源、用户分层等低频属性。
     */
    fun setUserProperty(name: String, value: String?): Boolean {
        val firebaseAnalytics = analytics ?: return false
        if (!analyticsEnabled || name.isBlank()) return false
        firebaseAnalytics.setUserProperty(name, value)
        return true
    }

    /**
     * 开关 Analytics 数据采集。
     *
     * 可用于隐私协议未同意前关闭采集，用户同意后再开启。
     */
    fun setAnalyticsCollectionEnabled(enabled: Boolean): Boolean {
        analyticsEnabled = enabled
        val firebaseAnalytics = analytics ?: return false
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
        return true
    }

    /**
     * 清理 Analytics 本地数据。
     *
     * 可用于用户注销账号或撤回隐私授权后的本地数据清理。
     */
    fun resetAnalyticsData(): Boolean {
        val firebaseAnalytics = analytics ?: return false
        firebaseAnalytics.resetAnalyticsData()
        return true
    }
}

private fun Map<String, Any?>.toFirebaseBundle(): Bundle {
    val bundle = Bundle()
    forEach { (key, value) ->
        if (key.isBlank() || value == null) return@forEach
        when (value) {
            is String -> bundle.putString(key, value)
            is Int -> bundle.putLong(key, value.toLong())
            is Long -> bundle.putLong(key, value)
            is Short -> bundle.putLong(key, value.toLong())
            is Byte -> bundle.putLong(key, value.toLong())
            is Double -> bundle.putDouble(key, value)
            is Float -> bundle.putDouble(key, value.toDouble())
            is Boolean -> bundle.putString(key, value.toString())
        }
    }
    return bundle
}
