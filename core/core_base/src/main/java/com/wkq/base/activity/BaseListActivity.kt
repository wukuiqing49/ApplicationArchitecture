package com.wkq.base.activity

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wkq.base.adapter.BaseRecyclerViewAdapter
import com.wkq.base.databinding.ViewBaseListBinding
import com.wkq.base.insets.SystemBarInsets
import com.wkq.base.widget.CommonTitleBar

/**
 * 完全封装的基础列表 Activity (无 ViewModel)
 * 外部只需传入数据实体类型 T
 */
abstract class BaseListActivity<T> : BaseActivity<ViewBaseListBinding>() {

    // ─── 重写 initViewBinding：BaseListActivity 固定使用 ViewBaseListBinding ──────
    override fun initViewBinding() {
        binding = ViewBaseListBinding.inflate(layoutInflater)
    }

    override fun applyDefaultSystemBarsInsets() {
        SystemBarInsets.applyScrollableBottomInset(binding.smartRefreshLayout)
        SystemBarInsets.applyBottomInset(binding.flFooterContainer)
    }

    protected fun setHeaderView(view: View) {
        binding.flHeaderContainer.removeAllViews()
        binding.flHeaderContainer.addView(view)
        if (view is CommonTitleBar) {
            view.applyStatusBarInset()
        }
    }

    protected fun setFooterView(view: View) {
        binding.flFooterContainer.removeAllViews()
        binding.flFooterContainer.addView(view)
    }

    // 默认起始页码
    protected var mPage = 1

    // 列表适配器
    protected lateinit var mAdapter: BaseRecyclerViewAdapter<*, T>

    override fun initView() {
        // 1. 设置 RecyclerView
        binding.recyclerView.layoutManager = getLayoutManager()
        mAdapter = createAdapter()
        binding.recyclerView.adapter = mAdapter

        // 2. 配置下拉刷新
        binding.smartRefreshLayout.setOnRefreshListener {
            mPage = 1
            loadListData(mPage)
        }

        // 3. 配置上拉加载更多
        binding.smartRefreshLayout.setOnLoadMoreListener {
            mPage++
            loadListData(mPage)
        }

        // 4. 配置 EmptyView 点击刷新
        binding.emptyView.setOnEmptyClickListener {
            autoRefreshList()
        }
    }

    override fun initData() {
        // 自动触发首次刷新
        autoRefreshList()
    }

    /**
     * 触发列表自动刷新，不向业务层暴露 SmartRefreshLayout。
     */
    protected fun autoRefreshList() {
        if (!isListUiActive()) return
        binding.smartRefreshLayout.autoRefresh()
    }

    /**
     * 设置空布局文案。
     */
    protected fun setEmptyText(text: CharSequence?) {
        if (!isListUiActive()) return
        binding.emptyView.setEmptyText(text)
    }

    /**
     * 显示空布局并隐藏列表。
     */
    protected fun showEmptyView(text: CharSequence? = null) {
        if (!isListUiActive()) return
        text?.let { binding.emptyView.setEmptyText(it) }
        binding.emptyView.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
    }

    /**
     * 显示列表内容并隐藏空布局。
     */
    protected fun showContentView() {
        if (!isListUiActive()) return
        binding.emptyView.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
    }

    /**
     * 结束下拉刷新和上拉加载动画。
     */
    protected fun stopRefreshAndLoadMore(success: Boolean = true) {
        if (!isListUiActive()) return
        binding.smartRefreshLayout.finishRefresh(success)
        binding.smartRefreshLayout.finishLoadMore(success)
    }

    /**
     * 提供默认的 LayoutManager (可被子类重写，如使用 GridLayoutManager)
     */
    protected open fun getLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(this)
    }

    /**
     * 网络请求完成时调用此方法
     * 自动处理上拉/下拉状态、数据组装、EmptyView 的显示
     *
     * @param data      拉取到的分页数据
     * @param hasMore   是否有下一页 (返回 false 会显示 "没有更多数据")
     */
    fun finishLoad(data: List<T>?, hasMore: Boolean) {
        if (!isListUiActive()) return

        val refreshLayout = binding.smartRefreshLayout
        
        refreshLayout.finishRefresh()
        refreshLayout.finishLoadMore()

        if (mPage == 1) {
            mAdapter.setData(data)
        } else {
            mAdapter.addData(data)
        }

        refreshLayout.setNoMoreData(!hasMore)

        if (mPage == 1 && (data == null || data.isEmpty())) {
            showEmptyView()
        } else {
            showContentView()
        }
    }

    /**
     * 请求失败时调用，统一结束刷新/加载动画并回退页码。
     */
    fun finishLoadFailed() {
        if (!isListUiActive()) return

        stopRefreshAndLoadMore(success = false)
        if (mPage > 1) {
            mPage--
        }
    }

    /**
     * 子类异步回调前可调用，避免页面销毁后继续更新 UI。
     */
    protected open fun isListUiActive(): Boolean {
        return !isFinishing && !isDestroyed && ::binding.isInitialized
    }

    /**
     * 子类必须提供具体的 Adapter 实例
     */
    abstract fun createAdapter(): BaseRecyclerViewAdapter<*, T>

    /**
     * 子类必须实现此方法以执行网络请求
     */
    abstract fun loadListData(page: Int)
}
