package com.wkq.base.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.wkq.base.reflect.resolveGenericClass

/**
 * 集成 ViewModel 的基础 Fragment
 */
abstract class BaseVMFragment<VB : ViewBinding, VM : ViewModel> : BaseFragment<VB>() {

    protected lateinit var viewModel: VM

    @Suppress("UNCHECKED_CAST")
    override fun initViewModel() {
        val clazz = resolveGenericClass<VM>(this, 1)
        viewModel = ViewModelProvider(this)[clazz]
    }
}
