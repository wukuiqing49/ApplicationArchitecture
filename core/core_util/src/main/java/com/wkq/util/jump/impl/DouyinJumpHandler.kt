package com.wkq.util.jump.impl

import com.wkq.util.jump.BaseJumpHandler

/**
 * 抖音 / 今日特卖处理器
 */
class DouyinJumpHandler : BaseJumpHandler() {

    override fun canHandle(url: String): Boolean {
        val lowUrl = url.lowercase()
        return lowUrl.contains("douyin.com") || lowUrl.contains("iesdouyin.com") || lowUrl.contains("jinritemai.com")
    }

    override fun convertToScheme(url: String): String? {
        val realUrl = cleanUrl(url)
        val lowUrl = realUrl.lowercase()
        val promotionId = getParam(realUrl, "promotion_id") ?: getParam(realUrl, "id")
        return if (promotionId != null && lowUrl.contains("detail")) {
            "snssdk1128://ec_goods_detail?promotion_id=$promotionId"
        } else {
            "snssdk1128://feed"
        }
    }
}
