package com.wkq.util.jump

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.wkq.util.log.ALog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 智能跳转工具类 (工程级重构版本)
 * 实现商品 URL 还原、多平台异步识别及安全跳转
 */
object SmartJumpUtils {

    private const val TAG = "SmartJumpUtils"

    /**
     * 智能跳转核心入口
     * 还原 URL -> 策略引擎转换 Scheme -> 尝试唤起 App -> 失败回退浏览器
     * 
     * @param context Context
     * @param url 原始商品 URL
     */
    suspend fun jumpToAppOrBrowser(context: Context, url: String) = withContext(Dispatchers.Main) {
        ALog.d(TAG, "开始智能跳转处理: $url")
        
        // 1. 处理重定向获取最终 URL (IO 线程)
        val finalUrl = UrlUtils.getFinalUrl(url)
        ALog.d(TAG, "解析后的最终 URL: $finalUrl")
        
        // 2. 使用策略引擎获取目标 Scheme
        val schemeUrl = SmartJumpEngine.getScheme(finalUrl)
        
        if (schemeUrl != null) {
            try {
                ALog.d(TAG, "尝试通过策略 Scheme 跳转: $schemeUrl")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(schemeUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return@withContext 
            } catch (e: Exception) {
                ALog.w(TAG, "策略 Scheme 跳转失败: ${e.message}")
            }
        }
        
        // 3. 兜底方案：使用系统浏览器
        ALog.d(TAG, "执行浏览器兜底跳转: $finalUrl")
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            ALog.e(TAG, "兜底跳转失败: ${e.message}")
        }
    }
}
