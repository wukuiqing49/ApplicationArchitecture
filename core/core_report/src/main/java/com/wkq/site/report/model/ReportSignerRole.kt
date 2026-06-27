package com.wkq.site.report.model

/**
 * 报告模板中的签名角色定义。
 *
 * 模板定义角色和签署声明，实际签名结果保存到 [ReportSignerInfo]。
 */
data class ReportSignerRole(
    val role: String,
    val label: String,
    val required: Boolean = false,
    val order: Int = 0,
    val consentText: String = "",
    val customFields: List<ReportTemplateField> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    fun createEmptySigner(
        id: String,
        templateId: String,
        templateVersion: Int
    ): ReportSignerInfo {
        return ReportSignerInfo(
            id = id,
            role = role,
            customFields = customFields.map { it.toReportField() },
            consentText = consentText,
            templateId = templateId,
            templateVersion = templateVersion
        )
    }
}
