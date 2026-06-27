package com.wkq.pdf

import java.util.Locale

/**
 * 将 StructuredPdfDocument 序列化为精美的 HTML 页面，以供 WebView 进行高质量 PDF 打印渲染。
 */
fun StructuredPdfDocument.toHtml(template: PdfHtmlTemplate): String {
    val primaryColor = template.theme.primaryColor.toCssHex()
    val accentColor = template.theme.accentColor.toCssHex()
    val titleColor = template.theme.titleColor.toCssHex()
    val bodyColor = template.theme.bodyColor.toCssHex()
    val mutedColor = template.theme.mutedColor.toCssHex()
    val lineColor = template.theme.lineColor.toCssHex()
    val fillColor = template.theme.fillColor.toCssHex()
    val headerFillColor = template.theme.headerFillColor.toCssHex()
    val bodyClass = template.bodyClass.escapeHtml()

    val sb = StringBuilder()
    sb.append("""
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            @import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap');
            @page {
                size: A4;
                margin: 1.6cm 1.4cm;
            }
            body {
                font-family: 'Outfit', -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Noto Sans CJK SC", sans-serif;
                color: $bodyColor;
                line-height: 1.6;
                font-size: 10pt;
                margin: 0;
                padding: 0;
                background-color: #ffffff;
            }
            .report-shell {
                width: 100%;
            }
            h1, h2, h3, h4 {
                color: $titleColor;
                margin-top: 0;
            }
            .header {
                margin-bottom: 24px;
                padding-bottom: 18px;
            }
            .title-container {
                position: relative;
            }
            
            /* Cover Styles */
            ${when (template.theme.coverStyle) {
                PdfCoverStyle.TOP_BAR -> """
                    .title-container {
                        padding-top: 20px;
                        border-top: 6px solid $primaryColor;
                    }
                """.trimIndent()
                PdfCoverStyle.LEFT_BAR -> """
                    .title-container {
                        border-left: 6px solid $primaryColor;
                        padding-left: 18px;
                    }
                """.trimIndent()
                PdfCoverStyle.BOXED -> """
                    .header {
                        border: 1px solid $lineColor;
                        border-radius: 12px;
                        padding: 24px;
                        background-color: $fillColor;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.02);
                    }
                """.trimIndent()
                PdfCoverStyle.MINIMAL -> ""
            }}
            
            .title {
                font-size: 26pt;
                font-weight: 700;
                color: $primaryColor;
                margin-bottom: 8px;
                line-height: 1.25;
                letter-spacing: -0.5px;
            }
            .subtitle {
                font-size: 12pt;
                color: $mutedColor;
                margin: 0;
            }
            
            /* Meta Grid - Premium Card */
            .meta-grid {
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 14px;
                margin-bottom: 30px;
                padding: 20px 24px;
                background-color: $fillColor;
                border: 1px solid $lineColor;
                border-radius: 12px;
                box-shadow: 0 4px 20px rgba(0,0,0,0.02);
            }
            .meta-item {
                display: flex;
                font-size: 9.5pt;
                align-items: baseline;
            }
            .meta-label {
                width: 130px;
                color: $mutedColor;
                font-weight: 600;
                flex-shrink: 0;
            }
            .meta-value {
                flex: 1;
                font-weight: 400;
                color: $bodyColor;
            }
            
            /* Section */
            .section {
                margin-bottom: 32px;
                break-inside: auto;
                page-break-inside: auto;
            }
            .section-title {
                font-size: 14pt;
                font-weight: 600;
                color: $primaryColor;
                margin-top: 0;
                margin-bottom: 12px;
                border-bottom: 2px solid $lineColor;
                padding-bottom: 6px;
                letter-spacing: -0.2px;
                break-after: avoid;
                page-break-after: avoid;
            }
            .paragraph {
                margin: 0 0 12px 0;
                text-align: justify;
                font-size: 9.5pt;
                color: $bodyColor;
            }
            .summary-strip {
                display: grid;
                grid-template-columns: repeat(4, 1fr);
                gap: 10px;
                margin: 10px 0 12px;
            }
            .summary-item {
                border: 1px solid $lineColor;
                border-radius: 8px;
                background: $fillColor;
                padding: 9px 10px;
                break-inside: avoid;
                page-break-inside: avoid;
            }
            .summary-label {
                display: block;
                color: $mutedColor;
                font-size: 8pt;
                font-weight: 600;
                margin-bottom: 3px;
            }
            .summary-value {
                display: block;
                color: $primaryColor;
                font-size: 13pt;
                font-weight: 700;
                line-height: 1.2;
            }
            .table-block {
                width: 100%;
                overflow: hidden;
                break-inside: auto;
                page-break-inside: auto;
            }
            
            /* Tables - Clean modern layout */
            table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 14px;
                margin-bottom: 20px;
                font-size: 9pt;
                border-radius: 8px;
                overflow: hidden;
                break-inside: auto;
                page-break-inside: auto;
            }
            thead {
                display: table-header-group;
            }
            tfoot {
                display: table-footer-group;
            }
            tr {
                break-inside: avoid;
                page-break-inside: avoid;
            }
            th, td {
                padding: ${if (template.theme.compactTables) "8px 10px" else "12px 14px"};
                text-align: left;
                vertical-align: top;
                break-inside: avoid;
                page-break-inside: avoid;
            }
            
            /* Table Style Customization */
            ${when (template.theme.tableStyle) {
                PdfTableStyle.GRID -> """
                    table {
                        border: 1px solid $lineColor;
                    }
                    th, td {
                        border: 1px solid $lineColor;
                    }
                    th {
                        background-color: $headerFillColor;
                        color: $titleColor;
                        font-weight: 600;
                    }
                """.trimIndent()
                PdfTableStyle.LIGHT -> """
                    th, td {
                        border-bottom: 1px solid $lineColor;
                    }
                    th {
                        color: $primaryColor;
                        font-weight: 600;
                        border-bottom: 2px solid $primaryColor;
                    }
                """.trimIndent()
                PdfTableStyle.STRIPED -> """
                    th {
                        background-color: $primaryColor;
                        color: #ffffff;
                        font-weight: 600;
                    }
                    tr:nth-child(even) {
                        background-color: $fillColor;
                    }
                    td {
                        border-bottom: 1px solid $lineColor;
                    }
                """.trimIndent()
            }}
            
            /* Badge - Pill shape with border */
            .badge {
                display: inline-block;
                padding: 3px 10px;
                font-size: 8pt;
                font-weight: 600;
                border-radius: 100px;
                white-space: nowrap;
                border: 1px solid transparent;
            }
            .badge-success {
                background-color: #ecfdf5;
                color: #059669;
                border-color: #a7f3d0;
            }
            .badge-danger {
                background-color: #fef2f2;
                color: #dc2626;
                border-color: #fecaca;
            }
            .badge-warning {
                background-color: #fffbeb;
                color: #d97706;
                border-color: #fde68a;
            }
            .badge-info {
                background-color: #f0f9ff;
                color: #0284c7;
                border-color: #bae6fd;
            }
            
            /* Images - Premium Cards */
            .image-grid {
                display: grid;
                grid-template-columns: repeat(${template.theme.imageColumns}, 1fr);
                gap: 14px;
                margin-top: 14px;
                margin-bottom: 20px;
            }
            .image-card {
                border: 1px solid $lineColor;
                border-radius: 10px;
                overflow: hidden;
                background-color: $fillColor;
                break-inside: avoid;
                page-break-inside: avoid;
                box-shadow: 0 4px 12px rgba(0,0,0,0.03);
            }
            .image-card img {
                width: 100%;
                height: ${if (template.theme.imageColumns == 3) "120px" else "170px"};
                object-fit: cover;
                display: block;
                background-color: #f8fafc;
            }
            .image-caption {
                padding: 8px 12px;
                font-size: 8pt;
                color: $mutedColor;
                text-align: center;
                border-top: 1px solid $lineColor;
                line-height: 1.4;
                background-color: #ffffff;
            }
            
            /* Footer */
            .footer {
                position: fixed;
                bottom: 0;
                left: 0;
                width: 100%;
                border-top: 1px solid $lineColor;
                padding-top: 10px;
                font-size: 8pt;
                color: $mutedColor;
                display: flex;
                justify-content: space-between;
            }
            
            /* Custom CSS from Template */
            ${template.css}
        </style>
        </head>
        <body class="$bodyClass">
        <main class="report-shell">
    """.trimIndent())

    // Title / Subtitle
    sb.append("""
        <div class="header">
            <div class="title-container">
                <div class="title">${title.escapeHtml()}</div>
                ${if (subtitle.isNotBlank()) """<div class="subtitle">${subtitle.escapeHtml()}</div>""" else ""}
            </div>
        </div>
    """.trimIndent())

    // Meta Info
    if (meta.isNotEmpty()) {
        sb.append("<div class=\"meta-grid\">")
        meta.forEach { (label, value) ->
            sb.append("""
                <div class="meta-item">
                    <div class="meta-label">${label.escapeHtml()}</div>
                    <div class="meta-value">${value.escapeHtml().formatMetaValue()}</div>
                </div>
            """.trimIndent())
        }
        sb.append("</div>")
    }

    // Sections
    sections.orderedFor(template.bodyClass).forEach { section ->
        val sectionClass = section.toSectionClass()
        sb.append("<section class=\"section $sectionClass\">")
        sb.append("<h2 class=\"section-title\">${section.title.escapeHtml()}</h2>")
        
        section.paragraphs.filter { it.isNotBlank() }.forEach { p ->
            val summaryCards = if (sectionClass == "section-report-summary") p.toSummaryCards() else null
            if (summaryCards != null) {
                sb.append(summaryCards)
            } else {
                sb.append("<p class=\"paragraph\">${p.escapeHtml()}</p>")
            }
        }
        
        // Tables
        section.tables.forEach { table ->
            if (table.headers.isNotEmpty()) {
                sb.append("<div class=\"table-block\"><table><thead><tr>")
                table.headers.forEach { th ->
                    sb.append("<th>${th.escapeHtml()}</th>")
                }
                sb.append("</tr></thead><tbody>")
                table.rows.forEach { row ->
                    sb.append("<tr>")
                    row.forEach { td ->
                        sb.append("<td>${td.escapeHtml().formatCellBadge()}</td>")
                    }
                    sb.append("</tr>")
                }
                sb.append("</tbody></table></div>")
            }
        }
        
        // Images
        if (section.images.isNotEmpty()) {
            sb.append("<div class=\"image-grid\">")
            section.images.forEach { image ->
                val src = "file://${image.filePath}"
                sb.append("""
                    <div class="image-card">
                        <img src="$src" />
                        ${if (image.caption.isNotBlank()) """<div class="image-caption">${image.caption.escapeHtml().replace("\n", "<br/>")}</div>""" else ""}
                    </div>
                """.trimIndent())
            }
            sb.append("</div>")
        }
        
        sb.append("</section>")
    }

    sb.append("</main>")

    // Footer
    if (footer.isNotBlank()) {
        sb.append("""
            <div class="footer">
                <span>${footer.escapeHtml()}</span>
                <span class="page-number"></span>
            </div>
        """.trimIndent())
    }

    sb.append("</body></html>")
    return sb.toString()
}

