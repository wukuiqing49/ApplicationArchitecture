package com.wkq.common.web.util

import android.content.Context
import android.content.Intent
import com.wkq.common.web.WebProcessService

/**
 * WebView 预加载器
 * 用于提前启动 :web 进程，缩短 H5 页面的首次打开时间
 */
object WebPreLoader {

    /**
     * 预热进程
     * 建议在 Application.onCreate 或用户进入包含 H5 入口的模块时调用
     */
    fun preWarm(context: Context) {
        try {
            val intent = Intent(context, WebProcessService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            // Android 8.0+ 后台启动 Service 限制处理 (可选：如果是在后台则跳过)
        }
    }
}
