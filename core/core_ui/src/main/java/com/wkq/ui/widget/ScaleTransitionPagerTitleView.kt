package com.wkq.ui.widget

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView

/**
 * 自定义 PagerTitleView：支持颜色和字体大小切换
 */
class ScaleTransitionPagerTitleView(context: Context) : ColorTransitionPagerTitleView(context) {

    var minScale = 0.85f // 未选中时的缩放比例
    var selectedTextSize = 18f // 选中时的字体大小 (sp)
    var normalTextSize = 15f   // 未选中时的字体大小 (sp)

    init {
        // 初始设为未选中状态的大小
        setTextSize(TypedValue.COMPLEX_UNIT_SP, normalTextSize)
    }

    override fun onSelected(index: Int, totalCount: Int) {
        super.onSelected(index, totalCount)
        // 选中时加粗加颜色
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun onDeselected(index: Int, totalCount: Int) {
        super.onDeselected(index, totalCount)
        // 未选中时常规字体
        typeface = Typeface.DEFAULT
    }

    /**
     * @param leavePercent 离开百分比，0.0f -> 1.0f
     */
    override fun onLeave(index: Int, totalCount: Int, leavePercent: Float, leftToRight: Boolean) {
        super.onLeave(index, totalCount, leavePercent, leftToRight)
        // 字体大小过渡：从 selectedSize 变回 normalSize
        val size = selectedTextSize + (normalTextSize - selectedTextSize) * leavePercent
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    /**
     * @param enterPercent 进入百分比，0.0f -> 1.0f
     */
    override fun onEnter(index: Int, totalCount: Int, enterPercent: Float, leftToRight: Boolean) {
        super.onEnter(index, totalCount, enterPercent, leftToRight)
        // 字体大小过渡：从 normalSize 变为 selectedSize
        val size = normalTextSize + (selectedTextSize - normalTextSize) * enterPercent
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }
}
