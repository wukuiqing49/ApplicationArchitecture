package com.wkq.util.jump.impl

import com.wkq.util.jump.BaseJumpHandler
import com.wkq.util.log.ALog
import java.net.URLDecoder

/**
 * 嵌入式 Scheme 处理器 (优先级最高)
 * 直接从 URL 参数中提取 scheme
 */
class EmbeddedSchemeHandler : BaseJumpHandler() {

    override fun getPriority(): Int = 100

    override fun canHandle(url: String): Boolean {
        val scheme = getParam(url, "scheme")
        return scheme != null && (scheme.startsWith("snssdk") || scheme.startsWith("taobao") || scheme.startsWith("openapp"))
    }

    override fun convertToScheme(url: String): String? {
        val scheme = getParam(url, "scheme")
        return try {
            if (scheme != null) URLDecoder.decode(scheme, "UTF-8") else null
        } catch (e: Exception) {
            ALog.e("EmbeddedSchemeHandler", "Decode failed: ${e.message}")
            null
        }
    }
}
