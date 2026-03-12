package com.wkq.ui.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.wkq.ui.R
import com.wkq.ui.databinding.ViewGradientShapeLabelBinding
import androidx.core.content.withStyledAttributes

/**
 * 自定义控件：中间文字，左右可加图，支持渐变背景和圆角
 */
class GradientShapeLabelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewGradientShapeLabelBinding = ViewGradientShapeLabelBinding.inflate(LayoutInflater.from(context), this)

    init {

        context.withStyledAttributes(attrs, R.styleable.GradientShapeLabelView) {

            // 1. 文字属性
            val text = getString(R.styleable.GradientShapeLabelView_label_text)
            val textColor = getColor(R.styleable.GradientShapeLabelView_label_text_color, Color.BLACK)
            val textSize = getDimensionPixelSize(R.styleable.GradientShapeLabelView_label_text_size, 0)

            binding.tvContent.text = text
            binding.tvContent.setTextColor(textColor)
            if (textSize > 0) {
                binding.tvContent.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
            }

            // 2. 图标属性
            val leftIcon = getResourceId(R.styleable.GradientShapeLabelView_label_left_icon, 0)
            val rightIcon = getResourceId(R.styleable.GradientShapeLabelView_label_right_icon, 0)
            val iconPadding =
                getDimensionPixelSize(R.styleable.GradientShapeLabelView_label_icon_padding, 0)
            val iconSize =
                getDimensionPixelSize(R.styleable.GradientShapeLabelView_label_icon_size, -1)

            if (leftIcon != 0) {
                binding.ivLeft.apply {
                    visibility = VISIBLE
                    setImageResource(leftIcon)
                    if (iconSize != -1) {
                        layoutParams = (layoutParams as LayoutParams).apply {
                            width = iconSize
                            height = iconSize
                        }
                    }
                }
            }

            if (rightIcon != 0) {
                binding.ivRight.apply {
                    visibility = VISIBLE
                    setImageResource(rightIcon)
                    if (iconSize != -1) {
                        layoutParams = (layoutParams as LayoutParams).apply {
                            width = iconSize
                            height = iconSize
                        }
                    }
                }
            }

            if (iconPadding > 0) {
                (binding.tvContent.layoutParams as LayoutParams).apply {
                    setMargins(iconPadding, 0, iconPadding, 0)
                }
            }

            // 3. 背景属性（渐变 + 圆角）
            val startColor =
                getColor(R.styleable.GradientShapeLabelView_label_bg_start_color, Color.TRANSPARENT)
            val endColor =
                getColor(R.styleable.GradientShapeLabelView_label_bg_end_color, Color.TRANSPARENT)
            val radius = getDimension(R.styleable.GradientShapeLabelView_label_bg_radius, 0f)
            val orientationIdx = getInt(R.styleable.GradientShapeLabelView_label_bg_orientation, 0)

            if (startColor != Color.TRANSPARENT) {
                val orientation = when (orientationIdx) {
                    0 -> GradientDrawable.Orientation.LEFT_RIGHT
                    1 -> GradientDrawable.Orientation.TOP_BOTTOM
                    2 -> GradientDrawable.Orientation.TL_BR
                    else -> GradientDrawable.Orientation.LEFT_RIGHT
                }

                val colors = if (endColor != Color.TRANSPARENT) {
                    intArrayOf(startColor, endColor)
                } else {
                    intArrayOf(startColor, startColor)
                }

                val shape = GradientDrawable(orientation, colors).apply {
                    cornerRadius = radius
                }
                background = shape
            }

        }
    }

    // ================= 暴露公共方法 =================

    fun setText(text: String?) {
        binding.tvContent.text = text
    }

    fun setLeftIcon(resId: Int) {
        if (resId != 0) {
            binding.ivLeft.visibility = VISIBLE
            binding.ivLeft.setImageResource(resId)
        } else {
            binding.ivLeft.visibility = GONE
        }
    }

    fun setRightIcon(resId: Int) {
        if (resId != 0) {
            binding.ivRight.visibility = VISIBLE
            binding.ivRight.setImageResource(resId)
        } else {
            binding.ivRight.visibility = GONE
        }

    }
}