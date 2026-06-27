package com.wkq.base.dialog

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.FrameLayout
import com.lxj.xpopup.core.CenterPopupView
import com.wkq.base.databinding.DialogCommonPopBinding

internal class PermissionsPopupView(
    private val context: Context,
    private val title: String,
    private val desc: String,
    private val sureText: String? = "",
    private val listener: CommonPopupListener? = null
) : CenterPopupView(context) {

    private val binding = DialogCommonPopBinding.inflate(LayoutInflater.from(context))

    override fun addInnerContent() {
        val popupWidth = (getScreenWidth() * 650f / 960f).toInt()
        val params = FrameLayout.LayoutParams(popupWidth, FrameLayout.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER
        centerPopupContainer.addView(binding.root, params)
    }

    private fun getScreenWidth(): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager? ?: return -1
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.currentWindowMetrics.bounds.width()
        } else {
            val point = Point()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealSize(point)
            point.x
        }
    }

    override fun onCreate() {
        super.onCreate()
        initView()
    }

    private fun initView() {
        binding.tvTitle.text = title
        binding.tvContent.text = desc

        if (!TextUtils.isEmpty(sureText)) {
            binding.tvRight.text = sureText
        }
        binding.tvRight.setOnClickListener {
            dismiss()
            listener?.sureClick()
        }
        binding.tvLeft.setOnClickListener {
            listener?.cancelClick()
            dismiss()
        }
    }
}
