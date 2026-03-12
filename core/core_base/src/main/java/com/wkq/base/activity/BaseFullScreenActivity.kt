package com.wkq.base.activity

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewbinding.ViewBinding

/**
 * 基础全屏 Activity (无 ViewModel)
 * 支持 API 24-36+，使用 WindowInsetsControllerCompat 隐藏状态栏和导航栏
 */
abstract class BaseFullScreenActivity<VB : ViewBinding> : BaseActivity<VB>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 设置全屏标志，允许内容延伸到系统栏并且突破边界限制
        // 在 API 35 (Android 15) 中 statusBarColor 与 navigationBarColor 已被正式废弃。
        // 全屏沉浸式官方推荐统一使用 AndroidX Activity 的 enableEdgeToEdge()，或者配合 WindowCompat。
        // 为了向下兼容并消除编译警告，直接使用反射/安全调用的方式，或者屏蔽不必要的硬编码颜色设置，因为后续会交给 controller 接管。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }

        super.onCreate(savedInstanceState)
    }

    override fun initImmersionBar() {
        // 让 Window 扩展到系统边栏区域
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.run {
            // 隐藏状态栏和导航栏
            hide(WindowInsetsCompat.Type.systemBars())
            // 滑动边缘可短暂显示系统栏 (沉浸式交互)
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        // 可选：确保根布局不会自动给这些系统栏留出 Padding
        findViewById<android.view.ViewGroup>(android.R.id.content)?.getChildAt(0)?.let {
            it.fitsSystemWindows = false
        }
    }
}
