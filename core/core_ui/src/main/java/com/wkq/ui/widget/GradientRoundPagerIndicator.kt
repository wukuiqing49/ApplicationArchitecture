package com.wkq.ui.widget

import android.content.Context
import android.graphics.*
import android.util.TypedValue
import android.view.View
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.model.PositionData

/**
 * 自定义 IPagerIndicator：支持圆角、渐变色、以及自定义宽高
 */
class GradientRoundPagerIndicator(context: Context) : View(context), IPagerIndicator {

    // 配置属性
    var indicatorHeight = dp(4f)
    var indicatorWidth = dp(20f)
    var cornerRadius = dp(2f)
    var startColor = Color.parseColor("#FF4081")
    var endColor = Color.parseColor("#FF80AB")
    var yOffset = dp(2f) // 距离底部的距离

    private var mPositionDataList: List<PositionData>? = null
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mRect = RectF()

    override fun onDraw(canvas: Canvas) {
        val colors = intArrayOf(startColor, endColor)
        mPaint.shader = LinearGradient(
            mRect.left, 0f, mRect.right, 0f,
            colors, null, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(mRect, cornerRadius, cornerRadius, mPaint)
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (mPositionDataList == null || mPositionDataList!!.isEmpty()) return

        // 计算当前位置和目标位置的数据
        val current = mPositionDataList!![Math.min(mPositionDataList!!.size - 1, position)]
        val next = mPositionDataList!![Math.min(mPositionDataList!!.size - 1, position + 1)]

        // 计算物理中心点
        val leftX = current.mLeft + (current.mRight - current.mLeft - indicatorWidth) / 2
        val nextLeftX = next.mLeft + (next.mRight - next.mLeft - indicatorWidth) / 2

        val currentLeft = leftX + (nextLeftX - leftX) * positionOffset
        
        mRect.left = currentLeft
        mRect.right = currentLeft + indicatorWidth
        mRect.top = height - indicatorHeight - yOffset
        mRect.bottom = height - yOffset

        invalidate()
    }

    override fun onPageSelected(position: Int) {}

    override fun onPageScrollStateChanged(state: Int) {}

    override fun onPositionDataProvide(dataList: MutableList<PositionData>?) {
        mPositionDataList = dataList
    }

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
}
