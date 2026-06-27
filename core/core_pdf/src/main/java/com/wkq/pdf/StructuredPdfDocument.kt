package com.wkq.pdf

data class StructuredPdfDocument(
    val title: String,
    val subtitle: String = "",
    val meta: List<Pair<String, String>> = emptyList(),
    val sections: List<StructuredPdfSection> = emptyList(),
    val footer: String = "",
    val theme: StructuredPdfTheme = StructuredPdfTheme()
)

data class StructuredPdfSection(
    val title: String,
    val paragraphs: List<String> = emptyList(),
    val tables: List<StructuredPdfTable> = emptyList(),
    val images: List<StructuredPdfImage> = emptyList(),
    val type: String = StructuredPdfSectionType.DEFAULT
)

object StructuredPdfSectionType {
    const val DEFAULT = "default"
    const val REPORT_SUMMARY = "report_summary"
    const val ISSUE_SUMMARY = "issue_summary"
    const val INSPECTION_DETAILS = "inspection_details"
    const val PHOTO_EVIDENCE = "photo_evidence"
    const val SIGN_OFF = "sign_off"
}

data class StructuredPdfTable(
    val headers: List<String>,
    val rows: List<List<String>>
)

data class StructuredPdfImage(
    val filePath: String,
    val caption: String = ""
)
