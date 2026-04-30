package com.wkq.test

import android.graphics.Color
import android.widget.Toast
import com.wkq.base.activity.BaseActivity
import com.wkq.base.widget.MultiSpanTextView
import com.wkq.router.annotation.Route
import com.wkq.test.databinding.ActivityMultiSpanTextViewTestBinding

/**
 * MultiSpanTextView 测试页
 */
@Route(path = "/test/multi_span_text")
class MultiSpanTextViewTestActivity : BaseActivity<ActivityMultiSpanTextViewTestBinding>() {

    override fun initView() {
        binding.multiSpanText.setTextWithSpans(
            "我已阅读《用户协议》和《隐私政策》，并同意《服务条款》",
            MultiSpanTextView.SpanItem(
                keyword = "《用户协议》",
                color = Color.parseColor("#1A73E8"),
                clickAction = {
                    Toast.makeText(this, "点击了 用户协议", Toast.LENGTH_SHORT).show()
                }
            ),
            MultiSpanTextView.SpanItem(
                keyword = "《隐私政策》",
                color = Color.parseColor("#D81B60"),
                clickAction = {
                    Toast.makeText(this, "点击了 隐私政策", Toast.LENGTH_SHORT).show()
                }
            ),
            MultiSpanTextView.SpanItem(
                keyword = "《服务条款》",
                color = Color.parseColor("#43A047"),
                clickAction = {
                    Toast.makeText(this, "点击了 服务条款", Toast.LENGTH_SHORT).show()
                },
                underlineText = true
            )
        )

        binding.btnReset.setOnClickListener {
            binding.multiSpanText.setTextWithSpans(
                "再次阅读《用户协议》和《隐私政策》",
                MultiSpanTextView.SpanItem(
                    keyword = "《用户协议》",
                    color = Color.parseColor("#1A73E8"),
                    clickAction = { Toast.makeText(this, "再次点击 用户协议", Toast.LENGTH_SHORT).show() }
                ),
                MultiSpanTextView.SpanItem(
                    keyword = "《隐私政策》",
                    color = Color.parseColor("#D81B60"),
                    clickAction = { Toast.makeText(this, "再次点击 隐私政策", Toast.LENGTH_SHORT).show() }
                )
            )
        }

        binding.btnPlain.setOnClickListener {
            binding.multiSpanText.setPlainText("这是普通文本，没有任何点击 span。")
        }
    }

    override fun initData() = Unit
}
