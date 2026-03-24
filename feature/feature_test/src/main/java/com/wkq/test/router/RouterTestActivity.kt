package com.wkq.test.router

import android.os.Bundle
import android.widget.Toast
import com.wkq.base.activity.BaseActivity
import com.wkq.router.annotation.Route
import com.wkq.router.api.Router
import com.wkq.test.databinding.ActivityRouterTestBinding

import com.wkq.router.annotation.Param

/**
 * 路由全场景测试页面
 */
@Route(path = "/test/router_overall")
class RouterTestActivity : BaseActivity<ActivityRouterTestBinding>() {

    @Param(name = "test_key")
    var testValue: String = "默认值"

    @Param 
    var from: String = "未知来源"

    @Param
    var userId: Long = 0L

    @Param
    var tags: IntArray? = null

    override fun initView() {
        // --- Section 1: Navigation ---
        
        binding.btnNavBasic.setOnClickListener {
            Router.open("/test/loader_image", this)
        }

        binding.btnNavAnim.setOnClickListener {
            Router.open("/test/net_demo", this)
        }

        binding.btnNavInterceptor.setOnClickListener {
            // 这个跳转会被 LoginInterceptor 拦截并重定向到目标页
            Router.open("/test/target", this)
        }

        // --- Section 2: Data & Result ---

        binding.btnDataParams.setOnClickListener {
            Router.build("/test/target")
                .withString("input", "这是来自测试页面的参数")
                .navigation(this)
        }

        binding.btnDataResult.setOnClickListener {
            Router.build("/test/target")
                .withString("input", "请求返回结果")
                .navigation(this) { result ->
                    val data = result.data?.getStringExtra("result") ?: "无返回内容"
                    Toast.makeText(this, "收到结果: $data", Toast.LENGTH_LONG).show()
                }
        }

        // --- Section 3: UI Components ---

        binding.btnGetFragment.setOnClickListener {
            val bundle = Bundle().apply { putString("info", "Router Overall Test") }
            val fragment = Router.getFragment("/test/fragment", bundle)
            if (fragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(binding.previewContainer.id, fragment)
                    .commit()
            } else {
                Toast.makeText(this, "Fragment not found", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGetView.setOnClickListener {
            val customView = Router.getView("/test/view", this)
            if (customView != null) {
                binding.previewContainer.removeAllViews()
                binding.previewContainer.addView(customView)
            } else {
                Toast.makeText(this, "View not found", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Section 4: Services ---

        binding.btnServiceTest.setOnClickListener {
            val service = Router.getService(ITestService::class)
            val msg = service?.sayHello("Router User") ?: "Service 丢失"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        binding.btnUtilTest.setOnClickListener {
            val util = Router.getService(ITestUtil::class)
            val msg = util?.doSomething("router is powerful") ?: "Util 丢失"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun initData() {
        Router.inject(this)
        if (testValue != "默认值" || from != "未知来源" || userId != 0L) {
            val tagInfo = tags?.joinToString(",") ?: "null"
            Toast.makeText(this, "注入成功: testValue=$testValue, from=$from, userId=$userId, tags=$tagInfo", Toast.LENGTH_LONG).show()
        }
    }
}
