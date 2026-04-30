package com.wkq.test

import android.widget.Toast
import com.wkq.base.activity.BaseActivity
import com.wkq.router.annotation.Route
import com.wkq.router.api.Router
import com.wkq.test.databinding.ActivityTestBinding
import com.wkq.test.router.ITestService

/**
 * 测试主入口页面
 */
@Route(path = "/test/main")
class TestActivity : BaseActivity<ActivityTestBinding>() {

    override fun initView() {
        binding.btnRouterOverall.setOnClickListener {
            Router.open("/test/router_overall", this)
        }

        binding.btnImageLoader.setOnClickListener {
            Router.open("/test/loader_image", this)
        }

        binding.btnGradientLabel.setOnClickListener {
            Router.open("/test/gradient_label", this)
        }

        binding.btnMultiSpanText.setOnClickListener {
            Router.open("/test/multi_span_text", this)
        }

        binding.btnMagicIndicator.setOnClickListener {
            Router.open("/test/magic_indicator", this)
        }

        binding.btnPhotoPicker.setOnClickListener {
            Router.open("/test/photo_picker", this)
        }

        binding.btnParticleLoading.setOnClickListener {
            Router.open("/test/particle_loading", this)
        }

        binding.btnProtocolDemo.setOnClickListener {
            Router.open("/test/protocol_demo", this)
        }

        binding.btnWebview.setOnClickListener {
            Router.open("/common/webview", this) {
                putExtra("url", "https://www.w3schools.com/tags/tryit.asp?filename=tryhtml5_input_type_file")
                putExtra("title", "WebView Demo")
            }
        }

        binding.btnNetDynamic.setOnClickListener {
            val testService = Router.getService(ITestService::class)
            val helloMsg = testService?.sayHello("Antigravity") ?: "Service not found"
            Toast.makeText(this, helloMsg, Toast.LENGTH_SHORT).show()
            Router.open("/test/net_demo", this)
        }

        binding.btnUrlResolve.setOnClickListener {
            Router.open("/test/smart_jump", this)
        }

        binding.btnRouterAdvanced.setOnClickListener {
            Router.build("/test/target")
                .withString("input", "Hello from Postcard!")
                .withTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                .navigation(this) { result ->
                    val data = result.data?.getStringExtra("result") ?: "No Result"
                    Toast.makeText(this, "收到返回结果: $data", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun initData() = Unit
}
