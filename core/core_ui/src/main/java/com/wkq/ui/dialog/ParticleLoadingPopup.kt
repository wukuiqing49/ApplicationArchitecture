package com.wkq.ui.dialog

import android.content.Context
import android.widget.TextView
import com.lxj.xpopup.core.CenterPopupView
import com.wkq.ui.R

/**
 * 粒子加载自定义弹窗
 */
class ParticleLoadingPopup(context: Context, private val msg: String) : CenterPopupView(context) {

    override fun getImplLayoutId(): Int = R.layout.view_particle_loading_popup

    override fun onCreate() {
        super.onCreate()

    }

    /**
     * 更新加载文字
     */

}
