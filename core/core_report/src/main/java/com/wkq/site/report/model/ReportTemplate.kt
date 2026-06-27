package com.wkq.site.report.model

/**
 * 报告模板定义。
 *
 * @param id 模板唯一标识，业务侧保存 templateId 时应使用该值。
 * @param name 模板展示名称。
 * @param assetPath 模板文件在 assets 中的路径，例如 report_templates/default.html。
 * @param minEdition 使用该模板所需的最低版本。
 * @param supportsCustomFields 是否支持渲染自定义字段。
 * @param description 模板说明文案。
 * @param metadata 预留扩展信息，例如模板预览图、行业分类、纸张方向等。
 */
data class ReportTemplate(
    val id: String,
    val name: String,
    val assetPath: String,
    val minEdition: ReportEdition = ReportEdition.BASIC,
    val supportsCustomFields: Boolean = true,
    val description: String = "",
    val metadata: Map<String, String> = emptyMap()
)
