package com.wkq.site.report.plan

import com.wkq.site.report.model.ReportData
import com.wkq.site.report.model.ReportEdition

/**
 * 报告版本预设能力。
 *
 * 这里集中维护基础版、专业版、定制版的能力边界，后续接入付费状态时只需要选择对应 preset。
 */
object ReportPlanPresets {
    /** 基础版限制：适合免费用户。 */
    val basic = ReportPlanLimits(
        edition = ReportEdition.BASIC,
        maxPhotos = 50,
        maxSigners = 1,
        allowCustomFields = false,
        allowCustomTemplates = false,
        allowWatermarkRemoval = false
    )

    /** 专业版限制：适合一次性买断或订阅用户。 */
    val pro = ReportPlanLimits(
        edition = ReportEdition.PRO,
        maxPhotos = null,
        maxSigners = 3,
        allowCustomFields = true,
        allowCustomTemplates = true,
        allowWatermarkRemoval = true
    )

    /** 定制版限制：适合企业客户或特殊行业模板。 */
    val custom = ReportPlanLimits(
        edition = ReportEdition.CUSTOM,
        maxPhotos = null,
        maxSigners = null,
        allowCustomFields = true,
        allowCustomTemplates = true,
        allowWatermarkRemoval = true
    )

    /**
     * 根据版本返回对应能力限制。
     *
     * @param edition 目标报告版本。
     * @return 对应版本的能力限制。
     */
    fun forEdition(edition: ReportEdition): ReportPlanLimits = when (edition) {
        ReportEdition.BASIC -> basic
        ReportEdition.PRO -> pro
        ReportEdition.CUSTOM -> custom
    }

    /**
     * 校验报告数据是否符合当前版本能力。
     *
     * @param data 待校验的报告数据。
     * @param limits 能力限制，默认根据 data.edition 自动选择。
     * @return 校验结果，包含是否通过和错误原因。
     */
    fun validate(data: ReportData, limits: ReportPlanLimits = forEdition(data.edition)): ReportValidationResult {
        val errors = mutableListOf<String>()

        limits.maxPhotos?.let { maxPhotos ->
            if (data.photos.size > maxPhotos) {
                errors += "照片数量超过当前版本限制：${maxPhotos}"
            }
        }

        limits.maxSigners?.let { maxSigners ->
            if (data.signers.size > maxSigners) {
                errors += "签名数量超过当前版本限制：${maxSigners}"
            }
        }

        if (!limits.allowCustomFields && data.customFields.isNotEmpty()) {
            errors += "当前版本不支持报告自定义字段"
        }

        if (!limits.allowCustomFields && data.photos.any { it.customFields.isNotEmpty() }) {
            errors += "当前版本不支持照片自定义字段"
        }

        if (!limits.allowCustomFields && data.signers.any { it.customFields.isNotEmpty() }) {
            errors += "当前版本不支持签名自定义字段"
        }

        return ReportValidationResult(valid = errors.isEmpty(), errors = errors)
    }
}
