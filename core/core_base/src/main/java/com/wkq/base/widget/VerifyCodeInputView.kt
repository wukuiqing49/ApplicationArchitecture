package com.wkq.base.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.View.BaseSavedState
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import com.wkq.base.R

/**
 * 验证码输入控件。
 *
 * 设计目标：
 * - 用“隐藏输入框”负责真实输入，规避多输入框方案在软键盘、退格、粘贴上的不稳定问题
 * - 用“可视化验证码格子”负责显示状态和样式
 * - 支持自动输入、自动聚焦、粘贴分发、退格、状态恢复
 */
class VerifyCodeInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private enum class CellState {
        NORMAL, FOCUSED, FILLED
    }

    private data class BoxHolder(
        val container: FrameLayout,
        val label: AppCompatTextView
    )

    private var codeLength = 6
    private var cellWidthPx = dp2px(44f)
    private var cellHeightPx = dp2px(52f)
    private var cellSpacingPx = dp2px(10f)
    private var cornerRadiusPx = dp2px(10f).toFloat()
    private var borderWidthPx = dp2px(1f)

    @ColorInt
    private var textColorInt = Color.WHITE

    private var textSizePx = sp2px(18f)

    @ColorInt
    private var normalBorderColor = Color.parseColor("#2C3A57")

    @ColorInt
    private var focusedBorderColor = Color.parseColor("#6F97FF")

    @ColorInt
    private var filledBorderColor = Color.parseColor("#3E547D")

    @ColorInt
    private var normalBgColor = Color.parseColor("#1E2A44")

    @ColorInt
    private var focusedBgColor = Color.parseColor("#26385C")

    @ColorInt
    private var filledBgColor = Color.parseColor("#23324D")

    private var numericOnly = true

    private val boxContainer = LinearLayout(context)
    private val hiddenEditor = AppCompatEditText(context)
    private val boxes = mutableListOf<BoxHolder>()

    private var currentCode = ""
    private var currentSelection = 0
    private var internalChange = false
    private var lastCompleteCode: String? = null

    /** 输入变化回调：code 为当前内容，complete 表示是否已填满 */
    var onCodeChangedListener: ((code: String, complete: Boolean) -> Unit)? = null

    /** 输入完成回调：当内容首次达到指定长度时触发 */
    var onCodeCompleteListener: ((code: String) -> Unit)? = null

    init {
        importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO
        isSaveEnabled = true
        descendantFocusability = FOCUS_AFTER_DESCENDANTS

        context.withStyledAttributes(attrs, R.styleable.VerifyCodeInputView) {
            codeLength = getInt(R.styleable.VerifyCodeInputView_verifyCode_length, codeLength).coerceAtLeast(1)
            cellWidthPx = getDimensionPixelSize(R.styleable.VerifyCodeInputView_verifyCode_cellWidth, cellWidthPx)
            cellHeightPx = getDimensionPixelSize(R.styleable.VerifyCodeInputView_verifyCode_cellHeight, cellHeightPx)
            cellSpacingPx = getDimensionPixelSize(R.styleable.VerifyCodeInputView_verifyCode_cellSpacing, cellSpacingPx)
            cornerRadiusPx = getDimension(R.styleable.VerifyCodeInputView_verifyCode_cornerRadius, cornerRadiusPx)
            borderWidthPx = getDimensionPixelSize(R.styleable.VerifyCodeInputView_verifyCode_borderWidth, borderWidthPx)
            textColorInt = getColor(R.styleable.VerifyCodeInputView_verifyCode_textColor, textColorInt)
            textSizePx = getDimensionPixelSize(R.styleable.VerifyCodeInputView_verifyCode_textSize, textSizePx)
            normalBorderColor = getColor(R.styleable.VerifyCodeInputView_verifyCode_normalBorderColor, normalBorderColor)
            focusedBorderColor = getColor(R.styleable.VerifyCodeInputView_verifyCode_focusedBorderColor, focusedBorderColor)
            filledBorderColor = getColor(R.styleable.VerifyCodeInputView_verifyCode_filledBorderColor, filledBorderColor)
            normalBgColor = getColor(R.styleable.VerifyCodeInputView_verifyCode_normalBgColor, normalBgColor)
            focusedBgColor = getColor(R.styleable.VerifyCodeInputView_verifyCode_focusedBgColor, focusedBgColor)
            filledBgColor = getColor(R.styleable.VerifyCodeInputView_verifyCode_filledBgColor, filledBgColor)
            numericOnly = getBoolean(R.styleable.VerifyCodeInputView_verifyCode_numericOnly, numericOnly)
        }

        initContainer()
        initHiddenEditor()
        buildBoxes()
        render()
    }

    private fun initContainer() {
        boxContainer.orientation = LinearLayout.HORIZONTAL
        boxContainer.gravity = Gravity.CENTER_VERTICAL
        boxContainer.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        addView(boxContainer)
    }

    private fun initHiddenEditor() {
        hiddenEditor.apply {
            layoutParams = LayoutParams(1, 1).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            alpha = 0f
            isCursorVisible = false
            isLongClickable = false
            setTextColor(Color.TRANSPARENT)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 0, 0, 0)
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_DONE
            isFocusable = true
            isFocusableInTouchMode = true
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO

            updateInputConfiguration()

            filters = arrayOf(InputFilter.LengthFilter(codeLength))

            setOnClickListener {
                requestInputFocus()
            }

            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    render()
                }
            }

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable?) {
                    if (internalChange) return
                    handleEditorChanged(s?.toString().orEmpty())
                }
            })

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE && isComplete()) {
                    onCodeCompleteListener?.invoke(currentCode)
                    true
                } else {
                    false
                }
            }
        }

        addView(hiddenEditor)
    }

    private fun buildBoxes() {
        boxContainer.removeAllViews()
        boxes.clear()

        repeat(codeLength) { index ->
            val holder = createBox(index)
            boxes.add(holder)

            val lp = LinearLayout.LayoutParams(cellWidthPx, cellHeightPx).apply {
                if (index > 0) {
                    leftMargin = cellSpacingPx / 2
                }
                if (index < codeLength - 1) {
                    rightMargin = cellSpacingPx / 2
                }
            }
            boxContainer.addView(holder.container, lp)
        }
    }

    private fun createBox(index: Int): BoxHolder {
        val label = AppCompatTextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            setTextColor(textColorInt)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx.toFloat())
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            includeFontPadding = false
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(cellWidthPx, cellHeightPx)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                focusPosition(index)
            }
            addView(label)
        }

        return BoxHolder(container, label)
    }

    private fun handleEditorChanged(rawText: String) {
        val normalized = normalizeInput(rawText).take(codeLength)
        val selection = hiddenEditor.selectionStart.coerceAtLeast(0)

        if (normalized != rawText) {
            replaceEditorText(normalized, normalized.length)
            return
        }

        currentCode = normalized
        currentSelection = selection.coerceIn(0, currentCode.length)
        render()
        dispatchCodeState()
    }

    private fun replaceEditorText(text: String, selection: Int) {
        internalChange = true
        hiddenEditor.setText(text)
        hiddenEditor.setSelection(selection.coerceIn(0, text.length))
        internalChange = false

        currentCode = text
        currentSelection = selection.coerceIn(0, currentCode.length)
        render()
        dispatchCodeState()
    }

    private fun normalizeInput(text: String): String {
        return if (numericOnly) {
            text.filter { it.isDigit() }
        } else {
            text.filter { !it.isWhitespace() }
        }
    }

    private fun render() {
        val focusedIndex = resolveFocusedIndex()

        boxes.forEachIndexed { index, holder ->
            val charText = currentCode.getOrNull(index)?.toString().orEmpty()
            holder.label.text = charText

            val state = when {
                index < currentCode.length -> {
                    if (index == focusedIndex && hasActiveFocus()) CellState.FOCUSED else CellState.FILLED
                }
                index == focusedIndex && hasActiveFocus() -> CellState.FOCUSED
                else -> CellState.NORMAL
            }

            holder.container.background = createCellBackground(state)
            holder.label.setTextColor(textColorInt)
            holder.label.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx.toFloat())
        }
    }

    private fun resolveFocusedIndex(): Int {
        if (!hasActiveFocus()) return -1
        if (currentSelection < 0) return -1
        if (currentSelection >= codeLength) {
            return if (currentCode.length < codeLength) currentCode.length else -1
        }
        return currentSelection
    }

    private fun hasActiveFocus(): Boolean {
        return hiddenEditor.hasFocus() || isFocused
    }

    private fun createCellBackground(state: CellState): GradientDrawable {
        val (borderColor, bgColor) = when (state) {
            CellState.NORMAL -> normalBorderColor to normalBgColor
            CellState.FOCUSED -> focusedBorderColor to focusedBgColor
            CellState.FILLED -> filledBorderColor to filledBgColor
        }

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusPx
            setColor(bgColor)
            setStroke(borderWidthPx.coerceAtLeast(1), borderColor)
        }
    }

    private fun dispatchCodeState() {
        val complete = isComplete()
        onCodeChangedListener?.invoke(currentCode, complete)

        if (complete) {
            if (lastCompleteCode != currentCode) {
                lastCompleteCode = currentCode
                onCodeCompleteListener?.invoke(currentCode)
            }
        } else {
            lastCompleteCode = null
        }
    }

    private fun focusPosition(index: Int) {
        val target = index.coerceIn(0, codeLength)
        requestInputFocus()
        internalChange = true
        hiddenEditor.setSelection(target.coerceAtMost(hiddenEditor.text?.length ?: 0))
        internalChange = false
        currentSelection = hiddenEditor.selectionStart.coerceAtLeast(0)
        render()
    }

    /**
     * 请求输入焦点，并尽量弹出软键盘。
     */
    fun requestInputFocus() {
        hiddenEditor.requestFocus()
        hiddenEditor.post {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(hiddenEditor, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /**
     * 清除输入焦点，并尽量收起软键盘。
     */
    fun clearInputFocus() {
        hiddenEditor.clearFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(windowToken, 0)
        render()
    }

    fun getCode(): String {
        return currentCode
    }

    fun isComplete(): Boolean {
        return currentCode.length == codeLength
    }

    fun setCode(code: String?) {
        val sanitized = normalizeInput(code.orEmpty()).take(codeLength)
        val selection = sanitized.length
        replaceEditorText(sanitized, selection)
    }

    fun clearCode() {
        replaceEditorText("", 0)
        requestInputFocus()
    }

    fun setCodeLength(length: Int) {
        codeLength = length.coerceAtLeast(1)
        hiddenEditor.filters = arrayOf(InputFilter.LengthFilter(codeLength))
        buildBoxes()
        setCode(currentCode)
    }

    fun setCellSize(widthPx: Int, heightPx: Int) {
        cellWidthPx = widthPx
        cellHeightPx = heightPx
        rebuildBoxes()
    }

    fun setCellSpacing(spacingPx: Int) {
        cellSpacingPx = spacingPx
        rebuildBoxes()
    }

    fun setCornerRadius(radiusPx: Float) {
        cornerRadiusPx = radiusPx
        render()
    }

    fun setBorderWidth(widthPx: Int) {
        borderWidthPx = widthPx
        render()
    }

    fun setTextColorInt(@ColorInt color: Int) {
        textColorInt = color
        render()
    }

    fun setTextSizePx(sizePx: Int) {
        textSizePx = sizePx
        render()
    }

    fun setNormalBorderColor(@ColorInt color: Int) {
        normalBorderColor = color
        render()
    }

    fun setFocusedBorderColor(@ColorInt color: Int) {
        focusedBorderColor = color
        render()
    }

    fun setFilledBorderColor(@ColorInt color: Int) {
        filledBorderColor = color
        render()
    }

    fun setNormalBgColor(@ColorInt color: Int) {
        normalBgColor = color
        render()
    }

    fun setFocusedBgColor(@ColorInt color: Int) {
        focusedBgColor = color
        render()
    }

    fun setFilledBgColor(@ColorInt color: Int) {
        filledBgColor = color
        render()
    }

    fun setNumericOnly(enabled: Boolean) {
        numericOnly = enabled
        updateInputConfiguration()
    }

    private fun updateInputConfiguration() {
        hiddenEditor.inputType = if (numericOnly) {
            InputType.TYPE_CLASS_NUMBER
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        hiddenEditor.imeOptions = EditorInfo.IME_ACTION_DONE
    }

    private fun rebuildBoxes() {
        boxContainer.removeAllViews()
        boxes.clear()
        buildBoxes()
        render()
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return SavedState(superState).apply {
            code = currentCode
            selection = currentSelection
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        setCode(state.code)
        focusPosition(state.selection)
    }

    private class SavedState : BaseSavedState {
        var code: String = ""
        var selection: Int = 0

        constructor(superState: Parcelable?) : super(superState)

        constructor(parcel: Parcel) : super(parcel) {
            code = parcel.readString().orEmpty()
            selection = parcel.readInt()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(code)
            dest.writeInt(selection)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }

    private fun dp2px(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    private fun sp2px(sp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            resources.displayMetrics
        ).toInt()
    }
}
