package com.wkq.site.report.model

/**
 * 报告签名信息。
 *
 * 基础版通常只需要一个巡检人签名；付费版/定制版可以通过多个 signer 和 customFields 支持客户、监理、
 * 审核人等多角色签名。
 *
 * @param id 签名记录唯一标识。
 * @param role 签名角色，例如 inspector、customer、supervisor。
 * @param company 公司或组织名称。
 * @param position 职位。
 * @param name 签名人姓名。
 * @param signatureImagePath 手写签名图片路径，可为空。
 * @param signedAt 签署时间，毫秒时间戳。
 * @param customFields 签名级自定义字段，例如联系电话、证件号、审核意见等。
 * @param metadata 预留扩展信息，例如签名来源、签名画布尺寸等。
 */
data class ReportSignerInfo(
    val id: String,
    val role: String = DEFAULT_ROLE,
    val company: String = "",
    val position: String = "",
    val name: String = "",
    val signatureImagePath: String? = null,
    val signedAt: Long? = null,
    val customFields: List<ReportField> = emptyList(),
    val signedLocation: ReportLocation? = null,
    val consentText: String = "",
    val contentHash: String = "",
    val templateId: String = "",
    val templateVersion: Int = 1,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        /** 默认签名角色：巡检人。 */
        const val DEFAULT_ROLE = "inspector"
    }
}
