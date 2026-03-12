package com.wkq.base.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.reflect.ParameterizedType

/**
 * 完全封装的基础列表 Fragment (集成 ViewModel)
 */
abstract class BaseVMListFragment<VM : ViewModel, T> : BaseListFragment<T>() {

    protected lateinit var viewModel: VM

    @Suppress("UNCHECKED_CAST")
    override fun initViewModel() {
        val type = javaClass.genericSuperclass as ParameterizedType
        val clazz = type.actualTypeArguments[0] as Class<VM>
        viewModel = ViewModelProvider(this)[clazz]
    }
}
