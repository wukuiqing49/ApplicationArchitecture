package com.wkq.site.report.template

/**
 * HTML 文本转义工具。
 *
 * 用于避免用户输入的标题、备注、自定义字段等内容破坏 HTML 结构。
 */
internal object HtmlEscaper {
    /**
     * 将普通文本转换为可安全插入 HTML 的文本。
     *
     * @param value 原始文本，可为空。
     * @return 转义后的 HTML 文本。
     */
    fun escape(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        return buildString(value.length) {
            value.forEach { char ->
                when (char) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&#39;")
                    else -> append(char)
                }
            }
        }
    }
}
