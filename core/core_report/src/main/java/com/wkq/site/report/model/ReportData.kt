package com.wkq.site.report.model

/**
 * 完整巡检报告数据。
 *
 * 这是报告预览、HTML 渲染、PDF 导出和分享流程的核心入参。
 *
 * @param id 报告唯一标识，建议使用 UUID 或数据库主键。
 * @param title 报告标题。
 * @param projectName 项目名称。
 * @param createdAt 报告创建时间，毫秒时间戳。
 * @param templateId 当前选择的模板 id。
 * @param edition 当前报告使用的版本能力，用于校验基础版/付费版/定制版限制。
 * @param photos 报告包含的照片列表。
 * @param signers 报告签名列表，可支持多个签名角色。
 * @param customFields 报告级自定义字段，例如项目地址、客户名称、验收结果等。
 * @param metadata 预留扩展信息，例如水印配置、导出来源、报告编号等。
 */
data class ReportData(
    val id: String,
    val title: String,
    val projectName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val templateId: String = DEFAULT_TEMPLATE_ID,
    val edition: ReportEdition = ReportEdition.BASIC,
    val photos: List<ReportPhotoItem> = emptyList(),
    val signers: List<ReportSignerInfo> = emptyList(),
    val customFields: List<ReportField> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        /** 默认模板 id。 */
        const val DEFAULT_TEMPLATE_ID = "default"
    }
}
