package com.wkq.site.report.model

/**
 * 报告功能版本。
 *
 * 用于区分基础能力、专业付费能力和企业定制能力，后续可据此控制字段、模板、导出水印等功能开关。
 */
enum class ReportEdition {
    /** 基础版，适合免费用户，能力受限。 */
    BASIC,

    /** 专业版，适合付费用户，开放自定义字段和模板等能力。 */
    PRO,

    /** 定制版，适合企业或特殊业务场景，能力完全开放。 */
    CUSTOM
}
