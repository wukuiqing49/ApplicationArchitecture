package com.wkq.test

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wkq.test.databinding.ActivityProtocolDemoBinding
import com.wkq.user.data.entity.UserEntity
import com.wkq.user.manager.UserManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 协议与账号管理核心功能演示页面
 */
class ProtocolDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProtocolDemoBinding
    private val userManager by lazy { UserManager.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProtocolDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        observeData()

    }

    private fun initView() {
        binding.btnLoginUser1.setOnClickListener {
            userManager.saveUser(UserEntity("001", "测试用户A", isCurrent = true))
        }

        binding.btnLoginUser2.setOnClickListener {
            userManager.saveUser(UserEntity("002", "测试用户B", isCurrent = true))
        }
        binding.btnSimulate401.setOnClickListener {
            simulateNetwork401()
        }
    }

    private fun observeData() {
        // 监听当前用户变化
        lifecycleScope.launch {
            userManager.currentUserFlow.collectLatest { user ->
                binding.tvCurrentUser.text = if (user != null) {
                    "当前用户: ${user.userName} (ID: ${user.userId})"
                } else {
                    "当前用户: 未登录"
                }
            }
        }


    }

    /**
     * 模拟网络请求触发 401
     * 注意：由于没有真实服务器，这里通过打印日志模拟
     */
    private fun simulateNetwork401() {
        Toast.makeText(this, "模拟发起请求，预期触发 401 拦截...", Toast.LENGTH_SHORT).show()
        
        // 实际上在 Application 中配置好了 GlobalNetHandler 以后，
        // 任何返回 401 的 Call.awaitResult() 都会触发拦截。
        // 这里手动调用拦截器逻辑以演示演示效果：
        lifecycleScope.launch {
             // 模拟拦截行为：假设后端返回了 401
             val code = 401
             println("Demo: 模拟捕获到 Code $code")
             
             // 检查拦截器是否生效 (逻辑上应该在 ApiRetrofit 内部触发，此处直接调用 UserManager 退出表示结果)
             if (code == 401) {

             }
        }
    }
}
