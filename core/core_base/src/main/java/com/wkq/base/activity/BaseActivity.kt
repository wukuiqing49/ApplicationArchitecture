package com.wkq.base.activity

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.gyf.immersionbar.ImmersionBar
import java.lang.reflect.ParameterizedType

/**
 * 基础 Activity，集成权限管理、ViewBinding 和沉浸式状态栏
 */
abstract class BaseActivity<VB : ViewBinding> : PermissionsActivity() {

    protected lateinit var binding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 初始化 ViewBinding
        initViewBinding()
        setContentView(binding.root)

        // 2. 初始化 ViewModel (提供给 VM 子类实现)
        initViewModel()

        // 3. 初始化沉浸式状态栏
        initImmersionBar()

        // 4. 初始化视图和数据
        initView()
        initData()
    }

    protected open fun initViewModel() {}

    @Suppress("UNCHECKED_CAST")
    protected open fun initViewBinding() {
        var type = javaClass.genericSuperclass
        while (type !is ParameterizedType) {
            val superclass = (type as? Class<*>)?.genericSuperclass
            if (superclass != null) {
                type = superclass
            } else {
                throw RuntimeException("未能在父类树中找到泛型 ViewBinding")
            }
        }
        val clazz = type.actualTypeArguments[0] as Class<VB>
        val method = clazz.getMethod("inflate", LayoutInflater::class.java)
        binding = method.invoke(null, layoutInflater) as VB
    }

    protected open fun initImmersionBar() {
        // 强制 Window 延伸到状态栏和导航栏区域 (全屏沉浸)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        ImmersionBar.with(this)
            .transparentStatusBar() // 透明状态栏
            .statusBarDarkFont(setStatusBarDarkFont())
            .init()
    }

    /**
     * 为指定的 View 解决状态栏重叠问题
     * 自动从顶部增加 statusBarHeight 到 PaddingTop 中，保持 Toolbar 颜色延伸到状态栏。
     */
    protected open fun setViewBelowStatusBar(view: android.view.View) {
        val originalPaddingTop = view.paddingTop
        val lp = view.layoutParams
        val originalHeight = lp.height

       ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top

            // 1. 设置 PaddingTop 让内容下移
            v.setPadding(
                v.paddingLeft,
                originalPaddingTop + statusBarHeight,
                v.paddingRight,
                v.paddingBottom
            )

            // 2. 如果 Toolbar 有固定高度，则将状态栏高度追加进去
            if (originalHeight > 0) {
                val layoutParams = v.layoutParams
                layoutParams.height = originalHeight + statusBarHeight
                v.layoutParams = layoutParams
            }

            insets
        }

        view.requestApplyInsets()
    }

   open fun setStatusBarDarkFont(): Boolean {
        // 白天模式 → 状态栏深色字体（黑色）
        // 黑夜模式 → 状态栏浅色字体（白色）
        val nightModeFlags = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags != android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
    /**
     * 初始化布局
     */
    abstract fun initView()

    /**
     * 初始化数据
     */
    abstract fun initData()
}
