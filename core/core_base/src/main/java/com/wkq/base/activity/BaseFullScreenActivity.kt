package com.wkq.base.activity

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewbinding.ViewBinding

/**
 * 基础全屏 Activity，使用 AndroidX WindowInsets 隐藏状态栏和导航栏。
 */
abstract class BaseFullScreenActivity<VB : ViewBinding> : BaseActivity<VB>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }

        super.onCreate(savedInstanceState)
    }

    override fun applyDefaultSystemBarsInsets() {
        // 全屏页隐藏系统栏，内容保持铺满屏幕，局部控件按需自行处理手势安全区。
    }

    override fun initImmersionBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).run {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        findViewById<android.view.ViewGroup>(android.R.id.content)?.getChildAt(0)?.let {
            it.fitsSystemWindows = false
        }
    }
}
