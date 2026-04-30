package com.wkq.base.activity

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.wkq.base.databinding.ViewTitleContentContainerBinding
import com.wkq.base.reflect.resolveGenericClass

/**
 * 带标题栏的基础 Activity (集成 ViewModel)
 */
abstract class BaseVMTitleActivity<VB : ViewBinding, VM : ViewModel> :
    BaseVMActivity<ViewTitleContentContainerBinding, VM>() {

    protected lateinit var contentBinding: VB

    @Suppress("UNCHECKED_CAST")
    override fun initViewBinding() {
        binding = ViewTitleContentContainerBinding.inflate(layoutInflater)

        val contentClass = resolveGenericClass<VB>(this, 0)
        val inflate = contentClass.getMethod("inflate", LayoutInflater::class.java)
        contentBinding = inflate.invoke(null, layoutInflater) as VB

        binding.flContent.addView(contentBinding.root)
    }

    @Suppress("UNCHECKED_CAST")
    override fun initViewModel() {
        val clazz = resolveGenericClass<VM>(this, 1)
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

    protected fun setPageTitle(title: String) {
        binding.titleBar.setTitle(title)
    }

    protected fun setRightText(text: String, onClick: (() -> Unit)? = null) {
        binding.titleBar.setRightText(text)
        onClick?.let { binding.titleBar.onRightClickListener = it }
    }

    protected fun setRightIcon(resId: Int, onClick: (() -> Unit)? = null) {
        binding.titleBar.setRightIcon(resId)
        onClick?.let { binding.titleBar.onRightClickListener = it }
    }

    protected fun setLeftVisible(visible: Boolean) {
        binding.titleBar.setLeftIconVisible(visible)
    }

    protected fun setLeftClickListener(block: () -> Unit) {
        binding.titleBar.onLeftClickListener = block
    }
}
