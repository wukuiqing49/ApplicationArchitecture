package com.wkq.site.report.model

/**
 * 报告模板字段定义。
 *
 * 该模型描述模板需要填写什么字段；实际填写后的值仍然保存到 [ReportField]。
 */
data class ReportTemplateField(
    val key: String,
    val label: String,
    val type: ReportFieldType = ReportFieldType.TEXT,
    val required: Boolean = false,
    val order: Int = 0,
    val group: String? = null,
    val defaultValue: String = "",
    val options: List<ReportFieldOption> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    fun toReportField(value: String = defaultValue): ReportField {
        return ReportField(
            key = key,
            label = label,
            value = value,
            type = type,
            required = required,
            order = order,
            group = group,
            options = options,
            metadata = metadata
        )
    }
}
