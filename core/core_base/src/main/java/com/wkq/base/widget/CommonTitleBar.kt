package com.wkq.base.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.wkq.base.R


/**
 * 灏佽鐨勫浘鏂?Toolbar
 * 鍖呭惈 宸︿晶(Image+Text) 涓棿(Text) 鍙充晶(Text+Image)
 */
class CommonTitleBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // 鎺т欢瀹氫箟
    private var llLeft: LinearLayout
    private var ivLeft: ImageView
    private var tvLeft: TextView

    private var tvCenter: TextView

    private var llRight: LinearLayout
    private var tvRight: TextView
    private var ivRight: ImageView

    // 鐐瑰嚮浜嬩欢鐩戝惉
    var onLeftClickListener: (() -> Unit)? = null
    var onRightClickListener: (() -> Unit)? = null

    init {
        // 鍔犺浇甯冨眬
        LayoutInflater.from(context).inflate(R.layout.view_common_title_bar, this, true)

        // 缁戝畾 ID
        llLeft = findViewById(R.id.ll_left_container)
        ivLeft = findViewById(R.id.iv_left_icon)
        tvLeft = findViewById(R.id.tv_left_text)

        tvCenter = findViewById(R.id.tv_center_title)

        llRight = findViewById(R.id.ll_right_container)
        tvRight = findViewById(R.id.tv_right_text)
        ivRight = findViewById(R.id.iv_right_icon)

        // 璇诲彇鑷畾涔夊睘鎬?
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CommonTitleBar)

            // 1. 涓棿鏍囬
            val title = typedArray.getString(R.styleable.CommonTitleBar_titleBar_title)
            val titleColor = typedArray.getColor(R.styleable.CommonTitleBar_titleBar_titleColor, ContextCompat.getColor(context, R.color.color_title_bar_text))
            val titleSize = typedArray.getDimensionPixelSize(R.styleable.CommonTitleBar_titleBar_titleSize, sp2px(18f))
            val titleStyle = typedArray.getInt(R.styleable.CommonTitleBar_titleBar_titleStyle, 0)
            
            setTitle(title)
            setTitleColor(titleColor)
            setTitleSize(titleSize)
            setTitleStyle(titleStyle)

            // 2. 宸︿晶閰嶇疆
            val leftIcon = typedArray.getResourceId(R.styleable.CommonTitleBar_titleBar_leftIcon, R.mipmap.ic_toolbar_back_black)
            val leftIconVisible = typedArray.getBoolean(R.styleable.CommonTitleBar_titleBar_leftIconVisible, false)
            val leftText = typedArray.getString(R.styleable.CommonTitleBar_titleBar_leftText)
            val leftTextColor = typedArray.getColor(R.styleable.CommonTitleBar_titleBar_leftTextColor, ContextCompat.getColor(context, R.color.color_title_bar_text))
            val leftTextSize = typedArray.getDimensionPixelSize(R.styleable.CommonTitleBar_titleBar_leftTextSize, sp2px(14f))

            if (leftIcon != 0) setLeftIcon(leftIcon)
            setLeftIconVisible(leftIconVisible)
            setLeftText(leftText)
            setLeftTextColor(leftTextColor)
            setLeftTextSize(leftTextSize)

            // 3. 鍙充晶閰嶇疆
            val rightIcon = typedArray.getResourceId(R.styleable.CommonTitleBar_titleBar_rightIcon, 0)
            val rightIconVisible = typedArray.getBoolean(R.styleable.CommonTitleBar_titleBar_rightIconVisible, false)
            val rightText = typedArray.getString(R.styleable.CommonTitleBar_titleBar_rightText)
            val rightTextColor = typedArray.getColor(R.styleable.CommonTitleBar_titleBar_rightTextColor, ContextCompat.getColor(context, R.color.color_title_bar_text))
            val rightTextSize = typedArray.getDimensionPixelSize(R.styleable.CommonTitleBar_titleBar_rightTextSize, sp2px(14f))

            if (rightIcon != 0) setRightIcon(rightIcon)
            setRightIconVisible(rightIconVisible)
            setRightText(rightText)
            setRightTextColor(rightTextColor)
            setRightTextSize(rightTextSize)

            typedArray.recycle()
        }

        // 榛樿鐐瑰嚮浜嬩欢鍒嗗彂
        llLeft.setOnClickListener { onLeftClickListener?.invoke() }
        llRight.setOnClickListener { onRightClickListener?.invoke() }
    }

    // --- 涓棿鏍囬寮€鏀炬柟娉?---
    fun setTitle(title: String?) {
        tvCenter.text = title ?: ""
    }

    fun setTitleColor(color: Int) {
        tvCenter.setTextColor(color)
    }

    fun setTitleSize(sizePx: Int) {
        tvCenter.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx.toFloat())
    }

    fun setTitleStyle(style: Int) {
        when (style) {
            1 -> tvCenter.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            2 -> tvCenter.typeface = Typeface.defaultFromStyle(Typeface.ITALIC)
            else -> tvCenter.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        }
    }

    // --- 宸︿晶寮€鏀炬柟娉?---
    fun setLeftIcon(resId: Int) {
        ivLeft.setImageResource(resId)
        setLeftIconVisible(true)
    }

    fun setLeftIconVisible(visible: Boolean) {
        ivLeft.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setLeftText(text: String?) {
        if (!text.isNullOrEmpty()) {
            tvLeft.text = text
            tvLeft.visibility = View.VISIBLE
        } else {
            tvLeft.visibility = View.GONE
        }
    }

    fun setLeftTextColor(color: Int) {
        tvLeft.setTextColor(color)
    }

    fun setLeftTextSize(sizePx: Int) {
        tvLeft.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx.toFloat())
    }

    // --- 鍙充晶寮€鏀炬柟娉?---
    fun setRightIcon(resId: Int) {
        ivRight.setImageResource(resId)
        setRightIconVisible(true)
    }

    fun setRightIconVisible(visible: Boolean) {
        ivRight.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setRightText(text: String?) {
        if (!text.isNullOrEmpty()) {
            tvRight.text = text
            tvRight.visibility = View.VISIBLE
        } else {
            tvRight.visibility = View.GONE
        }
    }

    fun setRightTextColor(color: Int) {
        tvRight.setTextColor(color)
    }

    fun setRightTextSize(sizePx: Int) {
        tvRight.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx.toFloat())
    }

    // DP/SP 杞?PX 宸ュ叿
    private fun sp2px(spValue: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            spValue,
            context.resources.displayMetrics
        ).toInt()
    }
}

