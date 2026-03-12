package com.wkq.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/**
 * 全局主题管理器（白天 / 黑夜 / 跟随系统）
 *
 * ### 使用方式
 *
 * **初始化**（在 Application.onCreate 中调用一次）：
 * ```kotlin
 * ThemeManager.init(this)
 * ```
 *
 * **切换主题**（在设置页中调用，立即生效无需重启）：
 * ```kotlin
 * ThemeManager.setMode(ThemeMode.DARK)   // 黑夜
 * ThemeManager.setMode(ThemeMode.LIGHT)  // 白天
 * ThemeManager.setMode(ThemeMode.SYSTEM) // 跟随系统（默认）
 * ```
 *
 * **获取当前模式（用于更新 UI 开关状态）**：
 * ```kotlin
 * val current = ThemeManager.currentMode  // ThemeMode 枚举
 * ```
 */
object ThemeManager {

    enum class ThemeMode(val value: Int) {
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
        DARK(AppCompatDelegate.MODE_NIGHT_YES),
        SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        companion object {
            fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: SYSTEM
        }
    }

    private const val PREF_NAME = "theme_prefs"
    private const val KEY_NIGHT_MODE = "night_mode"

    private lateinit var prefs: SharedPreferences

    /** 当前生效的主题模式 */
    val currentMode: ThemeMode
        get() = ThemeMode.fromInt(
            prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        )

    /**
     * 在 Application.onCreate() 中初始化，并恢复上次保存的主题
     */
    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        applyMode(currentMode)
    }

    /**
     * 切换主题模式，立即生效并持久化
     */
    fun setMode(mode: ThemeMode) {
        prefs.edit().putInt(KEY_NIGHT_MODE, mode.value).apply()
        applyMode(mode)
    }

    /**
     * 切换：白天 ↔ 黑夜（不经过"跟随系统"）
     */
    fun toggle() {
        val next = if (currentMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        setMode(next)
    }

    /** 是否当前为黑夜模式 */
    val isDark: Boolean
        get() = currentMode == ThemeMode.DARK

    private fun applyMode(mode: ThemeMode) {
        AppCompatDelegate.setDefaultNightMode(mode.value)
    }
}
