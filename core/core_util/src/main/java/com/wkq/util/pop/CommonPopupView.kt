package com.wkq.util.pop

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.FrameLayout

import com.lxj.xpopup.core.CenterPopupView
import com.wkq.core.res.databinding.DialogCommonPopBinding


/**
 *
 *@Author: wkq
 *
 *@Time: 2025/4/18 14:13
 *
 *@Desc:
 */
internal class CommonPopupView(
   var  mContext: Context, var title: String, var desc: String, var sureText: String?="",
    var listener: CommonPopupListener?=null
) : CenterPopupView(mContext) {

    var binding = DialogCommonPopBinding.inflate(LayoutInflater.from(context))

    /*重写：布局*/
    override fun addInnerContent() {
        val popupWidth = (getScreenWidth() * 650f / 960f).toInt()

        val params = FrameLayout.LayoutParams(popupWidth, LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER

        centerPopupContainer.addView(binding.root, params)
    }

    fun getScreenWidth(): Int {
        val wm = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager? ?: return -1
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

        if (!TextUtils.isEmpty(sureText)){
            binding.tvRight.text = sureText
        }
        binding.tvRight.setOnClickListener {
            listener?.let {
                dismiss()
                it.sureClick()
            }
        }
        binding.tvLeft.setOnClickListener {

            listener?.let {
                it.cancelClick()
            }
            dismiss()
        }
    }

}
