package com.wkq.test.router

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.wkq.base.activity.BaseActivity
import com.wkq.net.config.NetConfig
import com.wkq.net.core.ApiRetrofit
import com.wkq.net.core.NetManager
import com.wkq.net.core.awaitRawResult
import com.wkq.net.core.onError
import com.wkq.net.core.onSuccess
import com.wkq.net.interceptor.HeaderInterceptor
import com.wkq.test.databinding.ActivityNetDemoBinding
import com.wkq.util.showToast


import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.wkq.router.annotation.Route
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers

/**
 * 动态 BaseUrl 切换演示页面
 */
@Route(
    path = "/test/net_demo",
    enterAnim = "slide_in_right",
    exitAnim = "slide_out_left"
)
class NetDynamicBaseUrlActivity : BaseActivity<ActivityNetDemoBinding>() {

    /**
     * 定义演示接口
     */
    interface DemoService {
        // 默认地址请求
        @GET("api/music/list?page=1")
        fun getDefault(): Call<ResponseBody>

        // 切换到 Google
        @Headers("${HeaderInterceptor.HEADER_BASE_URL_KEY}: google")
        @GET("/")
        fun getGoogle(): Call<ResponseBody>

        // 切换到 GitHub
        @Headers("${HeaderInterceptor.HEADER_BASE_URL_KEY}: github")
        @GET("users/octocat")
        fun getGithubUser(): Call<ResponseBody>

        // 演示返回 Any (自动解析 JSON 为 Map/List)
        @Headers("${HeaderInterceptor.HEADER_BASE_URL_KEY}: github")
        @GET("users/octocat")
        fun getGithubUserAny(): Call<Any>
    }

    private val demoService by lazy { ApiRetrofit.create(DemoService::class.java) }

    override fun initView() {
        binding.btnRequestDefault.setOnClickListener {
            performRequest(demoService.getDefault(), "默认地址")
        }

        binding.btnRequestGoogle.setOnClickListener {
            performRequest(demoService.getGoogle(), "Google")
        }

        binding.btnRequestGithub.setOnClickListener {
            performRequest(demoService.getGithubUser(), "GitHub (ResponseBody)")
        }

        binding.btnRequestRaw.setOnClickListener {
            performRawRequest()
        }
    }

    private fun performRawRequest() {
        binding.tvResult.text = "正在请求 GitHub (Any) ..."
        
        // 使用协程演示 awaitRawResult()
        lifecycleScope.launch {
            demoService.getGithubUserAny().awaitRawResult()
                .onSuccess { data ->
                    binding.tvResult.text = """
                        [请求成功]
                        模式: Raw API (awaitRawResult)
                        返回类型: ${data?.javaClass?.simpleName}
                        内容: $data
                    """.trimIndent()
                }
                .onError { code, message ->
                    binding.tvResult.text = "[请求失败]\n错误码: $code\n消息: $message"
                }
        }
    }

    private fun performRequest(call: Call<ResponseBody>, label: String) {
        binding.tvResult.text = "正在请求 $label ..."
        
        lifecycleScope.launch {
            call.awaitRawResult()
                .onSuccess { body ->
                    // ResponseBody.string() 是耗时操作，且只能调用一次，切到 IO 线程处理
                    val content = withContext(Dispatchers.IO) {
                        body?.use { it.string() } ?: "Empty body"
                    }
                    val url = call.request().url.toString()
                    
                    binding.tvResult.text = """
                        [请求成功]
                        标签: $label
                        实际请求 URL: $url
                        内容预览 (前 500 字符):
                        ${content.take(500)}
                    """.trimIndent()
                }
                .onError { code, message ->
                    binding.tvResult.text = """
                        [请求失败]
                        标签: $label
                        实际请求 URL: ${call.request().url}
                        错误码: $code
                        消息: $message
                    """.trimIndent()
                }
        }
    }

    override fun initData() {
        // 确保 NetManager 已针对此 Demo 初始化（如果 Application 中没初始化的话）
        try {
            NetManager.getConfig()
        } catch (e: Exception) {
            val config = NetConfig.Builder()
                .setBaseUrl("https://api.example.com/")
                .putBaseUrl("google", "https://www.google.com/")
                .putBaseUrl("github", "https://api.github.com/")
                .setDebugLogsEnabled(true)
                .build()
            NetManager.init(config)
        }
    }
}
