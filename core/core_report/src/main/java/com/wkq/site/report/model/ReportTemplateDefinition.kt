package com.wkq.site.report.model

/**
 * 完整报告模板定义。
 *
 * [template] 保存模板基础信息；字段和签名角色用于生成表单、校验必填项和渲染签名区。
 */
data class ReportTemplateDefinition(
    val template: ReportTemplate,
    val templateVersion: Int = 1,
    val reportFields: List<ReportTemplateField> = emptyList(),
    val photoFields: List<ReportTemplateField> = emptyList(),
    val signerRoles: List<ReportSignerRole> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    val id: String get() = template.id
    val name: String get() = template.name

    fun createReportFields(): List<ReportField> {
        return reportFields.sortedBy { it.order }.map { it.toReportField() }
    }

    fun createPhotoFields(): List<ReportField> {
        return photoFields.sortedBy { it.order }.map { it.toReportField() }
    }
}
