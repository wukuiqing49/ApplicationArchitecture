package com.wkq.base.state

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.wkq.base.BaseUiEvent
import com.wkq.base.BaseUiState
import com.wkq.base.BaseViewModel
import com.wkq.base.ConfirmDialogResult
import com.wkq.base.dialog.CommonDialog
import com.wkq.base.dialog.LoadingDialog
import com.wkq.base.dialog.PopupHandle

/**
 * 统一处理 BaseViewModel 输出的 UI 状态和一次性事件。
 *
 * Activity / Fragment 基类只负责提供生命周期、Context 和页面回调，
 * 具体的 Loading、Toast、空布局回调、事件消费统一收敛到这里。
 */
class BaseUiDelegate(
    private val contextProvider: () -> Context?,
    private val lifecycleOwnerProvider: () -> LifecycleOwner?,
    private val onEmpty: ((String?) -> Unit)? = null,
    private val onContent: (() -> Unit)? = null,
    private val onStateChanged: ((BaseUiState) -> Unit)? = null,
    private val onEvent: ((BaseUiEvent) -> Boolean)? = null,
    private val viewModelProvider: (() -> BaseViewModel?)? = null
) {
    private var loadingHandle: PopupHandle? = null

    /**
     * 绑定 ViewModel 的状态和事件。
     */
    fun bind(viewModel: BaseViewModel) {
        val owner = lifecycleOwnerProvider() ?: return
        viewModel.uiStateLiveData.observe(owner) { state ->
            handleState(state)
        }
        viewModel.uiEventLiveData.observe(owner) { event ->
            event.consume { handleEvent(it) }
        }
    }

    /**
     * 页面销毁时释放内部持有的弹框。
     */
    fun clear() {
        dismissLoading()
    }

    private fun handleState(state: BaseUiState) {
        onStateChanged?.invoke(state)
        when (state) {
            BaseUiState.Idle -> dismissLoading()
            BaseUiState.Content -> {
                dismissLoading()
                onContent?.invoke()
            }
            is BaseUiState.Loading -> showLoading(state.message)
            is BaseUiState.Empty -> {
                dismissLoading()
                onEmpty?.invoke(state.message)
            }
            is BaseUiState.Error -> {
                dismissLoading()
                showToast(state.message)
            }
        }
    }

    private fun showLoading(message: String?) {
        val context = contextProvider() ?: return
        if (loadingHandle?.isShowing() == true) return
        loadingHandle = LoadingDialog.show(
            context = context,
            message = message ?: context.getString(com.wkq.base.R.string.base_loading)
        )
    }

    private fun dismissLoading() {
        val handle = loadingHandle ?: return
        if (handle.isShowing()) {
            handle.dismiss()
        }
        loadingHandle = null
    }

    private fun showToast(message: String) {
        val context = contextProvider() ?: return
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleEvent(event: BaseUiEvent) {
        val handledByPage = onEvent?.invoke(event) == true
        if (handledByPage) return

        when (event) {
            is BaseUiEvent.Toast -> showToast(event.message)
            is BaseUiEvent.ConfirmDialog -> showConfirmDialog(event)
            BaseUiEvent.Finish -> (contextProvider() as? Activity)?.finish()
            is BaseUiEvent.Navigate -> Unit
        }
    }

    private fun showConfirmDialog(event: BaseUiEvent.ConfirmDialog) {
        val context = contextProvider() ?: return
        val viewModel = viewModelProvider?.invoke()
        CommonDialog.showConfirm(
            context = context,
            title = event.title,
            message = event.message,
            confirmText = event.confirmText ?: context.getString(com.wkq.base.R.string.base_confirm),
            cancelText = event.cancelText ?: context.getString(com.wkq.base.R.string.base_cancel),
            onCancel = {
                viewModel?.onConfirmDialogResult(event.requestKey, ConfirmDialogResult.CANCEL)
            },
            onConfirm = {
                viewModel?.onConfirmDialogResult(event.requestKey, ConfirmDialogResult.CONFIRM)
            }
        )
    }

    companion object {
        /**
         * 按需创建并绑定 BaseUiDelegate，集中收敛 VM 基类里的样板逻辑。
         */
        fun bindIfEnabled(
            viewModel: ViewModel,
            enabled: Boolean,
            contextProvider: () -> Context?,
            lifecycleOwnerProvider: () -> LifecycleOwner?,
            onEmpty: ((String?) -> Unit)? = null,
            onContent: (() -> Unit)? = null,
            onStateChanged: ((BaseUiState) -> Unit)? = null,
            onEvent: ((BaseUiEvent) -> Boolean)? = null
        ): BaseUiDelegate? {
            if (!enabled) return null
            val baseViewModel = viewModel as? BaseViewModel ?: return null
            return BaseUiDelegate(
                contextProvider = contextProvider,
                lifecycleOwnerProvider = lifecycleOwnerProvider,
                onEmpty = onEmpty,
                onContent = onContent,
                onStateChanged = onStateChanged,
                onEvent = onEvent,
                viewModelProvider = { baseViewModel }
            ).also { it.bind(baseViewModel) }
        }
    }
}
