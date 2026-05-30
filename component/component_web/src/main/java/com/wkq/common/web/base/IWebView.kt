package com.wkq.common.web.base


import android.view.View

/**
 * WebView 抽象接口
 * 用于支持切换不同的 WebView 引擎 (如原生 WebView, 腾讯 X5 等)
 */
interface IWebView {

    /**
     * 加载 URL
     */
    fun loadUrl(url: String)
    fun getLoadUrl(): String?

    /**
     * 重新加载
     */
    fun reload()

    /**
     * 停止加载
     */
    fun stopLoading()

    /**
     * 清理历史
     */
    fun clearHistory()

    /**
     * 清理缓存
     */
    fun clearCache(includeDiskFiles: Boolean)

    /**
     * 生命周期: 暂停
     */
    fun onPause()

    /**
     * 生命周期: 恢复
     */
    fun onResume()

    /**
     * 销毁
     */
    fun destroy()

    /**
     * 添加 JS 交互接口
     */
    fun addJavascriptInterface(obj: Any, name: String)

    /**
     * 移除 JS 交互接口
     */
    fun removeJavascriptInterface(name: String)

    /**
     * 设置 WebViewClient (传入 Any 由实现类自行处理类型转换)
     */
    fun setWebViewClient(client: Any?)

    /**
     * 设置 WebChromeClient (传入 Any 由实现类自行处理类型转换)
     */
    fun setWebChromeClient(client: Any?)

    /**
     * 移除所有子视图
     */
    fun removeAllViews()

    /**
     * 获取设置项封装 (此处由于 WebSettings 也是引擎相关的，返回 IWebSettings)
     */
    fun getSettings(): IWebSettings

    /**
     * 获取真实的 View 实例用于添加进布局
     */
    fun getView(): View
    fun canGoBack(): Boolean
    fun goBack()
}

/**
 * 设置项抽象接口
 */
interface IWebSettings {
    fun setJavaScriptEnabled(enabled: Boolean)
    fun setSupportZoom(support: Boolean)
    fun setBuiltInZoomControls(enabled: Boolean)
    fun setUseWideViewPort(use: Boolean)
    fun setLoadWithOverviewMode(load: Boolean)
    fun setCacheMode(mode: Int)
    fun setAllowFileAccess(allow: Boolean)
    fun setDomStorageEnabled(enabled: Boolean)
    fun setTextZoom(textZoom: Int)
    fun setMixedContentMode(mode: Int)
    fun setJavaScriptCanOpenWindowsAutomatically(allow: Boolean)
    fun setUserAgentString(ua: String)
    fun setAllowFileAccessFromFileURLs(allow: Boolean)
    fun setAllowUniversalAccessFromFileURLs(allow: Boolean)
    fun setLoadsImagesAutomatically(allow: Boolean)
    fun setLayoutAlgorithm(algorithm: Any)
}
