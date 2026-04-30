package com.wkq.base.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wkq.base.reflect.resolveGenericClass

/**
 * 完全封装的基础列表 Activity (集成 ViewModel)
 */
abstract class BaseVMListActivity<VM : ViewModel, T> : BaseListActivity<T>() {

    protected lateinit var viewModel: VM

    @Suppress("UNCHECKED_CAST")
    override fun initViewModel() {
        val clazz = resolveGenericClass<VM>(this, 0)
        viewModel = ViewModelProvider(this)[clazz]
    }
}
