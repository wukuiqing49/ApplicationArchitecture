package com.wkq.util

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * 屏幕相关工具类
 */
object ScreenUtil {

    /**
     * 获取屏幕宽度 (像素)
     */
    fun getScreenWidth(context: Context): Int {
        return getDisplayMetrics(context).widthPixels
    }

    /**
     * 获取屏幕高度 (像素)
     */
    fun getScreenHeight(context: Context): Int {
        return getDisplayMetrics(context).heightPixels
    }

    /**
     * dp 转 px
     */
    fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * px 转 dp
     */
    fun px2dp(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    /**
     * sp 转 px
     */
    fun sp2px(context: Context, spValue: Float): Int {
        // 使用 configuration.fontScale 动态计算 scaledDensity，避免 deprecated
        val configuration = context.resources.configuration
        val density = context.resources.displayMetrics.density
        val scaledDensity = density * configuration.fontScale
        return (spValue * scaledDensity + 0.5f).toInt()
    }

    /**
     * 获取 DisplayMetrics (兼容新版 API)
     */
    private fun getDisplayMetrics(context: Context): DisplayMetrics {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            DisplayMetrics().apply {
                widthPixels = bounds.width()
                heightPixels = bounds.height()
                density = context.resources.displayMetrics.density
                densityDpi = context.resources.displayMetrics.densityDpi
                // 不再赋值 scaledDensity，sp2px 动态计算即可
            }
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            metrics
        }
    }
}