private fun List<StructuredPdfSection>.orderedFor(bodyClass: String): List<StructuredPdfSection> {
    val priority = when (bodyClass) {
        "template-archive" -> listOf(
            StructuredPdfSectionType.REPORT_SUMMARY,
            StructuredPdfSectionType.INSPECTION_DETAILS,
            StructuredPdfSectionType.ISSUE_SUMMARY,
            StructuredPdfSectionType.PHOTO_EVIDENCE,
            StructuredPdfSectionType.SIGN_OFF
        )
        "template-rectification",
        "template-compliance",
        "template-renovation" -> listOf(
            StructuredPdfSectionType.ISSUE_SUMMARY,
            StructuredPdfSectionType.REPORT_SUMMARY,
            StructuredPdfSectionType.INSPECTION_DETAILS,
            StructuredPdfSectionType.PHOTO_EVIDENCE,
            StructuredPdfSectionType.SIGN_OFF
        )
        "template-recheck" -> listOf(
            StructuredPdfSectionType.REPORT_SUMMARY,
            StructuredPdfSectionType.SIGN_OFF,
            StructuredPdfSectionType.ISSUE_SUMMARY,
            StructuredPdfSectionType.INSPECTION_DETAILS,
            StructuredPdfSectionType.PHOTO_EVIDENCE
        )
        "template-photo-evidence",
        "template-dossier" -> listOf(
            StructuredPdfSectionType.REPORT_SUMMARY,
            StructuredPdfSectionType.PHOTO_EVIDENCE,
            StructuredPdfSectionType.ISSUE_SUMMARY,
            StructuredPdfSectionType.INSPECTION_DETAILS,
            StructuredPdfSectionType.SIGN_OFF
        )
        "template-executive" -> listOf(
            StructuredPdfSectionType.REPORT_SUMMARY,
            StructuredPdfSectionType.ISSUE_SUMMARY,
            StructuredPdfSectionType.SIGN_OFF,
            StructuredPdfSectionType.INSPECTION_DETAILS,
            StructuredPdfSectionType.PHOTO_EVIDENCE
        )
        "template-client-minimal" -> listOf(
            StructuredPdfSectionType.REPORT_SUMMARY,
            StructuredPdfSectionType.INSPECTION_DETAILS,
            StructuredPdfSectionType.ISSUE_SUMMARY,
            StructuredPdfSectionType.SIGN_OFF,
            StructuredPdfSectionType.PHOTO_EVIDENCE
        )
        else -> listOf(
            StructuredPdfSectionType.REPORT_SUMMARY,
            StructuredPdfSectionType.ISSUE_SUMMARY,
            StructuredPdfSectionType.INSPECTION_DETAILS,
            StructuredPdfSectionType.PHOTO_EVIDENCE,
            StructuredPdfSectionType.SIGN_OFF
        )
    }
    return sortedWith(compareBy<StructuredPdfSection> {
        val index = priority.indexOf(it.normalizedType())
        if (index >= 0) index else priority.size
    }.thenBy {
        indexOf(it)
    })
}

