package com.wkq.site.report.template

import com.wkq.site.report.model.ReportData
import com.wkq.site.report.model.ReportField
import com.wkq.site.report.model.ReportPhotoItem
import com.wkq.site.report.model.ReportSignerInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 报告 HTML 模板渲染器。
 *
 * 第一版使用简单占位符替换，后续如果模板逻辑复杂，可以在不改变 ReportData 模型的前提下替换实现。
 *
 * @param dateFormat 时间格式化器，用于把毫秒时间戳渲染为报告展示文本。
 */
class ReportTemplateEngine(
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
) {
    /**
     * 渲染完整 HTML 报告。
     *
     * 支持的基础占位符包括：reportTitle、projectName、createdAt、photoList、signerList、customFields。
     *
     * @param templateHtml 原始 HTML 模板内容。
     * @param data 报告数据。
     * @return 替换占位符后的完整 HTML。
     */
    fun render(templateHtml: String, data: ReportData): String {
        return templateHtml
            .replace("{{reportTitle}}", HtmlEscaper.escape(data.title))
            .replace("{{projectName}}", HtmlEscaper.escape(data.projectName))
            .replace("{{createdAt}}", formatTime(data.createdAt))
            .replace("{{photoList}}", renderPhotos(data.photos))
            .replace("{{signerList}}", renderSigners(data.signers))
            .replace("{{customFields}}", renderFields(data.customFields))
    }

    /**
     * 渲染照片列表 HTML。
     *
     * @param photos 报告照片列表。
     * @return 照片区域 HTML。
     */
    private fun renderPhotos(photos: List<ReportPhotoItem>): String {
        return photos.sortedBy { it.sortOrder }.joinToString(separator = "\n") { photo ->
            val locationText = photo.location?.let { location ->
                listOfNotNull(
                    location.latitude?.let { "Lat: $it" },
                    location.longitude?.let { "Lng: $it" },
                    location.address
                ).joinToString(" / ")
            }.orEmpty()

            """
            <section class="photo-item">
              <h3>${HtmlEscaper.escape(photo.title)}</h3>
              <img class="report-photo" src="${HtmlEscaper.escape(photo.imagePath)}" />
              <p>${HtmlEscaper.escape(photo.note)}</p>
              <p class="meta">${formatTime(photo.capturedAt)} ${HtmlEscaper.escape(locationText)}</p>
              ${renderFields(photo.customFields)}
            </section>
            """.trimIndent()
        }
    }

    /**
     * 渲染签名列表 HTML。
     *
     * @param signers 报告签名列表。
     * @return 签名区域 HTML。
     */
    private fun renderSigners(signers: List<ReportSignerInfo>): String {
        return signers.joinToString(separator = "\n") { signer ->
            val signature = signer.signatureImagePath?.takeIf { it.isNotBlank() }?.let {
                """<img class="signature-image" src="${HtmlEscaper.escape(it)}" />"""
            }.orEmpty()

            """
            <section class="signer-item">
              <p>${HtmlEscaper.escape(signer.role)}</p>
              <p>${HtmlEscaper.escape(signer.company)} ${HtmlEscaper.escape(signer.position)}</p>
              <p>${HtmlEscaper.escape(signer.name)}</p>
              $signature
              <p class="meta">${formatTime(signer.signedAt)}</p>
              ${renderFields(signer.customFields)}
            </section>
            """.trimIndent()
        }
    }

    /**
     * 渲染自定义字段表格。
     *
     * @param fields 自定义字段列表。
     * @return 字段表格 HTML，无字段时返回空字符串。
     */
    private fun renderFields(fields: List<ReportField>): String {
        if (fields.isEmpty()) return ""
        return fields.sortedBy { it.order }.joinToString(
            prefix = """<table class="custom-fields"><tbody>""",
            postfix = "</tbody></table>",
            separator = "\n"
        ) { field ->
            """
            <tr>
              <th>${HtmlEscaper.escape(field.label)}</th>
              <td>${HtmlEscaper.escape(field.value)}</td>
            </tr>
            """.trimIndent()
        }
    }

    /**
     * 格式化时间戳。
     *
     * @param timeMillis 毫秒时间戳，可为空。
     * @return 格式化后的时间文本。
     */
    private fun formatTime(timeMillis: Long?): String {
        return timeMillis?.let { dateFormat.format(Date(it)) }.orEmpty()
    }
}
