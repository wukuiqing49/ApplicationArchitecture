package com.wkq.base.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

import com.wkq.base.R

/**
 * 自定义空布局组件
 * 支持文字和图片居中展示，支持设置图片、文字、文字大小、颜色以及点击事件
 */
class EmptyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val ivEmpty: ImageView
    private val tvEmpty: TextView

    init {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.view_empty, this, true)
        ivEmpty = findViewById(R.id.iv_empty)
        tvEmpty = findViewById(R.id.tv_empty)

        // 读取自定义属性
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.EmptyView)
        
        val imageRes = typedArray.getResourceId(R.styleable.EmptyView_emptyImage, -1)
        if (imageRes != -1) {
            ivEmpty.setImageResource(imageRes)
        }

        val text = typedArray.getString(R.styleable.EmptyView_emptyText)
        if (!text.isNullOrEmpty()) {
            tvEmpty.text = text
        }

        val textColor = typedArray.getColor(R.styleable.EmptyView_emptyTextColor, Color.parseColor("#999999"))
        tvEmpty.setTextColor(textColor)

        val textSize = typedArray.getDimension(R.styleable.EmptyView_emptyTextSize, -1f)
        if (textSize != -1f) {
            tvEmpty.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        }

        typedArray.recycle()
    }

    /**
     * 设置空布局图片
     */
    fun setEmptyImage(@DrawableRes resId: Int) {
        ivEmpty.setImageResource(resId)
    }

    /**
     * 设置空布局文字
     */
    fun setEmptyText(text: CharSequence?) {
        tvEmpty.text = text
    }

    /**
     * 设置文字颜色
     */
    fun setEmptyTextColor(@ColorInt color: Int) {
        tvEmpty.setTextColor(color)
    }

    /**
     * 设置文字大小（单位：sp）
     */
    fun setEmptyTextSize(sizeSp: Float) {
        tvEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    /**
     * 设置点击事件
     */
    fun setOnEmptyClickListener(listener: OnClickListener?) {
        this.setOnClickListener(listener)
    }
}
