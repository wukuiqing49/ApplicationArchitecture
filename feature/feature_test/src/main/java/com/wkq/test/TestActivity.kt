package com.wkq.test

import android.view.Gravity
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.wkq.base.activity.BaseActivity
import com.wkq.router.annotation.Route
import com.wkq.test.databinding.ActivityTestBinding

/**
 * 测试主入口页面。
 */
@Route(path = "/test/main")
class TestActivity : BaseActivity<ActivityTestBinding>() {

    private val entries by lazy { TestEntryRegistry.createMainEntries(this) }

    override fun initView() {
        binding.tvSummary.text = "${entries.size} 个测试入口，按能力分组"
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = TestEntryAdapter(entries)

        bindGroupFilters()
    }

    override fun initData() = Unit

    private fun bindGroupFilters() {
        binding.chipGroup.removeAllViews()
        entries.map { it.group }
            .distinct()
            .forEach { group ->
                binding.chipGroup.addView(createGroupChip(group))
            }
    }

    private fun createGroupChip(group: String): TextView {
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.test_chip_horizontal_padding)
        return TextView(this).apply {
            text = group
            gravity = Gravity.CENTER
            minWidth = resources.getDimensionPixelSize(R.dimen.test_chip_min_width)
            height = resources.getDimensionPixelSize(R.dimen.test_chip_height)
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            setTextColor(0xFF344054.toInt())
            textSize = 13f
            background = getDrawable(R.drawable.bg_test_filter_chip)
            foreground = obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground)).let {
                val drawable = it.getDrawable(0)
                it.recycle()
                drawable
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { scrollToGroup(group) }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                resources.getDimensionPixelSize(R.dimen.test_chip_height)
            ).apply {
                marginEnd = resources.getDimensionPixelSize(R.dimen.test_chip_spacing)
            }
        }
    }

    private fun scrollToGroup(group: String) {
        val index = entries.indexOfFirst { it.group == group }
        if (index >= 0) {
            binding.recyclerView.smoothScrollToPosition(index)
        }
    }
}
