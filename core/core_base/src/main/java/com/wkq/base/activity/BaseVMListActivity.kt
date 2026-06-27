package com.wkq.base.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wkq.base.BaseUiEvent
import com.wkq.base.BaseUiState
import com.wkq.base.reflect.resolveGenericClass
import com.wkq.base.state.BaseVmUiController

/**
 * 完全封装的基础列表 Activity (集成 ViewModel)
 */
abstract class BaseVMListActivity<VM : ViewModel, T> : BaseListActivity<T>() {

    protected lateinit var viewModel: VM
    private val uiController = BaseVmUiController()

    @Suppress("UNCHECKED_CAST")
    override fun initViewModel() {
        val clazz = resolveGenericClass<VM>(this, 0)
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
     * 空状态回调，列表页默认展示 EmptyView。
     */
    protected open fun onBaseUiEmpty(message: String?) {
        showEmptyView(message)
    }

    /**
     * 内容状态回调，列表页默认展示 RecyclerView。
     */
    protected open fun onBaseUiContent() {
        showContentView()
    }

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
