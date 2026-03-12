package com.wkq.base

import androidx.lifecycle.ViewModel

/**
 * 用于未来扩展的 Base ViewModel（如公共 UI 状态、加载中、Toast 事件等）
 */
abstract class BaseViewModel : ViewModel() {
    // 在此处添加公共逻辑（例如加载状态的 LiveData）
}