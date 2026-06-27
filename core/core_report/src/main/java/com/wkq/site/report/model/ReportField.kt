package com.wkq.site.report.model

/**
 * 报告通用自定义字段。
 *
 * 该模型用于支撑付费版/定制版扩展能力，可挂在报告、照片、签名等不同对象上。
 *
 * @param key 字段唯一标识，建议使用英文或业务稳定编码，例如 issue_level。
 * @param label 字段展示名称，例如“问题等级”。
 * @param value 字段当前值，第一版统一用字符串保存，渲染时按 type 解释。
 * @param type 字段类型，决定 UI 控件、校验方式和模板展示方式。
 * @param required 是否必填。
 * @param order 字段排序值，数值越小越靠前。
 * @param group 字段分组名称，可用于把字段归类到“项目信息”“整改信息”等区域。
 * @param options 单选/多选字段的候选项。
 * @param metadata 预留扩展信息，例如单位、最大长度、模板样式 key 等。
 */
data class ReportField(
    val key: String,
    val label: String,
    val value: String = "",
    val type: ReportFieldType = ReportFieldType.TEXT,
    val required: Boolean = false,
    val order: Int = 0,
    val group: String? = null,
    val options: List<ReportFieldOption> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)
