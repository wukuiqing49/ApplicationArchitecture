package com.wkq.test

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wkq.test.databinding.ActivityWebviewTestBinding

/**
 * WebView 测试页面
 * 用于验证 CommonWebView 的自定义进度条效果
 */
class WebViewTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebviewTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
    }

    private fun initView() {
        // 1. 初始化进度条默认颜色
        binding.commonWebview.setProgressBarColors(
            startColor = Color.parseColor("#FF4081"),
            endColor = Color.parseColor("#3F51B5"),
            bgColor = Color.parseColor("#F0F0F0")
        )

        // 2. 加载测试网页
        binding.commonWebview.loadUrl("https://www.baidu.com")

        // 3. 样式切换测试
        binding.btnStyle1.setOnClickListener {
            binding.commonWebview.setProgressBarColors(
                startColor = Color.parseColor("#FF0000"), // 纯红
                endColor = Color.parseColor("#0000FF"),   // 纯蓝
                bgColor = Color.parseColor("#E0E0E0")
            )
            binding.commonWebview.reload()
        }

        binding.btnStyle2.setOnClickListener {
            binding.commonWebview.setProgressBarColors(
                startColor = Color.parseColor("#00FF00"), // 纯绿
                endColor = Color.parseColor("#FFFF00"),   // 纯黄
                bgColor = Color.parseColor("#000000")      // 黑色背景
            )
            binding.commonWebview.reload()
        }

        binding.btnReload.setOnClickListener {
            binding.commonWebview.reload()
        }
    }
}
