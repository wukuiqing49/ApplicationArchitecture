package com.wkq.base.activity

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.WindowCompat
import androidx.viewbinding.ViewBinding
import com.wkq.base.insets.SystemBarInsets
import com.wkq.base.reflect.resolveGenericClass

/**
 * 基础 Activity，集成权限处理、ViewBinding 和系统栏适配。
 */
abstract class BaseActivity<VB : ViewBinding> : PermissionsActivity() {

    protected lateinit var binding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViewBinding()
        setContentView(binding.root)

        initViewModel()
        initImmersionBar()
        initView()
        initData()
    }

    protected open fun initViewModel() {}

    @Suppress("UNCHECKED_CAST")
    protected open fun initViewBinding() {
        val clazz = resolveGenericClass<VB>(this, 0)
        val method = clazz.getMethod("inflate", LayoutInflater::class.java)
        binding = method.invoke(null, layoutInflater) as VB
    }

    /**
     * 兼容旧子类 override，内部已改为 AndroidX/SystemBars 实现。
     */
    protected open fun initImmersionBar() {
        initSystemBars()
    }

    protected open fun initSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        val useDarkIcons = setStatusBarDarkFont()
        WindowCompat.getInsetsController(window, window.decorView).run {
            isAppearanceLightStatusBars = useDarkIcons
            isAppearanceLightNavigationBars = useDarkIcons
        }
        applyDefaultSystemBarsInsets()
    }

    protected open fun applyDefaultSystemBarsInsets() {
        SystemBarInsets.applySystemBarsInset(binding.root)
    }

    protected open fun setViewBelowStatusBar(view: android.view.View) {
        SystemBarInsets.applyTopInset(view)
    }

    open fun setStatusBarDarkFont(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags != Configuration.UI_MODE_NIGHT_YES
    }

    abstract fun initView()

    abstract fun initData()
}
