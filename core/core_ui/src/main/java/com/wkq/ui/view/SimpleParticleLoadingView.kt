package com.wkq.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 *
 * @ Author: wkq
 *
 * @ Time: 2026/3/16 16:28
 *
 * @ Desc:

 */
class SimpleParticleLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val particleCount = 10
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var rotation = 0f
    private val particles = mutableListOf<Particle>()

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1500
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            rotation = it.animatedValue as Float * 360f
            invalidate()
        }
    }

    init {
        repeat(particleCount) {
            particles.add(
                Particle(
                    angle = it * (360f / particleCount),
                    scaleOffset = Random.nextFloat() * 1000
                )
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.25f

        canvas.save()
        canvas.rotate(rotation, cx, cy)

        val time = System.currentTimeMillis()

        particles.forEach { p ->

            val rad = Math.toRadians(p.angle.toDouble())

            val x = (cx + cos(rad) * radius).toFloat()
            val y = (cy + sin(rad) * radius).toFloat()

            // 不规则缩放（自然呼吸感）
            val scale = 0.5f + 0.5f * sin((time + p.scaleOffset) / 400.0).toFloat()

            val size = dp(4f) * scale

            canvas.drawCircle(x, y, size, paint)
        }

        canvas.restore()
    }

    private fun dp(v: Float): Float =
        v * resources.displayMetrics.density

    data class Particle(
        val angle: Float,
        val scaleOffset: Float
    )
}