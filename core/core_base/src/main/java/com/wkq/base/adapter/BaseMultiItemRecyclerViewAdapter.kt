package com.wkq.base

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.wkq.base.adapter.BaseViewHolder

/**
 * 支持多布局的基础 RecyclerView 适配器
 */
abstract class BaseMultiItemRecyclerViewAdapter<T : IBaseMultiItem> :
    RecyclerView.Adapter<BaseViewHolder<ViewBinding>>() {

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
     * 添加数据
     */
    fun addData(item: T) {
        dataList.add(item)
        notifyItemInserted(dataList.size - 1)
    }

    /**
     * 追加多条数据。
     */
    fun addData(newData: List<T>?) {
        if (newData.isNullOrEmpty()) return
        val startPosition = dataList.size
        dataList.addAll(newData)
        notifyItemRangeInserted(startPosition, newData.size)
    }

    /**
     * 更新指定位置的数据。
     */
    fun updateData(position: Int, item: T) {
        if (position !in dataList.indices) return
        dataList[position] = item
        notifyItemChanged(position)
    }

    /**
     * 删除指定位置的数据。
     */
    fun removeAt(position: Int) {
        if (position !in dataList.indices) return
        dataList.removeAt(position)
        notifyItemRemoved(position)
    }

    /**
     * 清空所有数据。
     */
    fun clearData() {
        dataList.clear()
        notifyDataSetChanged()
    }

    /**
     * 获取数据列表
     */
    fun getData(): List<T> = dataList

    override fun getItemViewType(position: Int): Int {
        return dataList[position].itemType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ViewBinding> {
        return onCreateMultiViewHolder(parent, viewType)
    }

    /**
     * 根据 viewType 创建对应的 ViewHolder
     * 子类需通过 ViewBinding.inflate(...) 返回对应的 BaseViewHolder
     */
    abstract fun onCreateMultiViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ViewBinding>

    override fun onBindViewHolder(holder: BaseViewHolder<ViewBinding>, position: Int) {
        convert(holder.binding, dataList[position], position)
    }

    override fun getItemCount(): Int = dataList.size

    /**
     * 数据绑定逻辑
     * @param binding ViewBinding 实例（需根据逻辑强转为具体的 Binding 类）
     * @param item 数据实体
     * @param position 条目索引
     */
    abstract fun convert(binding: ViewBinding, item: T, position: Int)
}
