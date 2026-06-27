package com.wkq.base.insets

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object SystemBarInsets {

    fun applySystemBarsInset(
        view: View,
        includeTop: Boolean = true,
        includeBottom: Boolean = true,
        includeHorizontal: Boolean = false,
        includeIme: Boolean = false,
        includeGestureInset: Boolean = true
    ) {
        val initial = view.snapshot()
        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val topInset = if (includeTop) {
                insets.getInsets(
                    WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
                ).top
            } else {
                0
            }
            val bottomInset = if (includeBottom) {
                insets.resolveBottomInset(includeIme, includeGestureInset)
            } else {
                0
            }
            val horizontalInset = if (includeHorizontal) {
                insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            } else {
                null
            }
            target.setPadding(
                initial.left + (horizontalInset?.left ?: 0),
                initial.top + topInset,
                initial.right + (horizontalInset?.right ?: 0),
                initial.bottom + bottomInset
            )
            insets
        }
        view.requestApplyInsetsWhenAttached()
    }

    fun applyTopInset(
        view: View,
        resizeHeight: Boolean = true
    ) {
        val initial = view.snapshot()
        val initialHeight = view.layoutParams.height
        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val topInset = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
            if (resizeHeight && initialHeight > 0) {
                target.updateHeight(initialHeight + topInset)
            }
            target.setPadding(
                initial.left,
                initial.top + topInset,
                initial.right,
                initial.bottom
            )
            insets
        }
        view.requestApplyInsetsWhenAttached()
    }

    fun applyBottomInset(
        view: View,
        resizeHeight: Boolean = false,
        includeIme: Boolean = false,
        extraBottom: Int = 0,
        includeGestureInset: Boolean = true
    ) {
        val initial = view.snapshot()
        val initialHeight = view.layoutParams.height
        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val bottomInset = insets.resolveBottomInset(includeIme, includeGestureInset)
            if (resizeHeight && initialHeight > 0) {
                target.updateHeight(initialHeight + bottomInset)
            }
            target.setPadding(
                initial.left,
                initial.top,
                initial.right,
                initial.bottom + bottomInset + extraBottom
            )
            insets
        }
        view.requestApplyInsetsWhenAttached()
    }

    fun applyScrollableBottomInset(
        view: View,
        extraBottom: Int = 0,
        includeIme: Boolean = false,
        includeGestureInset: Boolean = true
    ) {
        (view as? ViewGroup)?.clipToPadding = false
        applyBottomInset(
            view = view,
            resizeHeight = false,
            includeIme = includeIme,
            extraBottom = extraBottom,
            includeGestureInset = includeGestureInset
        )
    }

    fun applyBottomMarginInset(
        view: View,
        includeIme: Boolean = false,
        extraBottom: Int = 0,
        includeGestureInset: Boolean = true
    ) {
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val initialBottomMargin = params.bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val targetParams = target.layoutParams as? ViewGroup.MarginLayoutParams
            if (targetParams != null) {
                targetParams.bottomMargin =
                    initialBottomMargin + insets.resolveBottomInset(includeIme, includeGestureInset) + extraBottom
                target.layoutParams = targetParams
            }
            insets
        }
        view.requestApplyInsetsWhenAttached()
    }

    /**
     * 为左右侧滑返回手势预留安全区，适合横向滑动组件或贴边操作按钮。
     *
     * 注意：不要全局无脑套在所有根布局上，否则内容会被过度收窄。
     */
    fun applyHorizontalGestureInset(
        view: View,
        applyLeft: Boolean = true,
        applyRight: Boolean = true,
        extraLeft: Int = 0,
        extraRight: Int = 0
    ) {
        val initial = view.snapshot()
        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            target.setPadding(
                initial.left + if (applyLeft) gestures.left + extraLeft else 0,
                initial.top,
                initial.right + if (applyRight) gestures.right + extraRight else 0,
                initial.bottom
            )
            insets
        }
        view.requestApplyInsetsWhenAttached()
    }

    private fun WindowInsetsCompat.resolveBottomInset(
        includeIme: Boolean,
        includeGestureInset: Boolean
    ): Int {
        val navigationBottom = getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        val gestureBottom = if (includeGestureInset) {
            maxOf(
                getInsets(WindowInsetsCompat.Type.systemGestures()).bottom,
                getInsets(WindowInsetsCompat.Type.mandatorySystemGestures()).bottom
            )
        } else {
            0
        }
        val imeBottom = if (includeIme) {
            getInsets(WindowInsetsCompat.Type.ime()).bottom
        } else {
            0
        }
        return maxOf(navigationBottom, gestureBottom, imeBottom)
    }

    private fun View.snapshot(): PaddingSnapshot {
        return PaddingSnapshot(paddingLeft, paddingTop, paddingRight, paddingBottom)
    }

    private fun View.requestApplyInsetsWhenAttached() {
        if (isAttachedToWindow) {
            ViewCompat.requestApplyInsets(this)
            return
        }
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                ViewCompat.requestApplyInsets(v)
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }

    private fun View.updateHeight(height: Int) {
        val params = layoutParams ?: return
        if (params.height == height) return
        params.height = height
        layoutParams = params
    }

    private data class PaddingSnapshot(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )
}
