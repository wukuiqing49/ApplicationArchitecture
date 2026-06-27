package com.wkq.site.report.model

/**
 * 自定义字段选项。
 *
 * @param value 选项实际保存值，建议保持稳定，不随展示文案变化。
 * @param label 选项展示文案，用于表单和报告模板展示。
 */
data class ReportFieldOption(
    val value: String,
    val label: String
)
