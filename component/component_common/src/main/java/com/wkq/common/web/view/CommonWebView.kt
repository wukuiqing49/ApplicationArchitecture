package com.wkq.common.web.view

import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.AttributeSet
import android.view.LayoutInflater
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.net.Uri
import androidx.constraintlayout.widget.ConstraintLayout
import com.wkq.common.databinding.LayoutCommonWebviewBinding
import com.wkq.common.web.base.IWebSettings
import com.wkq.common.web.base.IWebView
import com.wkq.common.web.util.JsBridge
import com.wkq.common.web.util.WebViewFactory


/**
 *
 * @ Author: wkq
 *
 * @ Time: 2026/3/21 10:04
 *
 * @ Desc: 通用 WebView 封装
 */
class CommonWebView @JvmOverloads constructor(
    var mContext: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(mContext, attrs, defStyleAttr) {

    private var bridgeNameList = arrayListOf<String>()



    private var isDestroyed = false

    var onShowFileChooser: ((ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean)? =
        null
    var onReceivedTitle: ((String?) -> Unit)? = null
    var onPageFinished: ((String?) -> Unit)? = null

    private val webView: IWebView = WebViewFactory.create(mContext)
    var hasError = false
    private val binding by lazy {
        LayoutCommonWebviewBinding.inflate(LayoutInflater.from(mContext), this, true)
    }
    private var settings: IWebSettings = webView.getSettings()

    init {
        binding.webviewContainer.addView(
            webView.getView(), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        settings.apply {
            setJavaScriptCanOpenWindowsAutomatically(true)
            setUserAgentString("User-Agent:Android")
            setSupportZoom(true)
            setBuiltInZoomControls(true)
            setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS)
            setLoadWithOverviewMode(true)
            setUseWideViewPort(true)
            setAllowFileAccess(false)
            setCacheMode(WebSettings.LOAD_NO_CACHE)
            setDomStorageEnabled(true)
            setTextZoom(100)
            setJavaScriptEnabled(false)
            setLoadsImagesAutomatically(true)
            setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW)
        }

        setLayerType(LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))

        webView.setWebViewClient(object : android.webkit.WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                hasError = false
            }

            override fun onReceivedError(
                view: WebView, request: android.webkit.WebResourceRequest,
                error: android.webkit.WebResourceError
            ) {
                if (request.isForMainFrame) {
                    hasError = true
                    showErrorPage()
                }
            }

            override fun onReceivedError(
                view: WebView, errorCode: Int, description: String?, failingUrl: String?
            ) {
                hasError = true
                showErrorPage()
            }

            override fun onReceivedSslError(
                view: WebView?, handler: SslErrorHandler?, error: SslError?
            ) {
                handler?.cancel()
                hasError = true
                showErrorPage()
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                onPageFinished?.invoke(url)
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                if (!hasError) hideErrorPage()
            }
        })

        webView.setWebChromeClient(object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                showProgress(newProgress)
                if (newProgress == 100) postDelayed({ hideProgress() }, 500)
            }

            override fun onShowFileChooser(
                webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                return onShowFileChooser?.invoke(filePathCallback, params)
                    ?: super.onShowFileChooser(webView, filePathCallback, params)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                onReceivedTitle?.invoke(title)
            }
        })

        binding.retryBtn.setOnClickListener { reload() }
    }

    // ---- 基础操作 ----
    fun loadUrl(url: String) = webView.loadUrl(url)
    fun reload() = webView.reload()

    private fun showProgress(progress: Int) {
        binding.progressBar.animate().cancel()
        binding.progressBar.alpha = 1f
        binding.progressBar.visibility = VISIBLE
        binding.progressBar.progress = progress
    }

    private fun hideProgress() {
        binding.progressBar.animate().alpha(0f).setDuration(300).withEndAction {
            binding.progressBar.visibility = GONE
            binding.progressBar.alpha = 1f
        }.start()
    }

    fun setProgressBarColors(start: Int, end: Int, bg: Int? = null) {
        if (bg != null) binding.progressBar.setBackgroundProgressColor(start, end, bg)
        else binding.progressBar.setColors(start, end)
    }

    private fun showErrorPage() {
        binding.errorLayout.visibility = VISIBLE
        webView.getView().visibility = GONE
    }

    private fun hideErrorPage() {
        binding.errorLayout.visibility = GONE
        webView.getView().visibility = VISIBLE
    }

    // ---- JS Bridge 核心方法 (瘦身后的版本) ----

    /**
     * 开启 JS 交互并设置默认 Bridge 名称 (window.app)
     */
    fun setJavaScriptInterface(isOpen: Boolean) {
        settings.setJavaScriptEnabled(isOpen)
    }


    /**
     * 原生增强：直接注入任意 Kotlin 对象作为 JS 接口
     * 这种方式最清晰，可适应不同的 JS 对象名和方法名
     * @param obj 带有 @JavascriptInterface 注解的对象
     * @param name JS 对象名 (如 "vendor")
     */
    fun addJavascriptInterface(obj:Any,name: String) {
        settings.setJavaScriptEnabled(true)
        bridgeNameList.add(name)
        webView.addJavascriptInterface(obj, name)
    }

    fun setCacheMode(model: Int = WebSettings.LOAD_NO_CACHE) = settings.setCacheMode(model)

    fun setFileOpenAccess(isOpen: Boolean = false) {
        settings.setAllowFileAccess(isOpen)
        settings.setAllowFileAccessFromFileURLs(isOpen)
        settings.setAllowUniversalAccessFromFileURLs(isOpen)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        destroyWebView()
    }

    fun destroyWebView() {
        if (isDestroyed) return
        isDestroyed = true
        try {
            webView.stopLoading()
            bridgeNameList.forEach {
                webView.removeJavascriptInterface(it)
            }

            webView.setWebViewClient(null)
            webView.setWebChromeClient(null)
            webView.clearHistory()
            webView.clearCache(true)
            webView.onPause()
            binding.webviewContainer.removeAllViews()
            webView.removeAllViews()
            webView.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}