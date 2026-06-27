package com.wkq.site.report.plan

import com.wkq.site.report.model.ReportEdition

/**
 * 报告版本能力限制。
 *
 * @param edition 对应的版本。
 * @param maxPhotos 最大照片数量，null 表示不限制。
 * @param maxSigners 最大签名数量，null 表示不限制。
 * @param allowCustomFields 是否允许报告、照片、签名使用自定义字段。
 * @param allowCustomTemplates 是否允许使用自定义 HTML 模板。
 * @param allowWatermarkRemoval 是否允许移除导出 PDF 水印。
 */
data class ReportPlanLimits(
    val edition: ReportEdition,
    val maxPhotos: Int?,
    val maxSigners: Int?,
    val allowCustomFields: Boolean,
    val allowCustomTemplates: Boolean,
    val allowWatermarkRemoval: Boolean
)
