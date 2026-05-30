package com.wkq.common.web

import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.annotation.DrawableRes
import com.lxj.xpopup.XPopup
import com.wkq.base.activity.BaseTitleActivity
import com.wkq.router.annotation.Route
import com.wkq.common.R
import com.wkq.common.databinding.ActivityCommonWebBinding
import com.wkq.common.web.util.JsBridge
import com.wkq.common.web.util.WebUrlUtil
import com.wkq.common.web.view.CommonWebView
import com.wkq.util.PhotoPickerHelper
import com.wkq.util.PickMediaType


/**
 * 通用 WebView Activity
 * 基于 BaseTitleActivity，支持标题栏文字、右侧菜单（文字/图标）的动态设置
 */
@Route(path = "/common/webview")
class CommonWebActivity : BaseTitleActivity<ActivityCommonWebBinding>() {

    companion object {
        const val KEY_URL = "url"
        const val KEY_TITLE = "title"
        const val KEY_OPEN_JS = "open_js"
    }

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var pickerHelper: PhotoPickerHelper
    private lateinit var title: String

    // 是否开启 js
    private val isOpenJS by lazy {
        intent.getBooleanExtra(KEY_OPEN_JS, false)
    }

    override fun initView() {
        var url = intent.getStringExtra(KEY_URL)
        title = intent.getStringExtra(KEY_TITLE) ?: ""
        url = WebUrlUtil.appendCommonParams(url, this)
        Log.e("CommonWebActivity", "url: $url")
        // 初始化标题
        if (!title.isNullOrEmpty()) {
            setPageTitle(title)
        }

        // 加载 URL
        if (!url.isNullOrEmpty()) {
            contentBinding.commonWebview.loadUrl(url)
        }

        binding.titleBar.setOnClickListener {
            showPhotoChooseDialog()
        }


        onBackPressedDispatcher.addCallback(this) {
            if (contentBinding.commonWebview.canGoBack() && contentBinding.commonWebview.getLoadUrl() != url) {
                contentBinding.commonWebview.goBack()
                return@addCallback
            }
            finish()

        }

    }

    override fun initData() {
        // 数据初始化逻辑
        pickerHelper = PhotoPickerHelper.with(this).register { uris ->
            if (uris.isNotEmpty()) {
                filePathCallback?.onReceiveValue(uris.toTypedArray())
            } else {
                filePathCallback?.onReceiveValue(null)
            }
            filePathCallback = null
        }

        contentBinding.commonWebview.onShowFileChooser = { callback, params ->
            this.filePathCallback = callback
            showPhotoChooseDialog()
            true
        }

        contentBinding.commonWebview.onReceivedTitle = { title ->
            if (!title.isNullOrEmpty() && this.title.isEmpty()) {
                updateTitle(title)
            }
        }
        contentBinding.commonWebview.setJavaScriptInterface(isOpenJS)
        if (isOpenJS) {
            // 方式 1：标准注册 (window.app.postMessage)
            contentBinding.commonWebview.addJavascriptInterface(JsBridge(this), "ThirdPlatformBridge")
        }
    }

    private fun showPhotoChooseDialog() {
        XPopup.Builder(this).asBottomList(
            getString(R.string.common_web_choose_action), arrayOf(
                getString(R.string.common_web_camera), getString(R.string.common_web_gallery)
            ), null, -1
        ) { position, _ ->
            when (position) {
                0 -> {
                    // Camera
                    requestAppPermissions(1, listOf(android.Manifest.permission.CAMERA))
                }

                1 -> {
                    // Gallery
                    pickerHelper.launch(PickMediaType.IMAGE_ONLY, false)
                }
            }
        }.show()
    }

    override fun authorized(permissionType: Int, permissionList: MutableList<String>) {
        if (permissionType == 1 && permissionList.contains(android.Manifest.permission.CAMERA)) {
            pickerHelper.launchCamera()
        } else {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    /**
     * 设置页面标题
     */
    fun updateTitle(title: String) {
        setPageTitle(title)
    }

    /**
     * 设置右侧文字菜单
     * @param text 文字内容
     * @param onClick 点击回调
     */
    fun setRightTextMenu(text: String, onClick: (() -> Unit)? = null) {
        setRightText(text, onClick)
    }

    /**
     * 设置右侧图标菜单
     * @param resId 图标资源 ID
     * @param onClick 点击回调
     */
    fun setRightIconMenu(@DrawableRes resId: Int, onClick: (() -> Unit)? = null) {
        setRightIcon(resId, onClick)
    }

    /**
     * 获取内部 WebView 实例进行高级操作
     */
    fun getWebView(): CommonWebView {
        return contentBinding.commonWebview
    }

    override fun onDestroy() {
        contentBinding.commonWebview.destroyWebView()
        super.onDestroy()
    }
}
