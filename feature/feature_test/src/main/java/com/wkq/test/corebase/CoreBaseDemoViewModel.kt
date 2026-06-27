package com.wkq.test.corebase

import androidx.lifecycle.MutableLiveData
import com.wkq.base.BaseViewModel
import com.wkq.base.ConfirmDialogResult

class CoreBaseDemoViewModel : BaseViewModel() {
    val state = MutableLiveData("ViewModel 已创建")

    fun markLoaded(name: String) {
        showLoading("$name loading")
        state.value = "$name 已加载：${System.currentTimeMillis()}"
        showContent()
    }

    fun markEmpty(name: String) {
        state.value = "$name 空状态"
        showEmpty("$name 暂无数据")
    }

    fun markError(name: String) {
        state.value = "$name 错误状态"
        showError("$name 加载失败")
    }

    fun sendDemoToast() {
        sendToast("BaseUiEvent.Toast 已消费")
    }

    fun sendDemoDialog() {
        sendConfirmDialog(
            requestKey = REQUEST_DEMO_CONFIRM,
            title = "BaseUiEvent",
            message = "这是 ViewModel 发送的一次性确认弹框事件。"
        )
    }

    fun sendDemoNavigate() {
        sendNavigate("/test/core_base/list_state_activity")
    }

    override fun onConfirmDialogResult(requestKey: String, result: ConfirmDialogResult) {
        if (requestKey == REQUEST_DEMO_CONFIRM) {
            sendToast(
                when (result) {
                    ConfirmDialogResult.CONFIRM -> "确认弹框：点击了确定"
                    ConfirmDialogResult.CANCEL -> "确认弹框：点击了取消"
                }
            )
        }
    }

    private companion object {
        const val REQUEST_DEMO_CONFIRM = "demo_confirm"
    }
}
