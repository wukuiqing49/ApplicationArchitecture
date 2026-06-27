package com.wkq.base.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.wkq.base.R

object CommonDialog {

    fun showConfirm(
        context: Context,
        title: String,
        message: String,
        confirmText: String = context.getString(R.string.base_confirm),
        cancelText: String = context.getString(R.string.base_cancel),
        confirmDanger: Boolean = false,
        cancelable: Boolean = true,
        onCancel: (() -> Unit)? = null,
        onConfirm: () -> Unit
    ): PopupHandle {
        val messageView = TextView(context).apply {
            text = message
            setTextColor(TEXT_SECONDARY)
            textSize = 14f
            gravity = Gravity.CENTER
            setLineSpacing(context.dp(2).toFloat(), 1.0f)
            setPadding(context.dp(20), context.dp(4), context.dp(20), context.dp(2))
        }
        return showContent(
            context = context,
            title = title,
            contentView = messageView,
            confirmText = confirmText,
            cancelText = cancelText,
            confirmDanger = confirmDanger,
            cancelable = cancelable,
            onConfirm = {
                onConfirm()
                true
            },
            onCancel = onCancel
        )
    }

    fun showContent(
        context: Context,
        title: String,
        contentView: View,
        confirmText: String = context.getString(R.string.base_confirm),
        cancelText: String = context.getString(R.string.base_cancel),
        neutralText: String? = null,
        confirmDanger: Boolean = false,
        scrollable: Boolean = true,
        cancelable: Boolean = true,
        onConfirm: (() -> Boolean)? = null,
        onNeutral: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ): PopupHandle {
        val popupView = CommonCenterPopupView(
            context = context,
            title = title,
            popupContentView = contentView,
            confirmText = confirmText,
            cancelText = cancelText,
            neutralText = neutralText,
            confirmDanger = confirmDanger,
            scrollable = scrollable,
            onConfirm = onConfirm,
            onNeutral = onNeutral,
            onCancel = onCancel,
            onDismissCallback = onDismiss
        )
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

    fun showRawCenter(
        context: Context,
        contentView: View,
        cancelable: Boolean = true,
        onDismiss: (() -> Unit)? = null
    ): PopupHandle {
        val popupView = CommonRawCenterPopupView(context, contentView, onDismiss)
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

    private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

private class CommonRawCenterPopupView(
    context: Context,
    private val popupContentView: View,
    private val onDismissCallback: (() -> Unit)?
) : CenterPopupView(context) {

    override fun addInnerContent() {
        centerPopupContainer.setBackgroundColor(Color.TRANSPARENT)
        (popupContentView.parent as? ViewGroup)?.removeView(popupContentView)
        val popupWidth = (resources.displayMetrics.widthPixels * 0.90f).toInt()
            .coerceAtMost(dp(460))
        centerPopupContainer.addView(
            popupContentView,
            FrameLayout.LayoutParams(popupWidth, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
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

private class CommonCenterPopupView(
    context: Context,
    private val title: String,
    private val popupContentView: View,
    private val confirmText: String,
    private val cancelText: String,
    private val neutralText: String?,
    private val confirmDanger: Boolean,
    private val scrollable: Boolean,
    private val onConfirm: (() -> Boolean)?,
    private val onNeutral: (() -> Unit)?,
    private val onCancel: (() -> Unit)?,
    private val onDismissCallback: (() -> Unit)?
) : CenterPopupView(context) {

    override fun addInnerContent() {
        val popupWidth = (resources.displayMetrics.widthPixels * 0.86f).toInt()
            .coerceAtMost(dp(420))
        centerPopupContainer.addView(
            buildRoot(),
            FrameLayout.LayoutParams(popupWidth, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
        )
    }

    override fun onDismiss() {
        super.onDismiss()
        onDismissCallback?.invoke()
    }

    private fun buildRoot(): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundRect(Color.WHITE, dp(12).toFloat())
            setPadding(dp(20), dp(18), dp(20), dp(16))

            addView(TextView(context).apply {
                text = title
                setTextColor(TEXT_PRIMARY)
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                includeFontPadding = false
            }, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

            addView(createContentContainer(), LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            addView(createActions(), LinearLayout.LayoutParams.MATCH_PARENT, dp(44))
        }
    }

    private fun createContentContainer(): View {
        (popupContentView.parent as? ViewGroup)?.removeView(popupContentView)
        val topMargin = dp(16)
        val maxHeight = (resources.displayMetrics.heightPixels * 0.64f).toInt()
        return if (scrollable) {
            ScrollView(context).apply {
                isFillViewport = false
                addView(
                    popupContentView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, topMargin, 0, dp(18))
                }
                post {
                    if (height > maxHeight) {
                        layoutParams = (layoutParams as LinearLayout.LayoutParams).apply {
                            height = maxHeight
                        }
                    }
                }
            }
        } else {
            FrameLayout(context).apply {
                addView(
                    popupContentView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, topMargin, 0, dp(18))
                }
            }
        }
    }

    private fun createActions(): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            neutralText?.let {
                addView(actionButton(it, outlined = true) {
                    onNeutral?.invoke()
                    dismiss()
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
                addSpacer()
            }
            addView(actionButton(cancelText, outlined = true) {
                onCancel?.invoke()
                dismiss()
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            addSpacer()
            addView(actionButton(confirmText, outlined = false, danger = confirmDanger) {
                val shouldDismiss = onConfirm?.invoke() ?: true
                if (shouldDismiss) dismiss()
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun LinearLayout.addSpacer() {
        addView(View(context), LinearLayout.LayoutParams(dp(10), 1))
    }

    private fun actionButton(
        textValue: String,
        outlined: Boolean,
        danger: Boolean = false,
        action: () -> Unit
    ): TextView {
        val fillColor = when {
            outlined -> Color.TRANSPARENT
            danger -> DANGER
            else -> PRIMARY
        }
        val textColor = when {
            outlined -> TEXT_PRIMARY
            else -> Color.WHITE
        }
        return TextView(context).apply {
            text = textValue
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            background = roundRect(
                color = fillColor,
                radius = dp(8).toFloat(),
                strokeColor = if (outlined) DIVIDER else Color.TRANSPARENT
            )
            setOnClickListener { action() }
        }
    }

    private fun roundRect(color: Int, radius: Float, strokeColor: Int = Color.TRANSPARENT): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            if (strokeColor != Color.TRANSPARENT) {
                setStroke(dp(1), strokeColor)
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

internal fun CenterPopupView.asHandle(): PopupHandle {
    return object : PopupHandle {
        override fun dismiss() {
            this@asHandle.dismiss()
        }

        override fun isShowing(): Boolean = this@asHandle.isShow
    }
}

private val TEXT_PRIMARY = Color.rgb(23, 32, 51)
private val TEXT_SECONDARY = Color.rgb(102, 112, 133)
private val PRIMARY = Color.rgb(31, 111, 235)
private val DANGER = Color.rgb(220, 38, 38)
private val DIVIDER = Color.rgb(228, 231, 236)
