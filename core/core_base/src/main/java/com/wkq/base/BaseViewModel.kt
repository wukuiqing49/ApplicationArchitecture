package com.wkq.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * 统一 ViewModel 父类，沉淀页面常见 UI 状态。
 */
abstract class BaseViewModel : ViewModel() {
    /**
     * 旧版错误提示入口，兼容历史调用。
     */
    val errorMutableLiveData = MutableLiveData<String>()

    private val _uiStateLiveData = MutableLiveData<BaseUiState>(BaseUiState.Idle)

    /**
     * 页面长生命周期 UI 状态，例如 loading、content、empty、error。
     */
    val uiStateLiveData: LiveData<BaseUiState> = _uiStateLiveData

    private val _uiEventLiveData = MutableLiveData<ConsumableEvent<BaseUiEvent>>()

    /**
     * 一次性 UI 事件，例如 Toast、弹框、跳转、关闭页面。
     */
    val uiEventLiveData: LiveData<ConsumableEvent<BaseUiEvent>> = _uiEventLiveData

    /**
     * 通知页面进入加载中状态。
     */
    protected fun showLoading(message: String? = null) {
        _uiStateLiveData.value = BaseUiState.Loading(message)
    }

    /**
     * 通知页面展示正常内容。
     */
    protected fun showContent() {
        _uiStateLiveData.value = BaseUiState.Content
    }

    /**
     * 通知页面展示空状态。
     */
    protected fun showEmpty(message: String? = null) {
        _uiStateLiveData.value = BaseUiState.Empty(message)
    }

    /**
     * 通知页面展示错误状态，并同步兼容旧的 errorMutableLiveData。
     */
    protected fun showError(message: String, throwable: Throwable? = null) {
        errorMutableLiveData.value = message
        _uiStateLiveData.value = BaseUiState.Error(message, throwable)
    }

    /**
     * 重置为初始状态。
     */
    protected fun resetUiState() {
        _uiStateLiveData.value = BaseUiState.Idle
    }

    /**
     * 发送一次性 Toast 事件。
     */
    protected fun sendToast(message: String) {
        sendUiEvent(BaseUiEvent.Toast(message))
    }

    /**
     * 发送一次性确认弹框事件。
     */
    protected fun sendConfirmDialog(
        requestKey: String,
        title: String,
        message: String,
        confirmText: String? = null,
        cancelText: String? = null
    ) {
        sendUiEvent(BaseUiEvent.ConfirmDialog(requestKey, title, message, confirmText, cancelText))
    }

    /**
     * 确认弹框按钮结果回调。
     */
    open fun onConfirmDialogResult(requestKey: String, confirmed: Boolean) = Unit

    /**
     * 确认弹框语义化结果回调。
     *
     * 默认会继续分发到旧版 Boolean 回调，兼容已有页面重写 onConfirmDialogResult(requestKey, confirmed) 的写法。
     */
    open fun onConfirmDialogResult(requestKey: String, result: ConfirmDialogResult) {
        onConfirmDialogResult(requestKey, result == ConfirmDialogResult.CONFIRM)
    }

    /**
     * 发送一次性导航事件，默认由页面接入路由处理。
     */
    protected fun sendNavigate(path: String) {
        sendUiEvent(BaseUiEvent.Navigate(path))
    }

    /**
     * 发送一次性关闭页面事件。
     */
    protected fun sendFinish() {
        sendUiEvent(BaseUiEvent.Finish)
    }

    /**
     * 发送自定义一次性 UI 事件。
     */
    protected fun sendUiEvent(event: BaseUiEvent) {
        _uiEventLiveData.value = ConsumableEvent(event)
    }
}

/**
 * 页面长生命周期 UI 状态。
 */
sealed class BaseUiState {
    /** 初始状态。 */
    data object Idle : BaseUiState()

    /** 内容展示状态。 */
    data object Content : BaseUiState()

    /** 加载中状态。 */
    data class Loading(val message: String? = null) : BaseUiState()

    /** 空数据状态。 */
    data class Empty(val message: String? = null) : BaseUiState()

    /** 错误状态。 */
    data class Error(val message: String, val throwable: Throwable? = null) : BaseUiState()
}
