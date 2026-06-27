package com.wkq.test.corebase

import android.content.Context
import com.wkq.base.adapter.BaseRecyclerViewAdapter
import com.wkq.test.databinding.ItemCoreBaseSampleBinding

data class CoreBaseSample(
    val title: String,
    val desc: String
)

class CoreBaseSampleAdapter(
    context: Context,
    private val onItemClick: ((CoreBaseSample) -> Unit)? = null
) : BaseRecyclerViewAdapter<ItemCoreBaseSampleBinding, CoreBaseSample>(
    context = context,
    inflate = ItemCoreBaseSampleBinding::inflate
) {

    override fun convert(binding: ItemCoreBaseSampleBinding, item: CoreBaseSample, position: Int) {
        binding.tvTitle.text = item.title
        binding.tvDesc.text = "${item.desc}  position=$position"
        binding.root.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }
}
