package com.wkq.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * 设备硬件信息工具类
 */
object DeviceUtil {

    /**
     * 获取设备型号 (如 iPhone 14, SM-G9810 等)
     */
    val deviceModel: String
        get() = Build.MODEL

    /**
     * 获取设备厂商 (如 Samsung, Xiaomi, Apple 等)
     */
    val deviceManufacturer: String
        get() = Build.MANUFACTURER

    /**
     * 获取系统版本字符串 (如 "12", "13", "14")
     */
    val osVersion: String
        get() = Build.VERSION.RELEASE

    /**
     * 获取系统 SDK 版本号 (如 31, 33, 34)
     */
    val sdkVersion: Int
        get() = Build.VERSION.SDK_INT

    /**
     * 获取设备唯一 Android ID
     * @param context 上下文
     * @return 设备的 Android ID，如果获取失败则返回空字符串
     */
    @SuppressLint("HardwareIds")
    fun getAndroidId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
