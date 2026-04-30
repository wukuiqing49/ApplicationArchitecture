package com.wkq.base.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wkq.base.reflect.resolveGenericClass

/**
 * 完全封装的基础列表 Fragment (集成 ViewModel)
 */
abstract class BaseVMListFragment<VM : ViewModel, T> : BaseListFragment<T>() {

    protected lateinit var viewModel: VM

    @Suppress("UNCHECKED_CAST")
    override fun initViewModel() {
        val clazz = resolveGenericClass<VM>(this, 0)
        viewModel = ViewModelProvider(this)[clazz]
    }
}
