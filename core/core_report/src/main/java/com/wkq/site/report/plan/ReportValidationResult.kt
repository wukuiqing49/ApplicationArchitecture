package com.wkq.site.report.plan

/**
 * 报告版本能力校验结果。
 *
 * @param valid 是否通过校验。
 * @param errors 校验失败原因列表，可直接用于 UI 提示或日志。
 */
data class ReportValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList()
)
