package com.wkq.base.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * 基础 ViewPager2 Fragment 适配器
 * 封装了常用的 Fragment 列表管理
 */
class BaseFragmentStateAdapter : FragmentStateAdapter {

    private val fragments = mutableListOf<Fragment>()

    constructor(fragmentActivity: FragmentActivity) : super(fragmentActivity)
    constructor(fragment: Fragment) : super(fragment)
    constructor(fragmentManager: FragmentManager, lifecycle: Lifecycle) : super(fragmentManager, lifecycle)

    /**
     * 设置新的 Fragment 列表
     */
    fun setFragments(newFragments: List<Fragment>) {
        fragments.clear()
        fragments.addAll(newFragments)
        notifyDataSetChanged()
    }

    /**
     * 添加单个 Fragment
     */
    fun addFragment(fragment: Fragment) {
        fragments.add(fragment)
        notifyItemInserted(fragments.size - 1)
    }

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}
