package com.wkq.base.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

/**
 * 集成 ViewModel 的基础 Activity
 */
abstract class BaseVMActivity<VB : ViewBinding, VM : ViewModel> : BaseActivity<VB>() {

    protected lateinit var viewModel: VM

    @Suppress("UNCHECKED_CAST")
    override fun initViewModel() {
        val type = javaClass.genericSuperclass as ParameterizedType
        val clazz = type.actualTypeArguments[1] as Class<VM>
        viewModel = ViewModelProvider(this)[clazz]
    }
}
