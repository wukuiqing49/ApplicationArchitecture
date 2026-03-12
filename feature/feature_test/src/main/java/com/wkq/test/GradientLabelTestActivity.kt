package com.wkq.test

import com.wkq.base.activity.BaseActivity
import com.wkq.test.databinding.ActivityGradientLabelTestBinding

/**
 * GradientShapeLabelView 功能测试页面
 */
class GradientLabelTestActivity : BaseActivity<ActivityGradientLabelTestBinding>() {

    override fun initView() {
        // 这里可以通过代码动态修改
        binding.root.postDelayed({
            // 示例：动态修改文字和图标
            // binding.someViewId.setText("动态文字")
        }, 2000)
    }

    override fun initData() {
        // 初始化数据
    }
}