private fun StructuredPdfSection.toSectionClass(): String {
    val slug = normalizedType()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "section" }
    return "section-$slug"
}

private fun StructuredPdfSection.normalizedType(): String {
    if (type.isNotBlank() && type != StructuredPdfSectionType.DEFAULT) return type
    return when (title) {
        "Report Summary" -> StructuredPdfSectionType.REPORT_SUMMARY
        "Issue Summary" -> StructuredPdfSectionType.ISSUE_SUMMARY
        "Inspection Details" -> StructuredPdfSectionType.INSPECTION_DETAILS
        "Photo Evidence" -> StructuredPdfSectionType.PHOTO_EVIDENCE
        "Sign-off" -> StructuredPdfSectionType.SIGN_OFF
        else -> title.lowercase(Locale.US)
    }
}

private fun String.toSummaryCards(): String? {
    val parts = split("|")
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (parts.size < 2) return null
    return parts.joinToString(
        separator = "",
        prefix = "<div class=\"summary-strip\">",
        postfix = "</div>"
    ) { part ->
        val labelValue = part.split(":", limit = 2)
        val label = labelValue.getOrNull(0).orEmpty().trim()
        val value = labelValue.getOrNull(1)?.trim().orEmpty()
        """
            <div class="summary-item">
                <span class="summary-label">${label.escapeHtml()}</span>
                <span class="summary-value">${value.ifBlank { "-" }.escapeHtml()}</span>
            </div>
        """.trimIndent()
    }
}

