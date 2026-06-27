package com.wkq.base.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.wkq.base.R

object LoadingDialog {

    fun show(
        context: Context,
        message: String = context.getString(R.string.base_loading),
        cancelable: Boolean = false,
        onDismiss: (() -> Unit)? = null
    ): PopupHandle {
        val popupView = LoadingPopupView(context, message, onDismiss)
        XPopup.Builder(context)
            .dismissOnTouchOutside(cancelable)
            .dismissOnBackPressed(cancelable)
            .moveUpToKeyboard(false)
            .hasShadowBg(true)
            .isViewMode(true)
            .enableDrag(false)
            .isDestroyOnDismiss(true)
            .asCustom(popupView)
            .show()
        return popupView.asHandle()
    }
}

private class LoadingPopupView(
    context: Context,
    private val message: String,
    private val onDismissCallback: (() -> Unit)?
) : CenterPopupView(context) {

    override fun addInnerContent() {
        centerPopupContainer.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(28), dp(24), dp(28), dp(22))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(12).toFloat()
                }

                addView(ProgressBar(context), LinearLayout.LayoutParams(dp(40), dp(40)))
                addView(TextView(context).apply {
                    text = message
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.rgb(23, 32, 51))
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(14)
                })
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        )
    }

    override fun onDismiss() {
        super.onDismiss()
        onDismissCallback?.invoke()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
