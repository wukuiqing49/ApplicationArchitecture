package com.wkq.test.router

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.wkq.router.annotation.Route

@Route(path = "/test/fragment")
class TestFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xFFE0E0E0.toInt())
        }
        
        val tv = TextView(requireContext()).apply {
            text = "我是由路由加载的 Fragment\n参数: ${arguments?.getString("info") ?: "无"}"
            textSize = 18f
            setTextColor(0xFF333333.toInt())
        }
        
        root.addView(tv)
        return root
    }
}
