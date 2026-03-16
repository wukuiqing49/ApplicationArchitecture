package com.wkq.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wkq.test.databinding.ActivityParticleLoadingDemoBinding
import com.wkq.ui.util.PopupUtil

/**
 * 粒子加载动画演示页面
 */
class ParticleLoadingDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParticleLoadingDemoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParticleLoadingDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 展示自定义粒子加载弹窗
        binding.btnShowPopup.setOnClickListener {
            PopupUtil.showParticleLoading(this, "Fetching Data...")

        }
    }
}
