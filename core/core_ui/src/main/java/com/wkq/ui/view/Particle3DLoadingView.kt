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
 * @ Time: 2026/3/16 16:22
 *
 * @ Desc:

 */
class Particle3DLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val particles = mutableListOf<Particle>()
    private val particleCount = 120

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private var progress = 0f

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 4000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        repeat(particleCount) {
            particles.add(
                Particle(
                    angle = Random.nextFloat() * 360f,
                    radius = 0.6f + Random.nextFloat() * 0.4f,
                    depth = Random.nextFloat()
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

        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = min(width, height) * 0.28f

        particles.forEach { p ->

            val angle = p.angle + progress * (0.5f + p.depth)

            val rad = Math.toRadians(angle.toDouble())

            val r = baseRadius * p.radius

            val x = (cx + cos(rad) * r).toFloat()
            val y = (cy + sin(rad) * r).toFloat()

            val size = dp(1.5f + p.depth * 2f)

            paint.alpha = (120 + p.depth * 135).toInt()

            drawParticle(canvas, x, y, size, p)
        }
    }

    private fun drawParticle(canvas: Canvas, x: Float, y: Float, size: Float, p: Particle) {

        val tail = 4

        val rad = Math.toRadians((p.angle + progress).toDouble())

        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()

        for (i in 0 until tail) {

            val alpha = (paint.alpha * (1f - i / tail.toFloat())).toInt()
            paint.alpha = alpha

            canvas.drawCircle(
                x - dx * i * dp(3f),
                y - dy * i * dp(3f),
                size - i * 0.3f,
                paint
            )
        }
    }

    private fun dp(v: Float): Float {
        return v * resources.displayMetrics.density
    }

    data class Particle(
        var angle: Float,
        var radius: Float,
        var depth: Float
    )
}