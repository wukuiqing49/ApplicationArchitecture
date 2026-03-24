package com.wkq.test.router

import android.app.Activity
import android.content.Intent
import com.wkq.base.activity.BaseActivity
import com.wkq.router.annotation.Route
import com.wkq.test.databinding.ActivityTargetBinding

@Route(path = "/test/target")
class TargetActivity : BaseActivity<ActivityTargetBinding>() {

    override fun initView() {
        val input = intent.getStringExtra("input") ?: "No input"
        binding.tvInput.text = "收到参数: $input"

        binding.btnReturn.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("result", "Hello from Target!")
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun initData() {
    }
}
