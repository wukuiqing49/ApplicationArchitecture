package com.wkq.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.*
import kotlin.random.Random

/**
 * 无缝循环粒子加载动画
 * 基于连续相位 sin，实现真正平滑循环
 */
class ParticleLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particleCount = 70
    private val particles = mutableListOf<Particle>()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 连续相位（0 → 2π 无限循环）
    private var phase = 0f

    private var animator: ValueAnimator? = null

    private val colors = intArrayOf(
        Color.parseColor("#FF8A00"),
        Color.parseColor("#FF00D6"),
        Color.parseColor("#00E0FF"),
        Color.parseColor("#7000FF")
    )

    init {
        initParticles()
    }

    private fun initParticles() {
        particles.clear()

        repeat(particleCount) {
            val angle = Random.nextFloat() * (2f * PI.toFloat())

            particles.add(
                Particle(
                    baseAngle = angle,
                    maxRadius = 150f + Random.nextFloat() * 120f,
                    size = 3f + Random.nextFloat() * 7f,
                    color = colors.random(),
                    speedFactor = 0.7f + Random.nextFloat() * 0.6f,
                    rotationSpeed = (if (Random.nextBoolean()) 1 else -1) *
                            (1f + Random.nextFloat() * 2f),
                    explode = Random.nextFloat() * 40f
                )
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    private fun startAnimation() {
        animator?.cancel()

        animator = ValueAnimator.ofFloat(0f, (2f * PI).toFloat()).apply {
            duration = 3200
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()

            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f

        // 连续循环函数：0 → 1 → 0 → 1 ...
        val t = (sin(phase) + 1f) / 2f

        // 平滑曲线（增强自然感）
        val smooth = t * t * sqrt(t)

        particles.forEach { p ->

            // 半径变化（聚合 + 分散）
            var radius = p.maxRadius * smooth * p.speedFactor

            // 爆发效果（中心附近）
            val explodeEffect = sin(phase * 2f) * 20f
            radius += explodeEffect * (1f - smooth)

            // 连续旋转
            val rotation = phase * p.rotationSpeed
            val angle = p.baseAngle + rotation

            val x = cx + radius * cos(angle)
            val y = cy + radius * sin(angle)

            // 呼吸透明度（自然渐变）
            val alphaFactor = (0.3f + 0.7f * sin(phase)).toFloat()
            paint.color = p.color
            paint.alpha = (alphaFactor * 255).toInt().coerceIn(0, 255)

            // 深度缩放
            val scale = 0.6f + smooth * 0.4f

            canvas.drawCircle(x, y, p.size * scale, paint)
        }
    }

    private data class Particle(
        val baseAngle: Float,
        val maxRadius: Float,
        val size: Float,
        val color: Int,
        val speedFactor: Float,
        val rotationSpeed: Float,
        val explode: Float
    )
}