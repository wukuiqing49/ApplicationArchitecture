package com.wkq.test

import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wkq.router.annotation.Route
import com.wkq.util.jump.SmartJumpUtils
import com.wkq.util.jump.UrlUtils
import kotlinx.coroutines.launch

@Route(path = "/test/smart_jump")
class SmartJumpTestActivity : AppCompatActivity() {

    private lateinit var etUrl: EditText
    private lateinit var btnResolve: Button
    private lateinit var llResults: LinearLayout

    private val testUrls = listOf(
     "https://3.cn/-2IyKiJ0?jkl@U3ix8FNKRRS@",
        "https://detail.tmall.com/item.htm?ali_refid=a3_420860_1007%3A6429089069%3AH%3A3387046312_0_24473476109%3Acb6b9b3afa19cb90a4d579722c8c4387&amp;ali_trackid=296_cb6b9b3afa19cb90a4d579722c8c4387&amp;id=1033824621892&amp;item_type=ad&amp;mi_id=0000HOdAEobkOQ2KEFxqFMgcUG8KEflRw1XXjfjBNSoSMWI&amp;mm_sceneid=2_0_6429089069_0&amp;spm=tbpc.pc_sem_alimama%2Fa.201876.d1"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smart_jump_test)

        etUrl = findViewById(R.id.et_url)
        btnResolve = findViewById(R.id.btn_resolve)
        llResults = findViewById(R.id.ll_results)

        btnResolve.setOnClickListener {
            val url = etUrl.text.toString()
            if (url.isNotEmpty()) {
                resolveAndShow(url)
            }
        }

        // 添加初始化示例
        testUrls.forEach { url ->
            addTestItem(url)
        }
    }

    private fun addTestItem(url: String) {
        val btn = Button(this).apply {
            text = "测试: $url"
            isAllCaps = false
            setOnClickListener {
                etUrl.setText(url)
                resolveAndShow(url)
            }
        }
        llResults.addView(btn)
    }

    private fun resolveAndShow(url: String) {
        lifecycleScope.launch {
            val statusView = TextView(this@SmartJumpTestActivity).apply {
                text = "正在解析: $url ..."
                setPadding(0, 8, 0, 8)
            }
            statusView.setLines(1)
            statusView.ellipsize = TextUtils.TruncateAt.MIDDLE
            llResults.addView(statusView, 0)

            val finalUrl = UrlUtils.getFinalUrl(url)
            val isSame = url == finalUrl
            
            statusView.text = """
                原始 URL: $url
            
                是否一致: $isSame
            """.trimIndent()

            // 添加智能跳转按钮
            val btnJump = Button(this@SmartJumpTestActivity).apply {
                text = "智能跳转 (Smart Jump)"
                setOnClickListener {
                    lifecycleScope.launch {
                        SmartJumpUtils.jumpToAppOrBrowser(this@SmartJumpTestActivity, url)
                    }
                }
            }
            llResults.addView(btnJump, 1) // 在状态文字下方插入按钮
            
            val divider = TextView(this@SmartJumpTestActivity).apply {
                text = "----------------------------"
            }
            llResults.addView(divider, 2)
        }
    }
}
