package com.wkq.base.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.reflect.ParameterizedType

/**
 * 完全封装的基础列表 Activity (集成 ViewModel)
 */
abstract class BaseVMListActivity<VM : ViewModel, T> : BaseListActivity<T>() {

    protected lateinit var viewModel: VM

    @Suppress("UNCHECKED_CAST")
    override fun initViewModel() {
        val type = javaClass.genericSuperclass as ParameterizedType
        val clazz = type.actualTypeArguments[0] as Class<VM>
        viewModel = ViewModelProvider(this)[clazz]
    }
}
