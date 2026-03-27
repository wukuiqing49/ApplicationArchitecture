package com.wkq.test


import android.os.Bundle
import android.widget.Toast
import com.wkq.base.activity.BaseActivity
import com.wkq.router.annotation.Route
import com.wkq.router.api.Router
import com.wkq.test.databinding.ActivityTestBinding
import com.wkq.test.router.ITestService
import com.wkq.test.router.ITestUtil

/**
 * 测试主入口页面
 */
@Route(path = "/test/main")
class TestActivity : BaseActivity<ActivityTestBinding>() {

    override fun initView() {

        // 🚀 进入路由全场景演示页面
        binding.btnRouterOverall.setOnClickListener {
            Router.open("/test/router_overall", this)
        }

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
                putExtra("title", "WebView Demo")
            }
        }

        // 打开动态 BaseUrl 演示页面 (测试 KSP 路由自动化 + 注解动画 + Service 自动注册)
        binding.btnNetDynamic.setOnClickListener {
            // 1. 测试 Service 自动注册
            val testService = Router.getService(ITestService::class)
            val helloMsg = testService?.sayHello("Antigravity") ?: "Service not found"
            Toast.makeText(this, helloMsg, Toast.LENGTH_SHORT).show()

            // 2. 测试带动画的跳转 (动画已在 NetDynamicBaseUrlActivity 的 @Route 中定义)
            Router.open("/test/net_demo", this)
        }

        // 打开相机控制测试页面
        binding.btnCameraTest.setOnClickListener {
            Router.open("/test/camera_control", this)
        }

        // 打开 URL 智能跳转测试页面
        binding.btnUrlResolve.setOnClickListener {
            Router.open("/test/smart_jump", this)
        }

        // 3. 高级路由测试 (Postcard + Result + Interceptor)
        binding.btnRouterAdvanced.setOnClickListener {
            Router.build("/test/target")
                .withString("input", "Hello from Postcard!")
                .withTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                .navigation(this) { result ->
                    val data = result.data?.getStringExtra("result") ?: "No Result"
                    Toast.makeText(this, "收到返回结果: $data", Toast.LENGTH_LONG).show()
                }
        }

        // 4. 获取 Fragment 演示
        binding.btnRouterFragment.setOnClickListener {
            val bundle = Bundle().apply { putString("info", "来自 TestActivity 的参数") }
            val fragment = Router.getFragment("/test/fragment", bundle)
            if (fragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(binding.container.id, fragment)
                    .commit()
            } else {
                Toast.makeText(this, "Fragment not found", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. 获取 View 演示
        binding.btnRouterView.setOnClickListener {
            val customView = Router.getView("/test/view", this)
            if (customView != null) {
                binding.container.removeAllViews()
                binding.container.addView(customView)
            } else {
                Toast.makeText(this, "View not found", Toast.LENGTH_SHORT).show()
            }
        }

        // 6. 获取 Util 演示
        binding.btnRouterUtil.setOnClickListener {
            val util = Router.getService(ITestUtil::class)
            val result = util?.doSomething("hello router") ?: "Util not found"
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
        }
    }

    override fun initData() {
    }
}