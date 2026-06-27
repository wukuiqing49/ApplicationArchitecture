package com.wkq.base.activity

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.viewbinding.ViewBinding
import com.wkq.base.databinding.ViewTitleContentContainerBinding
import com.wkq.base.reflect.resolveGenericClass

abstract class BaseTitleActivity<ContentVB : ViewBinding> :
    BaseActivity<ViewTitleContentContainerBinding>() {

    protected enum class TitleContentLayoutMode {
        VERTICAL,
        OVERLAY
    }

    protected lateinit var contentBinding: ContentVB

    @Suppress("UNCHECKED_CAST")
    override fun initViewBinding() {
        binding = ViewTitleContentContainerBinding.inflate(layoutInflater)

        val clazz = resolveGenericClass<ContentVB>(this, 0)
        val method = clazz.getMethod("inflate", LayoutInflater::class.java)
        contentBinding = method.invoke(null, layoutInflater) as ContentVB

        binding.flContent.addView(contentBinding.root)
    }

    protected fun setTitleContentLayoutMode(mode: TitleContentLayoutMode) {
        val set = ConstraintSet()
        set.clone(binding.root)

        if (mode == TitleContentLayoutMode.OVERLAY) {
            set.connect(
                binding.flContent.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                0
            )
        } else {
            set.connect(
                binding.flContent.id,
                ConstraintSet.TOP,
                binding.titleBar.id,
                ConstraintSet.BOTTOM,
                0
            )
        }

        set.applyTo(binding.root)
    }

    protected fun setContentOverlapTitleBar(overlap: Boolean) {
        setTitleContentLayoutMode(
            if (overlap) TitleContentLayoutMode.OVERLAY else TitleContentLayoutMode.VERTICAL
        )
    }

    /**
     * 让内容区域位于 title_bar 下方，属于普通竖排布局。
     */
    protected fun setContentBelowTitleBar() {
        setTitleContentLayoutMode(TitleContentLayoutMode.VERTICAL)
    }

    /**
     * Keep the whole page full-screen and transparent, but push one child view below the title bar.
     */
    protected fun setViewBelowTitleBar(view: View, extraTopMarginPx: Int = 0) {
        view.post {
            val titleBarBottom = binding.titleBar.bottom + extraTopMarginPx
            val params = view.layoutParams
            when (params) {
                is ViewGroup.MarginLayoutParams -> {
                    if (params.topMargin != titleBarBottom) {
                        params.topMargin = titleBarBottom
                        view.layoutParams = params
                    }
                }
                else -> {
                    view.setPadding(
                        view.paddingLeft,
                        titleBarBottom,
                        view.paddingRight,
                        view.paddingBottom
                    )
                }
            }
        }
    }

    override fun applyDefaultSystemBarsInsets() {
        // 标题页由 CommonTitleBar 处理顶部 Insets，内容区默认约束在标题栏下方。
    }

    override fun initImmersionBar() {
        super.initImmersionBar()
        binding.titleBar.applyStatusBarInset()
        setTitleContentLayoutMode(TitleContentLayoutMode.VERTICAL)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.titleBar.onLeftClickListener = { finish() }
    }

    fun showTitleFullScreen() {
        binding.titleBar.setLeftIconVisible(false)
        setTitleContentLayoutMode(TitleContentLayoutMode.OVERLAY)
        binding.titleBar.setBackgroundColor(Color.TRANSPARENT)
    }

    protected fun setPageTitle(title: String) {
        binding.titleBar.setTitle(title)
    }

    protected fun setPageTitleColor(color: Int) {
        binding.titleBar.setTitleColor(color)
    }

    protected fun setPageTitleSize(sizePx: Int) {
        binding.titleBar.setTitleSize(sizePx)
    }

    protected fun setPageTitleSizeSp(sizeSp: Float) {
        binding.titleBar.setTitleSizeSp(sizeSp)
    }

    protected fun setPageTitle(
        title: String,
        color: Int? = null,
        sizePx: Int? = null
    ) {
        binding.titleBar.setTitle(title)
        color?.let { binding.titleBar.setTitleColor(it) }
        sizePx?.let { binding.titleBar.setTitleSize(it) }
    }

    protected fun setRightText(text: String, onClick: (() -> Unit)? = null) {
        binding.titleBar.setRightText(text)
        onClick?.let { binding.titleBar.onRightClickListener = it }
    }

    protected fun setRightTextColor(color: Int) {
        binding.titleBar.setRightTextColor(color)
    }

    protected fun setRightTextSize(sizePx: Int) {
        binding.titleBar.setRightTextSize(sizePx)
    }

    protected fun setRightTextSizeSp(sizeSp: Float) {
        binding.titleBar.setRightTextSizeSp(sizeSp)
    }

    protected fun setRightText(
        text: String,
        color: Int? = null,
        sizePx: Int? = null,
        onClick: (() -> Unit)? = null
    ) {
        binding.titleBar.setRightText(text)
        color?.let { binding.titleBar.setRightTextColor(it) }
        sizePx?.let { binding.titleBar.setRightTextSize(it) }
        onClick?.let { binding.titleBar.onRightClickListener = it }
    }

    protected fun setRightIcon(@DrawableRes resId: Int, onClick: (() -> Unit)? = null) {
        binding.titleBar.setRightIcon(resId)
        onClick?.let { binding.titleBar.onRightClickListener = it }
    }

    protected fun clearRightIcon() {
        binding.titleBar.clearRightIcon()
    }

    protected fun setRightIconVisible(visible: Boolean) {
        binding.titleBar.setRightIconVisible(visible)
    }

    protected fun setLeftText(text: String) {
        binding.titleBar.setLeftText(text)
    }

    protected fun setLeftTextColor(color: Int) {
        binding.titleBar.setLeftTextColor(color)
    }

    protected fun setLeftTextSize(sizePx: Int) {
        binding.titleBar.setLeftTextSize(sizePx)
    }

    protected fun setLeftTextSizeSp(sizeSp: Float) {
        binding.titleBar.setLeftTextSizeSp(sizeSp)
    }

    protected fun setLeftText(
        text: String,
        color: Int? = null,
        sizePx: Int? = null,
        onClick: (() -> Unit)? = null
    ) {
        binding.titleBar.setLeftText(text)
        color?.let { binding.titleBar.setLeftTextColor(it) }
        sizePx?.let { binding.titleBar.setLeftTextSize(it) }
        onClick?.let { binding.titleBar.onLeftClickListener = it }
    }

    protected fun setLeftIcon(@DrawableRes resId: Int, onClick: (() -> Unit)? = null) {
        binding.titleBar.setLeftIcon(resId)
        onClick?.let { binding.titleBar.onLeftClickListener = it }
    }

    protected fun clearLeftIcon() {
        binding.titleBar.clearLeftIcon()
    }

    protected fun setLeftVisible(visible: Boolean) {
        binding.titleBar.setLeftIconVisible(visible)
    }

    protected fun setLeftClickListener(block: () -> Unit) {
        binding.titleBar.onLeftClickListener = block
    }
}
