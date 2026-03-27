package com.wkq.util.jump.impl

import com.wkq.util.jump.BaseJumpHandler

/**
 * 淘宝 & 天猫处理器
 */
class TaobaoJumpHandler : BaseJumpHandler() {

    override fun canHandle(url: String): Boolean {
        val lowUrl = url.lowercase()
        return lowUrl.contains("taobao.com") || lowUrl.contains("tmall.com") || lowUrl.contains("tb.cn")
    }

    override fun convertToScheme(url: String): String? {
        return url.replace("https://", "taobao://").replace("http://", "taobao://")
    }
}
