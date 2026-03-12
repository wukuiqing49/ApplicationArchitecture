package com.wkq.base.activity

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.wkq.base.databinding.ViewTitleContentContainerBinding
import java.lang.reflect.ParameterizedType

/**
 * 带标题栏的基础 Activity (集成 ViewModel)
 */
abstract class BaseVMTitleActivity<ContentVB : ViewBinding, VM : ViewModel> : 
    BaseVMActivity<ViewTitleContentContainerBinding, VM>() {

    /** 内容区域的 ViewBinding，供子类直接访问 */
    protected lateinit var contentBinding: ContentVB

    @Suppress("UNCHECKED_CAST")
    override fun initViewBinding() {
        super.initViewBinding()
        val type = javaClass.genericSuperclass as ParameterizedType
        // index 0 is ContentVB, index 1 is VM
        val clazz = type.actualTypeArguments[0] as Class<ContentVB>
        val method = clazz.getMethod("inflate", LayoutInflater::class.java)
        contentBinding = method.invoke(null, layoutInflater) as ContentVB
        binding.flContent.addView(contentBinding.root)
    }

    @Suppress("UNCHECKED_CAST")
    override fun initViewModel() {
        val type = javaClass.genericSuperclass as ParameterizedType
        // VM index is 1
        val clazz = type.actualTypeArguments[1] as Class<VM>
        viewModel = ViewModelProvider(this)[clazz]
    }

    override fun initImmersionBar() {
        super.initImmersionBar()
        setViewBelowStatusBar(binding.titleBar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.titleBar.onLeftClickListener = { finish() }
    }

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
    protected fun setRightIcon(resId: Int, onClick: (() -> Unit)? = null) {
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
