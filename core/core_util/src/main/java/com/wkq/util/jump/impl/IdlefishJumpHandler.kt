package com.wkq.util.jump.impl

import com.wkq.util.jump.BaseJumpHandler

/**
 * 闲鱼处理器
 */
class IdlefishJumpHandler : BaseJumpHandler() {

    override fun canHandle(url: String): Boolean {
        val lowUrl = url.lowercase()
        return lowUrl.contains("goofish.com") || lowUrl.contains("21.cn") || lowUrl.contains("idlefish.com") || lowUrl.contains("2.taobao.com")
    }

    override fun convertToScheme(url: String): String? {
        val realUrl = cleanUrl(url)
        val itemId = getParam(realUrl, "id") ?: getParam(realUrl, "item_id")
        return if (itemId != null) {
            "fleamarket://item?id=$itemId"
        } else {
            "fleamarket://home"
        }
    }
}
