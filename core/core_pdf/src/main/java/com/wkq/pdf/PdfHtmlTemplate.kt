package com.wkq.pdf

data class PdfHtmlTemplate(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val proOnly: Boolean,
    val libraryType: String = "professional",
    val bodyClass: String,
    val css: String,
    val theme: StructuredPdfTheme = StructuredPdfTheme()
)

data class StructuredPdfTheme(
    val primaryColor: Int = 0xFF1F6FEB.toInt(),
    val accentColor: Int = 0xFF25A56A.toInt(),
    val titleColor: Int = 0xFF111827.toInt(),
    val bodyColor: Int = 0xFF1F2937.toInt(),
    val mutedColor: Int = 0xFF64748B.toInt(),
    val lineColor: Int = 0xFFD8DEE8.toInt(),
    val fillColor: Int = 0xFFF8FAFC.toInt(),
    val headerFillColor: Int = 0xFFF1F5F9.toInt(),
    val coverStyle: PdfCoverStyle = PdfCoverStyle.TOP_BAR,
    val tableStyle: PdfTableStyle = PdfTableStyle.GRID,
    val imageColumns: Int = 2,
    val compactTables: Boolean = false
)

enum class PdfCoverStyle {
    TOP_BAR,
    LEFT_BAR,
    BOXED,
    MINIMAL
}

enum class PdfTableStyle {
    GRID,
    LIGHT,
    STRIPED
}
