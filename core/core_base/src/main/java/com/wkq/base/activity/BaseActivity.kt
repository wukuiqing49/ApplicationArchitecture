package com.wkq.base.activity

import android.os.Bundle
import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding
import com.gyf.immersionbar.ImmersionBar
import com.wkq.base.reflect.resolveGenericClass

/**
 * 基础 Activity，集成权限管理、ViewBinding 和沉浸式状态栏
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

    protected open fun initImmersionBar() {
        ImmersionBar.with(this)
            .transparentStatusBar()
            .statusBarDarkFont(setStatusBarDarkFont())
            .init()
    }

    protected open fun setViewBelowStatusBar(view: android.view.View) {
        // Intentionally empty. Specific pages can override when they need inset handling.
    }

    open fun setStatusBarDarkFont(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags != android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    abstract fun initView()

    abstract fun initData()
}
