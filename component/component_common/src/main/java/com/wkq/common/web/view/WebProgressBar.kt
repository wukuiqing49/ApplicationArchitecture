package com.wkq.common.web.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * 自定义 Web 进度条
 * 支持渐变色、进度半圆（末端圆角）和背景色
 */
class WebProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var progress: Int
        get() = mProgress
        set(value) {
            this.mProgress = value.coerceIn(0, 100)
            postInvalidate()
        }

    private var mProgress = 0
    private var mStartColor = Color.parseColor("#FF4081") // 默认起始色
    private var mEndColor = Color.parseColor("#3F51B5")   // 默认结束色
    private var mBgColor = Color.parseColor("#E0E0E0")    // 默认背景色

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        mBgPaint.color = mBgColor
    }

    /**
     * 设置渐变颜色
     */
    fun setColors(startColor: Int, endColor: Int) {
        this.mStartColor = startColor
        this.mEndColor = endColor
        postInvalidate()
    }

    private val mPath = Path()

    /**
     * 一键设置颜色
     */
    fun setBackgroundProgressColor(startColor: Int, endColor: Int, bgColor: Int) {
        this.mStartColor = startColor
        this.mEndColor = endColor
        this.mBgColor = bgColor
        mBgPaint.color = bgColor
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        val radius = height / 2

        // 1. 绘制背景 (矩形)
        canvas.drawRect(0f, 0f, width, height, mBgPaint)

        // 2. 绘制进度
        if (mProgress > 0) {
            val progressWidth = width * (mProgress / 100f)

            // 创建渐变
            val gradient = LinearGradient(
                0f, 0f, progressWidth, 0f,
                mStartColor, mEndColor,
                Shader.TileMode.CLAMP
            )
            mPaint.shader = gradient

            if (mProgress < 100) {
                // 进度小于 100% 时，左边直角，右边圆角 (半圆)
                mPath.reset()
                val radii = floatArrayOf(
                    0f, 0f,           // Top-left
                    radius, radius,   // Top-right
                    radius, radius,   // Bottom-right
                    0f, 0f            // Bottom-left
                )
                mPath.addRoundRect(0f, 0f, progressWidth, height, radii, Path.Direction.CW)
                canvas.drawPath(mPath, mPaint)
            } else {
                // 进度 100% 时，变成一根直的线 (全直角矩形)
                canvas.drawRect(0f, 0f, width, height, mPaint)
            }
        }
    }
}