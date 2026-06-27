package com.wkq.site.report.model

/**
 * 报告位置数据。
 *
 * @param latitude 纬度，允许为空表示未获取到定位。
 * @param longitude 经度，允许为空表示未获取到定位。
 * @param altitude 海拔高度，单位通常为米。
 * @param accuracy 定位精度，单位通常为米。
 * @param address 逆地理解析后的地址文本。
 */
data class ReportLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val address: String? = null
)
