package com.wkq.ui.view

/**
 *
 * @ Author: wkq
 *
 * @ Time: 2026/3/16 16:35
 *
 * @ Desc:

 */


import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class BreathingParticleLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particle(
        var baseX: Float,
        var baseY: Float,
        var radius: Float,
        var offsetX: Float,
        var offsetY: Float,
        var scale: Float,
        var alpha: Int,
        var phase: Float
    )

    private val particleCount = 20
    private val particles = mutableListOf<Particle>()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
    }

    private var animator: ValueAnimator? = null
    private var progress: Float = 0f

    init {
        startAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initParticles()
    }

    private fun initParticles() {
        particles.clear()

        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = minOf(width, height) / 6f

        repeat(particleCount) {
            val angle = Random.nextFloat() * 360f
            val distance = Random.nextFloat() * maxRadius

            val rad = Math.toRadians(angle.toDouble())
            val baseX = centerX + (cos(rad) * distance).toFloat()
            val baseY = centerY + (sin(rad) * distance).toFloat()

            particles.add(
                Particle(
                    baseX = baseX,
                    baseY = baseY,
                    radius = Random.nextFloat() * 8f + 6f,
                    offsetX = 0f,
                    offsetY = 0f,
                    scale = 1f,
                    alpha = 255,
                    phase = Random.nextFloat() * 360f
                )
            )
        }
    }

    private fun startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500L
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener {
                progress = it.animatedValue as Float
                updateParticles()
                invalidate()
            }

            start()
        }
    }

    private fun updateParticles() {
        val breathing = 0.85f + 0.15f * sin(progress * Math.PI * 2).toFloat()

        for (p in particles) {
            val localPhase = p.phase * Math.PI / 180f
            val moveRadius = 10f

            p.offsetX = (cos(progress * Math.PI * 2 + localPhase) * moveRadius).toFloat()
            p.offsetY = (sin(progress * Math.PI * 2 + localPhase) * moveRadius).toFloat()

            p.scale = breathing

            // 保持可见，不允许接近 0
            p.alpha = (150 + 105 * breathing).toInt()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (p in particles) {
            paint.alpha = p.alpha

            canvas.save()
            canvas.translate(p.baseX + p.offsetX, p.baseY + p.offsetY)
            canvas.scale(p.scale, p.scale)
            canvas.drawCircle(0f, 0f, p.radius, paint)
            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}