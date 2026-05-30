package com.wkq.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import es.dmoral.toasty.Toasty

/**
 * 安全工具扩展。
 */
fun Context.showToast(msg: String?) {
    if (this.isFinishing()) return
    if (!msg.isNullOrEmpty()) {
        Toasty.Config.getInstance()
            .allowQueue(false)
            .apply()
        Toasty.normal(this, msg).show()
    }
}

fun Fragment.showToast(msg: String?) {
    if (context.isFinishing()) return
    context?.let {
        if (!msg.isNullOrEmpty()) {
            Toasty.Config.getInstance()
                .allowQueue(false)
                .apply()
            Toasty.normal(it, msg).show()
        }
    }
}

fun View.showToast(msg: String?) {
    if (context.isFinishing()) return
    context?.let {
        if (!msg.isNullOrEmpty()) {
            Toasty.Config.getInstance()
                .allowQueue(false)
                .apply()
            Toasty.normal(it, msg).show()
        }
    }
}

/**
 * 安全判断 Context 关联的 Activity 是否处于无效状态。
 */
fun Context?.isFinishing(): Boolean {
    if (this == null) {
        return true
    }

    val activity = this.unwrapActivity() ?: return true
    return activity.isFinishing || activity.isDestroyed
}

/**
 * 从 Context 中解析出底层的 Activity。
 */
private fun Context.unwrapActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}

/**
 * 安全获取字符串资源，支持格式化参数，失败返回默认值。
 */
fun Context?.safeString(resId: Int, vararg formatArgs: Any, default: String = ""): String {
    if (this == null || resId == 0) return default
    return try {
        if (formatArgs.isEmpty()) {
            getString(resId)
        } else {
            getString(resId, *formatArgs)
        }
    } catch (_: Exception) {
        default
    }
}

/**
 * 安全获取 Fragment 的字符串资源，失败返回默认值。
 */
fun Fragment?.safeString(resId: Int, vararg formatArgs: Any, default: String = ""): String {
    return this?.context.safeString(resId, *formatArgs, default = default)
}

/**
 * 安全获取 View 的字符串资源，失败返回默认值。
 */
fun View?.safeString(resId: Int, vararg formatArgs: Any, default: String = ""): String {
    return this?.context.safeString(resId, *formatArgs, default = default)
}

/**
 * 安全获取颜色资源，失败返回默认颜色。
 */
fun Context?.safeColor(@ColorRes resIdColor: Int, defaultColor: Int = Color.TRANSPARENT): Int {
    if (this == null || resIdColor == 0) return defaultColor
    return try {
        ContextCompat.getColor(this, resIdColor)
    } catch (_: Exception) {
        defaultColor
    }
}

/**
 * 安全获取 Fragment 的颜色资源，失败返回默认颜色。
 */
fun Fragment?.safeColor(
    @ColorRes resIdColor: Int,
    defaultColor: Int = Color.TRANSPARENT
): Int {
    return this?.context.safeColor(resIdColor, defaultColor) ?: defaultColor
}

/**
 * 安全获取 View 的颜色资源，失败返回默认颜色。
 */
fun View?.safeColor(
    @ColorRes resIdColor: Int,
    defaultColor: Int = Color.TRANSPARENT
): Int {
    return this?.context.safeColor(resIdColor, defaultColor) ?: defaultColor
}
/**
 * 处理误触
 */
fun View.setSafeClickListener(interval: Long = 500L, action: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > interval) {
            lastClickTime = now
            action(it)
        }
    }
}


fun  TextView.textSafe(text: String?) {
    text?.let {
        this.text = it
    }
}

/**
 * 安全转换为 Int，支持默认值、范围限制、异常打印
 */
fun String?.toSafeInt(
    default: Int = 0,
    min: Int? = null,
    max: Int? = null,
    logError: Boolean = false
): Int {
    return try {
        val value = this?.toIntOrNull() ?: return default
        if ((min != null && value < min) || (max != null && value > max)) default else value
    } catch (e: Exception) {
        if (logError) e.printStackTrace()
        default
    }
}

fun String?.toSafeLong(
    default: Long = 0L,
    min: Long? = null,
    max: Long? = null,
    logError: Boolean = false
): Long {
    return try {
        val value = this?.toDoubleOrNull()?.toLong() ?: default ?: return default
        if ((min != null && value < min) || (max != null && value > max)) default else value
    } catch (e: Exception) {
        if (logError) e.printStackTrace()
        default
    }
}
