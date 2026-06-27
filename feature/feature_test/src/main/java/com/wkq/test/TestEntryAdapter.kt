package com.wkq.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wkq.test.databinding.ItemTestEntryBinding

class TestEntryAdapter(
    private val entries: List<TestEntry>
) : RecyclerView.Adapter<TestEntryAdapter.EntryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = ItemTestEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val item = entries[position]
        val showGroup = position == 0 || entries[position - 1].group != item.group
        holder.bind(item, showGroup)
    }

    override fun getItemCount(): Int = entries.size

    class EntryViewHolder(
        private val binding: ItemTestEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TestEntry, showGroup: Boolean) {
            binding.tvGroup.visibility = if (showGroup) View.VISIBLE else View.GONE
            binding.tvGroup.text = item.group
            binding.tvIcon.text = item.icon
            binding.tvTitle.text = item.title
            binding.tvDesc.text = item.desc
            binding.cardEntry.setOnClickListener { item.action.invoke() }
        }
    }
}
