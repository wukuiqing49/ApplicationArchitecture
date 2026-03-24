package com.wkq.test.router

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.TextView
import com.wkq.router.annotation.Route

@Route(path = "/test/view")
class TestCustomView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {

    init {
        text = "我是由路由加载的 Custom View"
        textSize = 16f
        setTextColor(0xFFFF4081.toInt())
        gravity = Gravity.CENTER
        setPadding(20, 20, 20, 20)
        setBackgroundColor(0xFFFCE4EC.toInt())
    }
}
