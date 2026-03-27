package com.wkq.util.jump.impl

import com.wkq.util.jump.BaseJumpHandler

/**
 * 拼多多处理器
 */
class PDDJumpHandler : BaseJumpHandler() {

    override fun canHandle(url: String): Boolean {
        val lowUrl = url.lowercase()
        return lowUrl.contains("yangkeduo.com") || lowUrl.contains("pinduoduo.com")
    }

    override fun convertToScheme(url: String): String? {
        val realUrl = cleanUrl(url)
        val goodsId = getParam(realUrl, "goods_id")
        return if (goodsId != null) {
            "pinduoduo://com.xunmeng.pinduoduo/goods_detail.html?goods_id=$goodsId"
        } else {
            "pinduoduo://com.xunmeng.pinduoduo/"
        }
    }
}
