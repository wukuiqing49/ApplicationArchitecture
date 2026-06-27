package com.wkq.test.corebase

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.wkq.base.BaseMultiItemRecyclerViewAdapter
import com.wkq.base.IBaseMultiItem
import com.wkq.base.activity.BaseListActivity
import com.wkq.base.adapter.BaseRecyclerViewAdapter
import com.wkq.base.adapter.BaseViewHolder
import com.wkq.base.widget.CommonTitleBar
import com.wkq.router.annotation.Route
import com.wkq.test.databinding.ItemCoreBaseSampleBinding
import com.wkq.test.databinding.ItemCoreBaseStateBannerBinding

@Route(path = "/test/core_base/list_state_activity")
class CoreBaseListStateDemoActivity : BaseListActivity<CoreBaseSample>() {

    private var mode = Mode.LOADING
    private lateinit var multiAdapter: StateMultiAdapter
    private lateinit var stateTextView: TextView

    override fun initView() {
        super.initView()
        setHeaderView(createHeader())
        setFooterView(createFooter())
    }

    override fun createAdapter(): BaseRecyclerViewAdapter<*, CoreBaseSample> {
        return CoreBaseSampleAdapter(this)
    }

    override fun loadListData(page: Int) {
        stateTextView.text = "当前模式：${mode.label}，page=$page"
        binding.recyclerView.postDelayed({
            if (!isListUiActive()) return@postDelayed
            when (mode) {
                Mode.LOADING -> {
                    finishLoad(createStatePageData(page), hasMore = page < 2)
                    stateTextView.text = "加载成功：已展示分页数据，继续上拉可验证加载更多。"
                }
                Mode.EMPTY -> {
                    setEmptyText("模拟空数据：点击可重新刷新")
                    finishLoad(emptyList(), hasMore = false)
                    stateTextView.text = "空数据：EmptyView 已显示，点击空布局可重新刷新。"
                }
                Mode.ERROR -> {
                    finishLoadFailed()
                    Toast.makeText(this, "模拟请求失败，刷新/加载动画已收起", Toast.LENGTH_SHORT).show()
                    stateTextView.text = "错误：已调用 finishLoadFailed()，列表保持上一次数据。"
                }
                Mode.MULTI -> {
                    finishLoad(createStatePageData(page), hasMore = false)
                    multiAdapter.setData(createMultiData())
                    multiAdapter.addData(
                        StateMultiItem(
                            StateMultiItem.TYPE_NORMAL,
                            "追加条目",
                            "验证 BaseMultiItemRecyclerViewAdapter.addData(item)。"
                        )
                    )
                    stateTextView.text = "多布局：底部区域展示 BaseMultiItemRecyclerViewAdapter。"
                }
            }
        }, 500L)
    }

    private fun createHeader(): CommonTitleBar {
        return CommonTitleBar(this).apply {
            setBackgroundColor(0xFFFFFFFF.toInt())
            setTitle("List 状态 Demo")
            setTitleColor(0xFF172033.toInt())
            setLeftIcon(com.wkq.base.R.mipmap.ic_toolbar_back_black)
            onLeftClickListener = { finish() }
        }
    }

    private fun createFooter(): LinearLayout {
        multiAdapter = StateMultiAdapter()
        stateTextView = TextView(this).apply {
            text = "当前模式：加载中"
            setTextColor(0xFF344054.toInt())
            textSize = 14f
            setPadding(dp(16), dp(12), dp(16), dp(10))
        }

        val modePanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(4), dp(12), dp(8))
        }

        Mode.values().forEach { item ->
            modePanel.addView(createModeButton(item))
        }

        val multiList = androidx.recyclerview.widget.RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@CoreBaseListStateDemoActivity)
            adapter = multiAdapter
            overScrollMode = android.view.View.OVER_SCROLL_NEVER
            isNestedScrollingEnabled = false
            setPadding(dp(12), 0, dp(12), dp(8))
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(stateTextView)
            addView(modePanel)
            addView(multiList)
        }
    }

    private fun createModeButton(item: Mode): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                marginEnd = dp(6)
            }
            gravity = android.view.Gravity.CENTER
            text = item.label
            textSize = 13f
            setTextColor(0xFF344054.toInt())
            setBackgroundResource(com.wkq.test.R.drawable.bg_test_filter_chip)
            setOnClickListener {
                mode = item
                if (item != Mode.MULTI) {
                    multiAdapter.setData(emptyList())
                }
                autoRefreshList()
            }
        }
    }

    private fun createStatePageData(page: Int): List<CoreBaseSample> {
        return List(6) { index ->
            CoreBaseSample(
                title = "状态列表 item ${(page - 1) * 6 + index + 1}",
                desc = "用于验证 BaseListActivity 的刷新、加载更多、错误和销毁保护。"
            )
        }
    }

    private fun createMultiData(): List<StateMultiItem> {
        return listOf(
            StateMultiItem(StateMultiItem.TYPE_BANNER, "多布局 Banner", "itemType=1，使用独立 Binding。"),
            StateMultiItem(StateMultiItem.TYPE_NORMAL, "普通条目 A", "itemType=2，复用普通列表 item。"),
            StateMultiItem(StateMultiItem.TYPE_NORMAL, "普通条目 B", "验证 BaseMultiItemRecyclerViewAdapter convert 分发。")
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private enum class Mode(val label: String) {
        LOADING("加载"),
        EMPTY("空"),
        ERROR("错误"),
        MULTI("多布局")
    }
}

private data class StateMultiItem(
    override val itemType: Int,
    val title: String,
    val desc: String
) : IBaseMultiItem {
    companion object {
        const val TYPE_BANNER = 1
        const val TYPE_NORMAL = 2
    }
}

private class StateMultiAdapter : BaseMultiItemRecyclerViewAdapter<StateMultiItem>() {

    override fun onCreateMultiViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<ViewBinding> {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ViewBinding = when (viewType) {
            StateMultiItem.TYPE_BANNER -> ItemCoreBaseStateBannerBinding.inflate(inflater, parent, false)
            else -> ItemCoreBaseSampleBinding.inflate(inflater, parent, false)
        }
        return BaseViewHolder(binding)
    }

    override fun convert(binding: ViewBinding, item: StateMultiItem, position: Int) {
        when (binding) {
            is ItemCoreBaseStateBannerBinding -> {
                binding.tvBannerTitle.text = item.title
                binding.tvBannerDesc.text = item.desc
            }
            is ItemCoreBaseSampleBinding -> {
                binding.tvTitle.text = item.title
                binding.tvDesc.text = "${item.desc}  position=$position"
            }
        }
    }
}
