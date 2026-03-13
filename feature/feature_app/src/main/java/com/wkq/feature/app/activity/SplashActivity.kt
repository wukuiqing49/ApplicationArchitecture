package com.wkq.feature.app.activity

import androidx.lifecycle.lifecycleScope
import com.wkq.base.activity.BaseFullScreenActivity
import com.wkq.core.router.Router
import com.wkq.feature.app.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseFullScreenActivity<ActivitySplashBinding>() {

    override fun initView() {

        // 延迟 3 秒跳转到登录页
        lifecycleScope.launch {
            delay(3000)
            Router.open("/test/main", this@SplashActivity)
//            finish()
        }
    }

    override fun initData() {
        // 初始化数据
    }
}