package com.wkq.test.corebase

import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.wkq.base.BaseUiEvent
import com.wkq.base.BaseUiState
import com.wkq.base.activity.BaseActivity
import com.wkq.base.activity.BaseFullScreenActivity
import com.wkq.base.activity.BaseListActivity
import com.wkq.base.activity.BaseTitleActivity
import com.wkq.base.activity.BaseVMActivity
import com.wkq.base.activity.BaseVMFullScreenActivity
import com.wkq.base.activity.BaseVMListActivity
import com.wkq.base.activity.BaseVMTitleActivity
import com.wkq.base.adapter.BaseRecyclerViewAdapter
import com.wkq.base.insets.SystemBarInsets
import com.wkq.base.widget.CommonTitleBar
import com.wkq.router.annotation.Route
import com.wkq.test.databinding.ActivityCoreBaseFullScreenPageBinding
import com.wkq.test.databinding.ActivityCoreBaseSimplePageBinding

@Route(path = "/test/core_base/base_activity")
class CoreBaseActivityDemoActivity : BaseActivity<ActivityCoreBaseSimplePageBinding>() {

    private val adapter by lazy { CoreBaseSampleAdapter(this) }

    override fun initView() {
        binding.bindSimplePage(
            title = "BaseActivity",
            desc = "验证 BaseActivity 的 ViewBinding、系统栏样式和普通页面生命周期。",
            state = "binding=${binding::class.java.simpleName}"
        )
        binding.rvSamples.layoutManager = LinearLayoutManager(this)
        binding.rvSamples.adapter = adapter
    }

    override fun initData() {
        adapter.setData(
            listOf(
                CoreBaseSample("ViewBinding", "通过 BaseActivity 泛型反射完成 inflate。"),
                CoreBaseSample("System Bars", "默认 edge-to-edge，系统栏 Insets 由基类统一处理。")
            )
        )
    }
}

@Route(path = "/test/core_base/base_vm_activity")
class CoreBaseVMActivityDemoActivity :
    BaseVMActivity<ActivityCoreBaseSimplePageBinding, CoreBaseDemoViewModel>() {

    override fun initView() {
        binding.bindSimplePage(
            title = "BaseVMActivity",
            desc = "验证 BaseVMActivity 自动创建 ViewModel。",
            state = "等待 ViewModel 状态"
        )
        viewModel.state.observe(this) {
            binding.tvState.text = it
        }
        viewModel.uiStateLiveData.observe(this) {
            binding.tvDesc.text = it.toDemoText()
        }
        binding.tvState.setOnClickListener {
            viewModel.sendDemoToast()
            viewModel.sendDemoDialog()
            viewModel.sendDemoNavigate()
        }
    }

    override fun initData() {
        viewModel.markLoaded("BaseVMActivity")
    }

    override fun onBaseUiEvent(event: BaseUiEvent): Boolean {
        return when (event) {
            is BaseUiEvent.Navigate -> {
                com.wkq.router.api.Router.open(event.path, this)
                true
            }
            else -> false
        }
    }
}

@Route(path = "/test/core_base/base_title_activity")
class CoreBaseTitleActivityDemoActivity :
    BaseTitleActivity<ActivityCoreBaseSimplePageBinding>() {

    override fun initView() {
        setPageTitle("Title 页面 Demo")
        setLeftIcon(com.wkq.base.R.mipmap.ic_toolbar_back_black)
        setRightText("动作") {
            Toast.makeText(this, "右侧按钮点击正常", Toast.LENGTH_SHORT).show()
        }
        contentBinding.bindSimplePage(
            title = "Title 页面",
            desc = "使用 BaseTitleActivity + CommonTitleBar，验证返回、标题、右侧按钮和内容区域。",
            state = "contentBinding=${contentBinding::class.java.simpleName}"
        )
    }

    override fun initData() = Unit
}

@Route(path = "/test/core_base/base_vm_title_activity")
class CoreBaseVMTitleActivityDemoActivity :
    BaseVMTitleActivity<ActivityCoreBaseSimplePageBinding, CoreBaseDemoViewModel>() {

    override fun initView() {
        setPageTitle("BaseVMTitleActivity")
        setLeftIcon(com.wkq.base.R.mipmap.ic_toolbar_back_black)
        setRightText("刷新") {
            viewModel.markLoaded("BaseVMTitleActivity")
        }
        contentBinding.bindSimplePage(
            title = "BaseVMTitleActivity",
            desc = "验证标题栏容器和 ViewModel 自动创建。",
            state = "等待 ViewModel 状态"
        )
        viewModel.state.observe(this) {
            contentBinding.tvState.text = it
        }
        viewModel.uiStateLiveData.observe(this) {
            contentBinding.tvDesc.text = it.toDemoText()
        }
    }

    override fun initData() {
        viewModel.markLoaded("BaseVMTitleActivity")
    }
}

