package com.wkq.site.report.model

/**
 * 巡检报告中的单张照片数据。
 *
 * 基础版使用 imagePath、title、note、capturedAt、location 等固定字段；付费版/定制版可通过 customFields
 * 扩展检查项、问题等级、整改状态、房间号等业务字段。
 *
 * @param id 照片唯一标识，建议使用 UUID 或数据库主键。
 * @param imagePath 图片路径，可为本地文件路径、file uri 或后续模板引擎支持的资源地址。
 * @param title 照片标题，例如“消防通道检查”。
 * @param note 照片备注或问题描述。
 * @param capturedAt 拍摄时间，毫秒时间戳。
 * @param location 拍摄地点信息。
 * @param tags 照片标签，例如“整改前”“隐患”“合格”。
 * @param customFields 照片级自定义字段，主要用于付费版/定制版扩展。
 * @param sortOrder 报告中的排序值，数值越小越靠前。
 * @param metadata 预留扩展信息，例如原图宽高、压缩图路径、来源页面等。
 */
data class ReportPhotoItem(
    val id: String,
    val imagePath: String,
    val title: String = "",
    val note: String = "",
    val capturedAt: Long? = null,
    val location: ReportLocation? = null,
    val tags: List<String> = emptyList(),
    val customFields: List<ReportField> = emptyList(),
    val sortOrder: Int = 0,
    val metadata: Map<String, String> = emptyMap()
)