private fun Int.toCssHex(): String {
    return String.format(Locale.US, "#%06X", this and 0xFFFFFF)
}

private fun String.escapeHtml(): String {
    return this.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}

private fun String.formatCellBadge(): String {
    val trimmed = this.trim()
    return when {
        trimmed.equals("Pass", ignoreCase = true) || trimmed == "已通过" || trimmed == "通过" || trimmed == "符合" -> {
            "<span class=\"badge badge-success\">$trimmed</span>"
        }
        trimmed.equals("Fail", ignoreCase = true) || trimmed.equals("Needs Fix", ignoreCase = true) || trimmed == "不通过" || trimmed == "不符合" || trimmed == "需整改" -> {
            "<span class=\"badge badge-danger\">$trimmed</span>"
        }
        trimmed.equals("Pending", ignoreCase = true) || trimmed == "待检查" || trimmed == "待定" -> {
            "<span class=\"badge badge-warning\">$trimmed</span>"
        }
        trimmed.equals("Fixed", ignoreCase = true) || trimmed == "已整改" -> {
            "<span class=\"badge badge-info\">$trimmed</span>"
        }
        else -> this.replace("\n", "<br/>")
    }
}

private fun String.formatMetaValue(): String {
    val trimmed = this.trim()
    return when {
        trimmed.contains("Pass") || trimmed.contains("通过") || trimmed.contains("符合") || trimmed.contains("良好") -> {
            "<span style=\"color: #059669; font-weight: 600;\">$trimmed</span>"
        }
        trimmed.contains("Fail") || trimmed.contains("不通过") || trimmed.contains("不符合") || trimmed.contains("需整改") || trimmed.contains("缺陷") -> {
            "<span style=\"color: #dc2626; font-weight: 600;\">$trimmed</span>"
        }
        else -> this
    }
}
