package com.wkq.test

import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.wkq.base.activity.BaseActivity
import com.wkq.base.dialog.CommonDialog
import com.wkq.base.dialog.LoadingDialog
import com.wkq.base.insets.SystemBarInsets
import com.wkq.router.annotation.Route
import com.wkq.router.api.Router
import com.wkq.test.corebase.CoreBaseSample
import com.wkq.test.corebase.CoreBaseSampleAdapter
import com.wkq.test.databinding.ActivityCoreBaseDemoBinding

@Route(path = "/test/core_base")
class CoreBaseDemoActivity : BaseActivity<ActivityCoreBaseDemoBinding>() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val sampleAdapter by lazy { CoreBaseSampleAdapter(this, ::openBasePage) }

    override fun initView() {
        SystemBarInsets.applyScrollableBottomInset(binding.scrollView, extraBottom = dp(12))
        SystemBarInsets.applyHorizontalGestureInset(binding.actionPanel)
        binding.rvSamples.layoutManager = LinearLayoutManager(this)
        binding.rvSamples.adapter = sampleAdapter

        binding.btnConfirmDialog.setOnClickListener { showConfirmDialogDemo() }
        binding.btnLoadingDialog.setOnClickListener { showLoadingDialogDemo() }
        binding.btnRawDialog.setOnClickListener { showRawDialogDemo() }
        binding.emptyView.setOnEmptyClickListener {
            binding.emptyView.setEmptyText("EmptyView 点击事件正常：${System.currentTimeMillis()}")
        }
    }

    override fun initData() {
        sampleAdapter.setData(
            listOf(
                CoreBaseSample("Title 页面 Demo", "带 CommonTitleBar 的普通标题页，验证返回、右侧按钮和内容区域。"),
                CoreBaseSample("List 页面 Demo", "带 CommonTitleBar + SmartRefreshLayout + EmptyView 的列表页。"),
                CoreBaseSample("FullScreen 页面 Demo", "全屏沉浸页，验证系统栏隐藏和底部手势安全区。"),
                CoreBaseSample("Fragment 页面 Demo", "Fragment 宿主页，验证普通、VM、List Fragment 切换。"),
                CoreBaseSample("List 状态 Demo", "验证空数据、错误、加载中和多布局 Adapter。"),
                CoreBaseSample("Insets 适配", "当前页底部滚动区域处理导航栏，操作面板处理左右侧滑返回手势区。"),
                CoreBaseSample("基础 Adapter", "当前列表由 BaseRecyclerViewAdapter 驱动。")
            )
        )
    }

    private fun openBasePage(item: CoreBaseSample) {
        val path = when (item.title) {
            "Title 页面 Demo" -> "/test/core_base/base_title_activity"
            "List 页面 Demo" -> "/test/core_base/base_list_activity"
            "FullScreen 页面 Demo" -> "/test/core_base/base_full_screen_activity"
            "Fragment 页面 Demo" -> "/test/core_base/base_fragment"
            "List 状态 Demo" -> "/test/core_base/list_state_activity"
            "BaseActivity" -> "/test/core_base/base_activity"
            "BaseVMActivity" -> "/test/core_base/base_vm_activity"
            "BaseTitleActivity" -> "/test/core_base/base_title_activity"
            "BaseVMTitleActivity" -> "/test/core_base/base_vm_title_activity"
            "BaseListActivity" -> "/test/core_base/base_list_activity"
            "BaseVMListActivity" -> "/test/core_base/base_vm_list_activity"
            "BaseFullScreenActivity" -> "/test/core_base/base_full_screen_activity"
            "BaseVMFullScreenActivity" -> "/test/core_base/base_vm_full_screen_activity"
            "BaseFragment 系列" -> "/test/core_base/base_fragment"
            else -> null
        }
        if (path != null) {
            Router.open(path, this)
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun showConfirmDialogDemo() {
        CommonDialog.showConfirm(
            context = this,
            title = "通用确认弹框",
            message = "这是 core_base 提供的 CommonDialog.showConfirm，用于统一确认、取消、危险操作等交互。",
            confirmText = "确认",
            cancelText = "取消"
        ) {
            Toast.makeText(this, "点击了确认", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoadingDialogDemo() {
        val handle = LoadingDialog.show(this, message = "模拟加载中...")
        mainHandler.postDelayed({
            if (!isFinishing && !isDestroyed && handle.isShowing()) {
                handle.dismiss()
                Toast.makeText(this, "LoadingDialog 已关闭", Toast.LENGTH_SHORT).show()
            }
        }, 1200L)
    }

    private fun showRawDialogDemo() {
        val contentView = TextView(this).apply {
            text = "这是自定义内容弹框，可以承载任意 View。\n适合业务侧做统一样式的自定义弹窗。"
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(18), dp(20), dp(18))
            setTextColor(0xFF344054.toInt())
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        }
        CommonDialog.showRawCenter(this, contentView)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
