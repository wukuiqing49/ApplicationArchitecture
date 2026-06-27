package com.wkq.pdf

import android.print.PrintAttributes

data class HtmlPdfRenderConfig(
    val jobName: String = "html_report",
    val baseUrl: String? = null,
    val mediaSize: PrintAttributes.MediaSize = PrintAttributes.MediaSize.ISO_A4,
    val margins: PrintAttributes.Margins = PrintAttributes.Margins.NO_MARGINS,
    val resolution: PrintAttributes.Resolution = PrintAttributes.Resolution("pdf", "pdf", 300, 300),
    val colorMode: Int = PrintAttributes.COLOR_MODE_COLOR,
    val renderDelayMillis: Long = 300L,
    val timeoutMillis: Long = 30_000L
)
