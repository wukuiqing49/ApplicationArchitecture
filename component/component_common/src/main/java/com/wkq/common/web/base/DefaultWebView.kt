package com.wkq.common.web.base

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * 默认 WebView 实现 (Android 系统 WebView)
 */
class DefaultWebView(context: Context) : IWebView {

    private var webView: WebView = WebView(context)
    private val settingsProxy = DefaultWebSettings(webView.settings)

    override fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    override fun getLoadUrl(): String{
       return webView.url?:""
    }


    override fun reload() {
        webView.reload()

    }

    override fun stopLoading() {
        webView.stopLoading()
    }

    override fun clearHistory() {
        webView.clearHistory()
    }

    override fun clearCache(includeDiskFiles: Boolean) {
        webView.clearCache(includeDiskFiles)
    }

    override fun onPause() {
        webView.onPause()
    }

    override fun onResume() {
        webView.onResume()
    }

    override fun destroy() {
        webView.post {
            webView.destroy()
        }
    }

    @SuppressLint("JavascriptInterface")
    override fun addJavascriptInterface(obj: Any, name: String) {
        webView.addJavascriptInterface(obj, name)
    }

    override fun removeJavascriptInterface(name: String) {
        webView.removeJavascriptInterface(name)
    }

    override fun setWebViewClient(client: Any?) {
        if (client == null) {
            webView.webViewClient = WebViewClient()
        } else if (client is WebViewClient) {
            webView.webViewClient = client
        }
    }

    override fun setWebChromeClient(client: Any?) {
        if (client == null) {
            webView.webChromeClient = null
        } else if (client is WebChromeClient) {
            webView.webChromeClient = client
        }
    }

    override fun removeAllViews() {
        webView.removeAllViews()
    }

    override fun getSettings(): IWebSettings = settingsProxy

    override fun getView(): View = webView
    override fun canGoBack(): Boolean {
       return webView.canGoBack()
    }
    override fun goBack() {
        webView.goBack()
    }
}

class DefaultWebSettings(private val settings: WebSettings) : IWebSettings {
    override fun setJavaScriptEnabled(enabled: Boolean) {
        settings.javaScriptEnabled = enabled
    }

    override fun setSupportZoom(support: Boolean) {
        settings.setSupportZoom(support)
    }

    override fun setBuiltInZoomControls(enabled: Boolean) {
        settings.builtInZoomControls = enabled
    }

    override fun setUseWideViewPort(use: Boolean) {
        settings.useWideViewPort = use
    }

    override fun setLoadWithOverviewMode(load: Boolean) {
        settings.loadWithOverviewMode = load
    }

    override fun setCacheMode(mode: Int) {
        settings.cacheMode = mode
    }

    override fun setAllowFileAccess(allow: Boolean) {
        settings.allowFileAccess = allow
    }

    override fun setDomStorageEnabled(enabled: Boolean) {
        settings.domStorageEnabled = enabled
    }

    override fun setTextZoom(textZoom: Int) {
        settings.textZoom = textZoom
    }

    override fun setMixedContentMode(mode: Int) {
        settings.mixedContentMode = mode
    }

    override fun setJavaScriptCanOpenWindowsAutomatically(allow: Boolean) {
        settings.javaScriptCanOpenWindowsAutomatically = allow
    }

    override fun setUserAgentString(ua: String) {
        settings.userAgentString = ua
    }

    override fun setAllowFileAccessFromFileURLs(allow: Boolean) {
        settings.allowFileAccessFromFileURLs = allow
    }

    override fun setAllowUniversalAccessFromFileURLs(allow: Boolean) {
        settings.allowUniversalAccessFromFileURLs = allow
    }

    override fun setLoadsImagesAutomatically(allow: Boolean) {
        settings.loadsImagesAutomatically = allow
    }

    override fun setLayoutAlgorithm(algorithm: Any) {
        if (algorithm is WebSettings.LayoutAlgorithm) {
            settings.layoutAlgorithm = algorithm
        }
    }
}
