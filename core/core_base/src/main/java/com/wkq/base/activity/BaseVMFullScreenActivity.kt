package com.wkq.base.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.wkq.base.reflect.resolveGenericClass

/**
 * 集成 ViewModel 的基础全屏 Activity
 */
abstract class BaseVMFullScreenActivity<VB : ViewBinding, VM : ViewModel> :
    BaseFullScreenActivity<VB>() {

    protected lateinit var viewModel: VM

    @Suppress("UNCHECKED_CAST")
    override fun initViewModel() {
        val clazz = resolveGenericClass<VM>(this, 1)
        viewModel = ViewModelProvider(this)[clazz]
    }
}
