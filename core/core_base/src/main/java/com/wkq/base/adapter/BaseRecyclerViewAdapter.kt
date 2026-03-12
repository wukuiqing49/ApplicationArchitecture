package com.wkq.base.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * 使用 ViewBinding 和泛型 Bean 的基础 RecyclerView 适配器
 */
abstract class BaseRecyclerViewAdapter<VB : ViewBinding, T>(
    protected val context: Context,
    private val inflate: (LayoutInflater, ViewGroup, Boolean) -> VB
) : RecyclerView.Adapter<BaseViewHolder<VB>>() {

    protected val inflater: LayoutInflater = LayoutInflater.from(context)
    protected val dataList = mutableListOf<T>()

    /**
     * 设置新数据
     */
    fun setData(newData: List<T>?) {
        dataList.clear()
        newData?.let { dataList.addAll(it) }
        notifyDataSetChanged()
    }

    /**
     * 追加多条数据 (用于上拉加载更多)
     */
    fun addData(newData: List<T>?) {
        if (!newData.isNullOrEmpty()) {
            val startPosition = dataList.size
            dataList.addAll(newData)
            notifyItemRangeInserted(startPosition, newData.size)
        }
    }

    /**
     * 添加数据
     */
    fun addData(item: T) {
        dataList.add(item)
        notifyItemInserted(dataList.size - 1)
    }

    /**
     * 获取数据列表
     */
    fun getData(): List<T> = dataList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        val binding = inflate(inflater, parent, false)
        return BaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
        convert(holder.binding, dataList[position], position)
    }

    override fun getItemCount(): Int = dataList.size

    /**
     * 数据绑定逻辑，由子类实现
     * @param binding ViewBinding 实例
     * @param item 数据实体
     * @param position 条目索引
     */
    abstract fun convert(binding: VB, item: T, position: Int)
}
