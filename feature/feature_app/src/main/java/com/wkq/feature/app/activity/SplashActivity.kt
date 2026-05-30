package com.wkq.feature.app.activity

import androidx.lifecycle.lifecycleScope
import com.wkq.base.activity.BaseFullScreenActivity

import com.wkq.router.api.Router
import com.wkq.feature.app.databinding.ActivitySplashBinding
import com.wkq.user.manager.UserManager
import com.wkq.util.showToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseFullScreenActivity<ActivitySplashBinding>() {

    override fun initView() {
//        UserManager.getInstance().getUserAsync {
//            showToast("当前用户信息: ${it?.userName}")
//        }
      lifecycleScope.launch {
          val user=UserManager.getInstance().getSafeCurrentUser()
          showToast("当前用户信息: ${user?.userName}")
      }

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
