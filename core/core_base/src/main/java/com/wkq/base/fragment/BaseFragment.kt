package com.wkq.base.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.wkq.base.reflect.resolveGenericClass

/**
 * 基础 Fragment，集成 ViewBinding
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    protected var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): View? {
        initViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        initView()
        initData()
    }

    protected open fun initViewModel() {}

    @Suppress("UNCHECKED_CAST")
    protected open fun initViewBinding(inflater: LayoutInflater, container: ViewGroup?) {
        val clazz = resolveGenericClass<VB>(this, 0)
        val method = clazz.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java)
        _binding = method.invoke(null, inflater, container, false) as VB
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    abstract fun initView()

    abstract fun initData()
}
