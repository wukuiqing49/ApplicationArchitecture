package com.wkq.base.activity

import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.wkq.base.databinding.ViewTitleContentContainerBinding
import java.lang.reflect.ParameterizedType

/**
 * 带标题栏的基础 Activity
 *
 * 使用方式：
 *  - 继承本类，声明 **内容区域的 ViewBinding** 和 **ViewModel** 两个泛型
 *  - 实现 [initView] 和 [initData]，通过 [contentBinding] 访问页面内容
 *  - 无需自己处理返回键、沉浸式状态栏、标题栏
 *
 * 常用 API：
 *  - [setPageTitle]      设置标题文字
 *  - [setRightText]      设置右侧文字按钮（含点击回调）
 *  - [setRightIcon]      设置右侧图标按钮（含点击回调）
 *  - [setLeftVisible]    控制左侧返回箭头的显示隐藏
 *
 * ```kotlin
 * class MyActivity : BaseTitleActivity<ActivityMyBinding, MyViewModel>() {
 *     override fun initView() {
 *         setPageTitle("我的页面")
 *         setRightText("完成") { viewModel.save() }
 *         contentBinding.tvHello.text = "Hello"
 *     }
 *     override fun initData() { viewModel.load() }
 * }
 * ```
 */
/**
 * 带标题栏的基础 Activity (无 ViewModel)
 */
abstract class BaseTitleActivity<ContentVB : ViewBinding> : BaseActivity<ViewTitleContentContainerBinding>() {

    /** 内容区域的 ViewBinding，供子类直接访问 */
    protected lateinit var contentBinding: ContentVB

    // ─── 重写 initViewBinding：固定注入容器布局 + 内容布局 ──────────────

    @Suppress("UNCHECKED_CAST")
    override fun initViewBinding() {
        super.initViewBinding()
        val type = javaClass.genericSuperclass as ParameterizedType
        val clazz = type.actualTypeArguments[0] as Class<ContentVB>
        val method = clazz.getMethod("inflate", LayoutInflater::class.java)
        contentBinding = method.invoke(null, layoutInflater) as ContentVB
        binding.flContent.addView(contentBinding.root)
    }

    // ─── 沉浸式：自动让标题栏适配状态栏高度 ──────────────────────────────

    override fun initImmersionBar() {
        super.initImmersionBar()
        setViewBelowStatusBar(binding.titleBar)
    }

    // ─── 默认左侧点击 = finish() ──────────────────────────────────────────

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        binding.titleBar.onLeftClickListener = { finish() }
    }

    // ─── 公开 API ─────────────────────────────────────────────────────────

    /** 设置顶部标题文字 */
    protected fun setPageTitle(title: String) {
        binding.titleBar.setTitle(title)
    }

    /** 设置右侧文字按钮（可选点击回调） */
    protected fun setRightText(text: String, onClick: (() -> Unit)? = null) {
        binding.titleBar.setRightText(text)
        onClick?.let { binding.titleBar.onRightClickListener = it }
    }

    /** 设置右侧图标按钮（可选点击回调） */
    protected fun setRightIcon(@DrawableRes resId: Int, onClick: (() -> Unit)? = null) {
        binding.titleBar.setRightIcon(resId)
        onClick?.let { binding.titleBar.onRightClickListener = it }
    }

    /** 控制左侧返回箭头显隐（默认显示） */
    protected fun setLeftVisible(visible: Boolean) {
        binding.titleBar.setLeftIconVisible(visible)
    }

    /** 自定义左侧点击行为（默认是 finish()） */
    protected fun setLeftClickListener(block: () -> Unit) {
        binding.titleBar.onLeftClickListener = block
    }
}
