package com.wkq.ui.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView

/**
 * 旗舰自定义 PagerTitleView
 * 
 * 布局逻辑说明：
 * 1. [viewMargin]：控制圆角矩形背景离 View 物理边界的距离，从而实现 Tab 间的间距。
 * 2. [textPadding]：控制文字到圆角矩形背景边缘的距离，从而实现背景块的大小控制。
 * 
 * 实现方式：通过 onDraw 手动绘制背景矩形，完美避开 LayoutParams 被覆盖导致 margin 失效的问题。
 */
class GradientRoundPagerTitleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs), IPagerTitleView {

    // ========== 1. 文字样式配置 ==========
    var normalTextColor = Color.parseColor("#999999")
    var selectedTextColor = Color.parseColor("#333333")
    var normalTextSizeSp = 14f
    var selectedTextSizeSp = 16f

    // ========== 2. 背景样式配置 ==========
    var cornerRadiusPx = dp(15f)
    var selectedBgColors = intArrayOf(Color.parseColor("#FF4081"), Color.parseColor("#FF80AB"))
    var normalBgColors = intArrayOf(Color.parseColor("#EEEEEE"), Color.parseColor("#EEEEEE"))

    // ========== 3. 动态间距配置 ==========
    
    /** 文字距离圆角矩形背景边缘的内边距 (决定背景块大小) */
    var textPaddingHorizontal = dp(15f).toInt()
    var textPaddingVertical = dp(6f).toInt()

    /** 圆角矩形背景距离 View 边界的外部边距 (决定 Tab 间的间距) */
    var viewMarginHorizontal = dp(8f).toInt()
    var viewMarginVertical = dp(4f).toInt()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgRect = RectF()
    private var currentPercent = 0f // 滑动进度 0-1

    init {
        gravity = Gravity.CENTER
        includeFontPadding = false
        background = null // 禁用默认背景
    }

    /**
     * 设置完 padding/margin 属性后，调用此方法刷新布局测量
     */
    fun refresh() {
        // 总 Padding = 文字离背景的内距 + 背景离边界的外距
        val totalH = textPaddingHorizontal + viewMarginHorizontal
        val totalV = textPaddingVertical + viewMarginVertical
        setPadding(totalH, totalV, totalH, totalV)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        // 1. 计算背景矩形的绘制区域（扣除 viewMargin）
        bgRect.set(
            viewMarginHorizontal.toFloat(),
            viewMarginVertical.toFloat(),
            width.toFloat() - viewMarginHorizontal,
            height.toFloat() - viewMarginVertical
        )

        // 2. 根据滑动百分比融合颜色
        val startColor = blendColor(normalBgColors[0], selectedBgColors[0], currentPercent)
        val endColor = blendColor(normalBgColors.last(), selectedBgColors.last(), currentPercent)

        if (startColor != Color.TRANSPARENT || endColor != Color.TRANSPARENT) {
            if (startColor == endColor) {
                bgPaint.shader = null
                bgPaint.color = startColor
            } else {
                bgPaint.shader = LinearGradient(
                    bgRect.left, 0f, bgRect.right, 0f,
                    startColor, endColor, Shader.TileMode.CLAMP
                )
            }
            // 3. 绘制圆角矩形背景
            canvas.drawRoundRect(bgRect, cornerRadiusPx, cornerRadiusPx, bgPaint)
        }

        // 4. 调用 TextView 绘制文字
        super.onDraw(canvas)
    }

    // ========== IPagerTitleView 进度监听 ==========

    override fun onLeave(index: Int, totalCount: Int, leavePercent: Float, leftToRight: Boolean) {
        updateState(1f - leavePercent)
    }

    override fun onEnter(index: Int, totalCount: Int, enterPercent: Float, leftToRight: Boolean) {
        updateState(enterPercent)
    }

    private fun updateState(percent: Float) {
        currentPercent = percent
        // 文字颜色动态渐变
        setTextColor(blendColor(normalTextColor, selectedTextColor, percent))
        // 文字大小动态渐变
        val size = normalTextSizeSp + (selectedTextSizeSp - normalTextSizeSp) * percent
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        // 选中态自动加粗
        typeface = if (percent > 0.5f) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        invalidate()
    }

    override fun onSelected(index: Int, totalCount: Int) {}
    override fun onDeselected(index: Int, totalCount: Int) {}

    // ========== 颜色混合工具 ==========

    private fun blendColor(c1: Int, c2: Int, ratio: Float): Int {
        val ir = 1f - ratio
        val a = (Color.alpha(c1) * ir + Color.alpha(c2) * ratio).toInt()
        val r = (Color.red(c1) * ir + Color.red(c2) * ratio).toInt()
        val g = (Color.green(c1) * ir + Color.green(c2) * ratio).toInt()
        val b = (Color.blue(c1) * ir + Color.blue(c2) * ratio).toInt()
        return Color.argb(a, r, g, b)
    }

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
}
