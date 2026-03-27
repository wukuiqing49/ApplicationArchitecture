package com.wkq.util.jump.impl

import com.wkq.util.jump.BaseJumpHandler

/**
 * 京东处理器
 */
class JDJumpHandler : BaseJumpHandler() {

    override fun canHandle(url: String): Boolean {
        val lowUrl = url.lowercase()
        return lowUrl.contains("jd.com") || lowUrl.contains("jd.hk") || lowUrl.contains("3.cn")
    }

    override fun convertToScheme(url: String): String? {
        // 1. 通用清洗：提取真实 URL (解决风险验证页等问题)
        val realUrl = cleanUrl(url)

        // 2. 尝试提取 SKU ID (京东 App 需要 skuId 才能精准直达详情页)
        // 支持两种形式：https://item.m.jd.com/product/123.html 或 URL 参数中的 id/skuId
        var skuId = getParam(realUrl, "id") ?: getParam(realUrl, "skuId")
        if (skuId == null && (realUrl.contains("/product/") || realUrl.contains("/ware/view.action?wareId="))) {
            // 解析类似 /product/12345.html 的路径
            val pattern = "/product/(\\d+)".toRegex()
            skuId = pattern.find(realUrl)?.groupValues?.get(1)
        }

        // 3. 构造 params JSON
        val params = if (skuId != null) {
            // 如果有 skuId，使用 productDetail 直达
            "{\"category\":\"jump\",\"des\":\"productDetail\",\"skuId\":\"$skuId\",\"sourceType\":\"JSHOP_SOURCE_TYPE\",\"sourceValue\":\"JSHOP_SOURCE_VALUE\"}"
        } else {
            // 如果没有，使用 m 页面通用跳转
            "{\"category\":\"jump\",\"des\":\"m\",\"url\":\"$realUrl\"}"
        }
        
        return try {
            "openapp.jdmobile://virtual?params=${java.net.URLEncoder.encode(params, "UTF-8")}"
        } catch (e: Exception) {
            null
        }
    }
}
