package com.wkq.base.activity

import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wkq.base.adapter.BaseRecyclerViewAdapter
import com.wkq.base.databinding.ViewBaseListBinding

import java.lang.reflect.ParameterizedType

/**
 * 完全封装的基础列表 Activity (无 ViewModel)
 * 外部只需传入数据实体类型 T
 */
abstract class BaseListActivity<T> : BaseActivity<ViewBaseListBinding>() {

    // ─── 重写 initViewBinding：BaseListActivity 固定使用 ViewBaseListBinding ──────
    override fun initViewBinding() {
        binding = ViewBaseListBinding.inflate(layoutInflater)
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
            binding.smartRefreshLayout.autoRefresh()
        }
    }

    override fun initData() {
        // 自动触发首次刷新
        binding.smartRefreshLayout.autoRefresh()
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
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
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
