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

        // 打开 PhotoPicker 演示页面
        binding.btnPhotoPicker.setOnClickListener {
            Router.open("/test/photo_picker", this)
        }

        // 打开粒子加载演示页面
        binding.btnParticleLoading.setOnClickListener {
            Router.open("/test/particle_loading", this)
        }

        // 打开协议与账号管理演示页面
        binding.btnProtocolDemo.setOnClickListener {
            Router.open("/test/protocol_demo", this)
        }

        // 打开原生 WebView 演示页面
        binding.btnWebview.setOnClickListener {

            Router.open("/common/webview", this){
                putExtra("url", "https://www.w3schools.com/tags/tryit.asp?filename=tryhtml5_input_type_file")
                putExtra("title", "")
            }

        }

        // 打开动态 BaseUrl 演示页面
        binding.btnNetDynamic.setOnClickListener {
            startActivity(android.content.Intent(this, NetDynamicBaseUrlActivity::class.java))
        }
    }

    override fun initData() {
    }
}