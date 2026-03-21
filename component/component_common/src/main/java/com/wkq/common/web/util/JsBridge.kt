package com.wkq.common.web.util

import android.content.Context
import android.webkit.JavascriptInterface

/**
 *
 * @ Author: wkq
 *
 * @ Time: 2026/3/21 10:48
 *
 * @ Desc:  基础 Web 调试桥，支持标准 postMessage 方式
 *
 */
class JsBridge(var mContext: Context) {

    /**
     * 原生 JS 调用的方法
     */
    @JavascriptInterface
    fun postMessage(message: String) {

    }
}