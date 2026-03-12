package com.wkq.test

import com.wkq.base.activity.BaseActivity
import com.wkq.test.databinding.ActivityTestBinding
import com.wkq.core.router.Router

/**
 * 测试主入口页面
 */
class TestActivity : BaseActivity<ActivityTestBinding>() {

    override fun initView() {
        // 打开图片加载演示页面
        binding.btnImageLoader.setOnClickListener {
            Router.open("/test/loader_image", this)
        }

        // 打开渐变标签演示页面
        binding.btnGradientLabel.setOnClickListener {
            Router.open("/test/gradient_label", this)
        }

        // 打开 MagicIndicator 演示页面
        binding.btnMagicIndicator.setOnClickListener {
            Router.open("/test/magic_indicator", this)
        }
    }

    override fun initData() {
    }
}