@Route(path = "/test/core_base/base_list_activity")
class CoreBaseListActivityDemoActivity : BaseListActivity<CoreBaseSample>() {

    override fun initView() {
        super.initView()
        setHeaderView(createListTitleBar("List 页面 Demo"))
    }

    override fun createAdapter(): BaseRecyclerViewAdapter<*, CoreBaseSample> {
        return CoreBaseSampleAdapter(this)
    }

    override fun loadListData(page: Int) {
        binding.recyclerView.postDelayed({
            finishLoad(createPageData("List 页面", page), hasMore = page < 2)
        }, 250L)
    }
}

@Route(path = "/test/core_base/base_vm_list_activity")
class CoreBaseVMListActivityDemoActivity :
    BaseVMListActivity<CoreBaseDemoViewModel, CoreBaseSample>() {

    override fun initView() {
        super.initView()
        setHeaderView(createListTitleBar("BaseVMListActivity"))
    }

    override fun createAdapter(): BaseRecyclerViewAdapter<*, CoreBaseSample> {
        return CoreBaseSampleAdapter(this)
    }

    override fun loadListData(page: Int) {
        viewModel.markLoaded("BaseVMListActivity page=$page")
        binding.recyclerView.postDelayed({
            finishLoad(createPageData("BaseVMListActivity", page), hasMore = page < 2)
        }, 250L)
    }
}

@Route(path = "/test/core_base/base_full_screen_activity")
class CoreBaseFullScreenActivityDemoActivity :
    BaseFullScreenActivity<ActivityCoreBaseFullScreenPageBinding>() {

    override fun initView() {
        binding.tvTitle.text = "FullScreen 页面"
        binding.tvDesc.text = "使用 BaseFullScreenActivity，验证隐藏系统栏和底部手势安全区。"
        SystemBarInsets.applyBottomInset(binding.contentPanel, extraBottom = dp(12))
        binding.btnFinish.setOnClickListener { finish() }
    }

    override fun initData() = Unit

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

@Route(path = "/test/core_base/base_vm_full_screen_activity")
class CoreBaseVMFullScreenActivityDemoActivity :
    BaseVMFullScreenActivity<ActivityCoreBaseFullScreenPageBinding, CoreBaseDemoViewModel>() {

    override fun initView() {
        binding.tvTitle.text = "BaseVMFullScreenActivity"
        binding.tvDesc.text = "验证全屏模式和 ViewModel 自动创建。"
        SystemBarInsets.applyBottomInset(binding.contentPanel, extraBottom = dp(12))
        binding.btnFinish.setOnClickListener { finish() }
        viewModel.state.observe(this) {
            binding.tvDesc.text = it
        }
        viewModel.uiStateLiveData.observe(this) {
            binding.tvTitle.text = it.toDemoText()
        }
    }

    override fun initData() {
        viewModel.markLoaded("BaseVMFullScreenActivity")
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

private fun ActivityCoreBaseSimplePageBinding.bindSimplePage(
    title: String,
    desc: String,
    state: String
) {
    tvTitle.text = title
    tvDesc.text = desc
    tvState.text = state
    SystemBarInsets.applyScrollableBottomInset(scrollView)
}

private fun createPageData(prefix: String, page: Int): List<CoreBaseSample> {
    return List(8) { index ->
        CoreBaseSample(
            title = "$prefix item ${(page - 1) * 8 + index + 1}",
            desc = "分页 page=$page，验证刷新、加载更多和 EmptyView 状态。"
        )
    }
}

private fun android.app.Activity.createListTitleBar(title: String): CommonTitleBar {
    return CommonTitleBar(this).apply {
        setBackgroundColor(0xFFFFFFFF.toInt())
        setTitle(title)
        setTitleColor(0xFF172033.toInt())
        setLeftIcon(com.wkq.base.R.mipmap.ic_toolbar_back_black)
        onLeftClickListener = { finish() }
    }
}

private fun BaseUiState.toDemoText(): String {
    return when (this) {
        BaseUiState.Idle -> "BaseUiState: Idle"
        BaseUiState.Content -> "BaseUiState: Content"
        is BaseUiState.Loading -> "BaseUiState: Loading ${message.orEmpty()}"
        is BaseUiState.Empty -> "BaseUiState: Empty ${message.orEmpty()}"
        is BaseUiState.Error -> "BaseUiState: Error $message"
    }
}
