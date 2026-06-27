package com.wkq.test

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.wkq.router.api.Router
import com.wkq.test.router.ITestService

object TestEntryRegistry {

    const val GROUP_ROUTER = "路由"
    const val GROUP_CORE = "Core 模块"
    const val GROUP_UI = "UI / Widget"
    const val GROUP_MEDIA = "媒体 / 系统"
    const val GROUP_WEB = "Web"
    const val GROUP_ACCOUNT = "账号 / 协议"

    fun createMainEntries(activity: FragmentActivity): List<TestEntry> {
        return listOf(
            TestEntry(
                group = GROUP_ROUTER,
                icon = "R",
                title = "路由全场景演示",
                desc = "覆盖 Activity、Fragment、服务、降级等路由能力"
            ) {
                Router.open("/test/router_overall", activity)
            },
            TestEntry(
                group = GROUP_ROUTER,
                icon = "A",
                title = "高级路由演示",
                desc = "测试 Result 回调、转场动画和拦截器"
            ) {
                Router.build("/test/target")
                    .withString("input", "Hello from Postcard!")
                    .withTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    .navigation(activity) { result ->
                        val data = result.data?.getStringExtra("result") ?: "No Result"
                        Toast.makeText(activity, "收到返回结果: $data", Toast.LENGTH_LONG).show()
                    }
            },
            TestEntry(
                group = GROUP_ROUTER,
                icon = "U",
                title = "URL 还原测试",
                desc = "测试 URL 解析、参数还原和跳转"
            ) {
                Router.open("/test/smart_jump", activity)
            },
            TestEntry(
                group = GROUP_CORE,
                icon = "C",
                title = "Core 模块 Demo",
                desc = "集中验证 core_base、core_util、PDF、上报、上传等模块"
            ) {
                Router.open("/test/core_modules", activity)
            },
            TestEntry(
                group = GROUP_CORE,
                icon = "N",
                title = "动态 BaseUrl 演示",
                desc = "测试网络模块动态域名和路由服务获取"
            ) {
                val testService = Router.getService(ITestService::class)
                val helloMsg = testService?.sayHello("ApplicationArchitecture") ?: "Service not found"
                Toast.makeText(activity, helloMsg, Toast.LENGTH_SHORT).show()
                Router.open("/test/net_demo", activity)
            },
            TestEntry(
                group = GROUP_UI,
                icon = "G",
                title = "渐变标签",
                desc = "GradientShapeLabelView 自定义样式测试"
            ) {
                Router.open("/test/gradient_label", activity)
            },
            TestEntry(
                group = GROUP_UI,
                icon = "S",
                title = "多段 Span 文本",
                desc = "MultiSpanTextView 高亮、点击和多段文本测试"
            ) {
                Router.open("/test/multi_span_text", activity)
            },
            TestEntry(
                group = GROUP_UI,
                icon = "M",
                title = "MagicIndicator",
                desc = "指示器和 ViewPager2 联动测试"
            ) {
                Router.open("/test/magic_indicator", activity)
            },
            TestEntry(
                group = GROUP_UI,
                icon = "L",
                title = "粒子加载",
                desc = "ParticleLoading 自定义加载动画测试"
            ) {
                Router.open("/test/particle_loading", activity)
            },
            TestEntry(
                group = GROUP_MEDIA,
                icon = "I",
                title = "图片加载",
                desc = "图片加载、缓存和展示能力测试"
            ) {
                Router.open("/test/loader_image", activity)
            },
            TestEntry(
                group = GROUP_MEDIA,
                icon = "P",
                title = "媒体选择",
                desc = "PhotoPicker 权限、选择和结果回调测试"
            ) {
                Router.open("/test/photo_picker", activity)
            },
            TestEntry(
                group = GROUP_WEB,
                icon = "W",
                title = "原生 Web 演示",
                desc = "CommonWebActivity 文件选择和基础 WebView 能力"
            ) {
                Router.open("/common/webview", activity) {
                    putExtra("url", "https://www.w3schools.com/tags/tryit.asp?filename=tryhtml5_input_type_file")
                    putExtra("title", "WebView Demo")
                }
            },
            TestEntry(
                group = GROUP_WEB,
                icon = "J",
                title = "JSBridge 测试",
                desc = "测试 JS 调用 Native 和 callback 流程"
            ) {
                Router.open("/common/webview", activity) {
                    putExtra(
                        "url",
                        "file:///android_asset/test.html" +
                            "?bridge=ThirdPlatformBridge" +
                            "&method=invoke" +
                            "&mode=api_params_callback"
                    )
                    putExtra("title", "JSBridge Test")
                    putExtra("open_js", true)
                }
            },
            TestEntry(
                group = GROUP_ACCOUNT,
                icon = "P",
                title = "协议及账号演示",
                desc = "用户协议、隐私政策和账号相关入口测试"
            ) {
                Router.open("/test/protocol_demo", activity)
            }
        )
    }
}
