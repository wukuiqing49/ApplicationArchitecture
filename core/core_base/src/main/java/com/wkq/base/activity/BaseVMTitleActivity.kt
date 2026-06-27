package com.wkq.base.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.wkq.base.BaseUiEvent
import com.wkq.base.BaseUiState
import com.wkq.base.reflect.resolveGenericClass
import com.wkq.base.state.BaseVmUiController

/**
 * 带通用标题栏和 ViewModel 的基础 Activity。
 */
abstract class BaseVMTitleActivity<VB : ViewBinding, VM : ViewModel> :
    BaseTitleActivity<VB>() {

    protected lateinit var viewModel: VM
    private val uiController = BaseVmUiController()

    @Suppress("UNCHECKED_CAST")
    override fun initViewModel() {
        val clazz = resolveGenericClass<VM>(this, 1)
        viewModel = ViewModelProvider(this)[clazz]
        bindBaseUiStateIfNeeded()
    }

    /**
     * 是否启用 BaseViewModel 的状态和事件自动处理。
     */
    protected open fun enableBaseUiStateObserver(): Boolean = true

    /**
     * BaseUiState 变化回调，子类可用于补充自定义 UI 处理。
     */
    protected open fun onBaseUiStateChanged(state: BaseUiState) = Unit

    /**
     * 一次性事件回调；返回 true 表示子类已处理，基类不再执行默认逻辑。
     */
    protected open fun onBaseUiEvent(event: BaseUiEvent): Boolean = false

    /**
     * 空状态回调，标题页默认不处理。
     */
    protected open fun onBaseUiEmpty(message: String?) = Unit

    /**
     * 内容状态回调，标题页默认不处理。
     */
    protected open fun onBaseUiContent() = Unit

    private fun bindBaseUiStateIfNeeded() {
        uiController.bind(
            viewModel = viewModel,
            enabled = enableBaseUiStateObserver(),
            contextProvider = { if (!isFinishing && !isDestroyed) this else null },
            lifecycleOwnerProvider = { this },
            onEmpty = { onBaseUiEmpty(it) },
            onContent = { onBaseUiContent() },
            onStateChanged = { onBaseUiStateChanged(it) },
            onEvent = { onBaseUiEvent(it) }
        )
    }

    override fun onDestroy() {
        uiController.clear()
        super.onDestroy()
    }
}
