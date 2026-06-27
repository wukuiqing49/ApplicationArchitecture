package com.wkq.base.state

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.wkq.base.BaseUiEvent
import com.wkq.base.BaseUiState

/**
 * VM 页面宿主的 UI 状态绑定控制器。
 *
 * Activity / Fragment 基类只需要持有这个控制器，不再各自维护 BaseUiDelegate 的创建和释放细节。
 */
internal class BaseVmUiController {
    private var uiDelegate: BaseUiDelegate? = null

    /**
     * 按需绑定 BaseViewModel 的状态和一次性事件。
     */
    fun bind(
        viewModel: ViewModel,
        enabled: Boolean,
        contextProvider: () -> Context?,
        lifecycleOwnerProvider: () -> LifecycleOwner?,
        onEmpty: ((String?) -> Unit)? = null,
        onContent: (() -> Unit)? = null,
        onStateChanged: ((BaseUiState) -> Unit)? = null,
        onEvent: ((BaseUiEvent) -> Boolean)? = null
    ) {
        clear()
        uiDelegate = BaseUiDelegate.bindIfEnabled(
            viewModel = viewModel,
            enabled = enabled,
            contextProvider = contextProvider,
            lifecycleOwnerProvider = lifecycleOwnerProvider,
            onEmpty = onEmpty,
            onContent = onContent,
            onStateChanged = onStateChanged,
            onEvent = onEvent
        )
    }

    /**
     * 页面销毁时释放弹框等 UI 引用。
     */
    fun clear() {
        uiDelegate?.clear()
        uiDelegate = null
    }
}
