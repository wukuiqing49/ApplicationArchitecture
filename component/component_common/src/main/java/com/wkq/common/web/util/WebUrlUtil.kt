package com.wkq.common.web.util

import android.content.Context
import android.net.Uri
import android.os.Build
import com.wkq.user.manager.UserManager

/**
 * WebUrlUtil: Web URL 参数拼接工具类
 */
object WebUrlUtil {

    /**
     * 为 URL 拼接通用参数
     * @param url 原始 URL
     * @param context 上下文 (用于获取版本号)
     * @param extraParams 额外的自定义参数
     * @return 拼接后的 URL
     */
    fun appendCommonParams(
        url: String?,
        context: Context,
        extraParams: Map<String, String>? = null
    ): String {
        if (url.isNullOrBlank()) return ""

        val builder = try {
            Uri.parse(url).buildUpon()
        } catch (e: Exception) {
            return url
        }

        // 1. 基础平台信息
        builder.appendQueryParameter("platform", "android")
        builder.appendQueryParameter("os_version", Build.VERSION.RELEASE)
        builder.appendQueryParameter("device_model", Build.MODEL)

        // 2. 版本号
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            builder.appendQueryParameter("version_name", packageInfo.versionName)
            builder.appendQueryParameter("version_code", packageInfo.versionCode.toString())
        } catch (e: Exception) {
            // 忽略版本获取失败
        }

        // 3. 用户 Token
        try {
            // 从 UserManager 获取当前登录用户的 Token
            val token = UserManager.getInstance().currentUserFlow.value?.token
            if (!token.isNullOrBlank()) {
                builder.appendQueryParameter("token", token)
            }
        } catch (e: Exception) {
            // UserManager 可能未初始化或处于未登录状态
        }

        // 4. 时间戳 (防缓存)
        builder.appendQueryParameter("_t", System.currentTimeMillis().toString())

        // 5. 额外参数
        extraParams?.forEach { (key, value) ->
            builder.appendQueryParameter(key, value)
        }

        return builder.build().toString()
    }
}
