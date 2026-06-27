package com.wkq.base

import java.util.concurrent.atomic.AtomicBoolean

/**
 * 只允许消费一次的事件包装，避免配置变化或重复观察导致 Toast、跳转等动作重复执行。
 */
class ConsumableEvent<out T>(private val value: T) {
    private val consumed = AtomicBoolean(false)

    /**
     * 首次调用时执行 block，后续调用会被忽略。
     */
    fun consume(block: (T) -> Unit) {
        if (consumed.compareAndSet(false, true)) {
            block(value)
        }
    }

    /**
     * 仅查看事件内容，不改变消费状态。
     */
    fun peek(): T = value
}

/**
 * ViewModel 发送给页面的一次性 UI 事件。
 */
sealed class BaseUiEvent {
    /** 显示短 Toast。 */
    data class Toast(val message: String) : BaseUiEvent()

    /**
     * 显示确认弹框。
     *
     * requestKey 用于区分弹框来源，用户点击确认/取消后会回调到 BaseViewModel.onConfirmDialogResult。
     */
    data class ConfirmDialog(
        val requestKey: String,
        val title: String,
        val message: String,
        val confirmText: String? = null,
        val cancelText: String? = null
    ) : BaseUiEvent()

    /** 页面跳转事件，基类默认不处理，业务页面可接入项目路由。 */
    data class Navigate(val path: String) : BaseUiEvent()

    /** 关闭当前 Activity。 */
    data object Finish : BaseUiEvent()
}

/**
 * 确认弹框的用户操作结果。
 */
enum class ConfirmDialogResult {
    /** 用户点击取消或关闭弹框。 */
    CANCEL,

    /** 用户点击确认按钮。 */
    CONFIRM
}
