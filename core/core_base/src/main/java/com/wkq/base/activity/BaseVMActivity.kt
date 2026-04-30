package com.wkq.base.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.wkq.base.reflect.resolveGenericClass

/**
 * 集成 ViewModel 的基础 Activity
 */
abstract class BaseVMActivity<VB : ViewBinding, VM : ViewModel> : BaseActivity<VB>() {

    protected lateinit var viewModel: VM

    @Suppress("UNCHECKED_CAST")
    override fun initViewModel() {
        val clazz = resolveGenericClass<VM>(this, 1)
        viewModel = ViewModelProvider(this)[clazz]
    }
}
