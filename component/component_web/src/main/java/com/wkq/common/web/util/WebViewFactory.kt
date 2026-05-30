package com.wkq.common.web.util

import android.content.Context
import com.wkq.common.web.base.DefaultWebView
import com.wkq.common.web.base.IWebView

/**
 * WebView 工厂类
 * 统一管理各引擎 WebView 的实例化逻辑
 */
object WebViewFactory {

    /**
     * 是否全局使用 X5 引擎的开关
     * 默认为 false，以后集成 TBS SDK 后可动态开启
     */
    private var useX5Engine = false

    /**
     * 创建对应的 IWebView 实例
     */
    fun create(context: Context): IWebView {
        return if (useX5Engine) {
            // TODO: 当集成 X5 后的代码示例:
            // TbsWebView(context)
            DefaultWebView(context)
        } else {
            DefaultWebView(context)
        }
    }

    /**
     * 设置全局是否启用 X5 引擎
     */
    fun setUseX5Engine(enabled: Boolean) {
        useX5Engine = enabled
    }

    /**
     * 获取当前是否开启了 X5 引擎
     */
    fun isUseX5Engine(): Boolean = useX5Engine
}
