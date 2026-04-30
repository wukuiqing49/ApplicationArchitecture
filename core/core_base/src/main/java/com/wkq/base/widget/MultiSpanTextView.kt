package com.wkq.base.widget

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView

/**
 * A TextView that supports multiple clickable color spans.
 *
 * Example:
 * multiSpanTextView.setTextWithSpans(
 *     "我已阅读《用户协议》和《隐私政策》",
 *     MultiSpanTextView.SpanItem("《用户协议》", Color.BLUE) { ... },
 *     MultiSpanTextView.SpanItem("《隐私政策》", Color.BLUE) { ... }
 * )
 */
class MultiSpanTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    data class SpanItem(
        val keyword: String,
        @ColorInt val color: Int? = null,
        val clickAction: (() -> Unit)? = null,
        val underlineText: Boolean = false
    )

    init {
        // ClickableSpan needs movementMethod to receive taps.
        movementMethod = LinkMovementMethod.getInstance()
        highlightColor = Color.TRANSPARENT
        isClickable = true
        isLongClickable = false
    }

    /**
     * Set the full text and apply multiple spans.
     */
    fun setTextWithSpans(text: CharSequence?, vararg items: SpanItem) {
        if (text.isNullOrEmpty()) {
            setText(text)
            return
        }
        setTextWithSpans(text, items.toList())
    }

    /**
     * Set the full text and apply multiple spans.
     */
    fun setTextWithSpans(text: CharSequence, items: List<SpanItem>) {
        if (items.isEmpty()) {
            setText(text)
            return
        }

        val spannable = SpannableStringBuilder(text)
        val ranges = buildSpanRanges(text, items)

        ranges.forEach { range ->
            range.item.color?.let {
                spannable.setSpan(
                    ForegroundColorSpan(it),
                    range.start,
                    range.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            spannable.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: android.view.View) {
                        range.item.clickAction?.invoke()
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        // Keep the configured color instead of TextView's default link color.
                        range.item.color?.let { ds.color = it }
                        ds.isUnderlineText = range.item.underlineText
                    }
                },
                range.start,
                range.end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        setText(spannable)
    }

    /**
     * Clear span styling and restore plain text.
     */
    fun setPlainText(text: CharSequence?) {
        setText(text)
    }

    private data class SpanRange(
        val start: Int,
        val end: Int,
        val item: SpanItem
    )

    private fun buildSpanRanges(text: CharSequence, items: List<SpanItem>): List<SpanRange> {
        val candidates = mutableListOf<SpanRange>()

        items.forEach { item ->
            if (item.keyword.isEmpty()) return@forEach

            var fromIndex = 0
            while (fromIndex < text.length) {
                val start = text.indexOf(item.keyword, fromIndex)
                if (start < 0) break
                candidates += SpanRange(start, start + item.keyword.length, item)
                fromIndex = start + item.keyword.length
            }
        }

        if (candidates.isEmpty()) return emptyList()

        val selected = mutableListOf<SpanRange>()
        val occupied = BooleanArray(text.length)

        candidates
            .sortedWith(compareBy<SpanRange> { it.start }.thenByDescending { it.end - it.start })
            .forEach { candidate ->
                val overlaps = (candidate.start until candidate.end).any { occupied[it] }
                if (!overlaps) {
                    for (i in candidate.start until candidate.end) {
                        occupied[i] = true
                    }
                    selected += candidate
                }
            }

        return selected
    }
}
