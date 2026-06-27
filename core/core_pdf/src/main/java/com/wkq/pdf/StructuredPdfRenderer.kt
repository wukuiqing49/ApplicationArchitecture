package com.wkq.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil

class StructuredPdfRenderer(private val context: Context) {

    fun renderToFile(
        document: StructuredPdfDocument,
        outputFile: File,
        config: HtmlPdfRenderConfig = HtmlPdfRenderConfig()
    ): File {
        outputFile.parentFile?.mkdirs()
        val pdf = PdfDocument()
        val state = RenderState(pdf, config, document.theme)
        try {
            state.newPage()
            state.drawTitle(document.title, document.subtitle)
            state.drawMeta(document.meta)
            document.sections.forEach { state.drawSection(it) }
            if (document.footer.isNotBlank()) {
                state.drawFooter(document.footer)
            }
            state.finish()
            FileOutputStream(outputFile).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
        return outputFile
    }

    private inner class RenderState(
        private val pdf: PdfDocument,
        config: HtmlPdfRenderConfig,
        private val theme: StructuredPdfTheme
    ) {
        private val pageWidth = config.mediaSize.widthMils.toPdfPoints().coerceAtLeast(1)
        private val pageHeight = config.mediaSize.heightMils.toPdfPoints().coerceAtLeast(1)
        private val margin = 40f
        private val contentWidth = pageWidth - margin * 2
        private var pageIndex = 0
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = margin

        private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.titleColor
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
        }
        private val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryColor
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        }
        private val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.bodyColor
            textSize = 10.5f
        }
        private val mutedPaint = TextPaint(bodyPaint).apply {
            color = theme.mutedColor
            textSize = 9.5f
        }
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.lineColor
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.fillColor
            style = Paint.Style.FILL
        }
        private val headerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.headerFillColor
            style = Paint.Style.FILL
        }
        private val primaryFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryColor
            style = Paint.Style.FILL
        }

        fun newPage() {
            finishCurrentPage()
            pageIndex += 1
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create())
            canvas = page?.canvas
            y = margin
        }

        fun finish() {
            finishCurrentPage()
        }

        fun drawTitle(title: String, subtitle: String) {
            drawCoverAccent()
            drawText(title, titlePaint, bottomSpace = 8f)
            if (subtitle.isNotBlank()) drawText(subtitle, mutedPaint, bottomSpace = 14f)
        }

        private fun drawCoverAccent() {
            when (theme.coverStyle) {
                PdfCoverStyle.TOP_BAR -> {
                    canvas?.drawRect(margin, y, pageWidth - margin, y + 5f, primaryFillPaint)
                    y += 18f
                }
                PdfCoverStyle.LEFT_BAR -> {
                    canvas?.drawRect(margin, y, margin + 6f, y + 54f, primaryFillPaint)
                    y += 4f
                }
                PdfCoverStyle.BOXED -> {
                    canvas?.drawRoundRect(
                        RectF(margin - 8f, y - 8f, pageWidth - margin + 8f, y + 72f),
                        8f,
                        8f,
                        linePaint
                    )
                    y += 4f
                }
                PdfCoverStyle.MINIMAL -> Unit
            }
        }

        fun drawMeta(meta: List<Pair<String, String>>) {
            if (meta.isEmpty()) return
            ensureSpace(72f)
            val startY = y
            val labelWidth = 62f
            val colWidth = (contentWidth - 12f) / 2f
            meta.chunked(2).forEach { row ->
                var rowHeight = 0f
                row.forEachIndexed { index, pair ->
                    val x = margin + index * (colWidth + 12f)
                    val valueWidth = colWidth - labelWidth
                    rowHeight = maxOf(
                        rowHeight,
                        textHeight(pair.first, mutedPaint, labelWidth) + 2f,
                        textHeight(pair.second.ifBlank { "未填写" }, bodyPaint, valueWidth) + 2f
                    )
                    drawTextAt(pair.first, mutedPaint, x, y, labelWidth)
                    drawTextAt(pair.second.ifBlank { "未填写" }, bodyPaint, x + labelWidth, y, valueWidth)
                }
                y += rowHeight + 6f
            }
            if (theme.tableStyle != PdfTableStyle.LIGHT) {
                canvas?.drawRoundRect(RectF(margin - 8f, startY - 8f, pageWidth - margin + 8f, y + 2f), 8f, 8f, linePaint)
            }
            y += 18f
        }

        fun drawSection(section: StructuredPdfSection) {
            ensureSpace(44f)
            y += 6f
            drawText(section.title, sectionPaint, bottomSpace = 5f)
            canvas?.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 8f
            section.paragraphs.filter { it.isNotBlank() }.forEach {
                drawText(it, bodyPaint, bottomSpace = 8f)
            }
            section.tables.forEach { drawTable(it) }
            if (section.images.isNotEmpty()) drawImages(section.images)
        }

        fun drawFooter(text: String) {
            ensureSpace(32f)
            y += 10f
            canvas?.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 8f
            drawText(text, mutedPaint, bottomSpace = 0f)
        }

        private fun drawTable(table: StructuredPdfTable) {
            if (table.headers.isEmpty()) return
            val columns = table.headers.size.coerceAtLeast(1)
            val widths = List(columns) { contentWidth / columns }
            drawTableRow(table.headers, widths, header = true)
            table.rows.forEach { row ->
                drawTableRow(List(columns) { row.getOrNull(it).orEmpty() }, widths, header = false)
            }
            y += 10f
        }

        private fun drawTableRow(cells: List<String>, widths: List<Float>, header: Boolean) {
            val paint = if (header) TextPaint(bodyPaint).apply { typeface = Typeface.DEFAULT_BOLD } else bodyPaint
            val verticalPadding = if (theme.compactTables) 6f else 10f
            val minRowHeight = if (theme.compactTables) 20f else 24f
            val rowHeight = cells.mapIndexed { index, cell ->
                textHeight(cell.ifBlank { "-" }, paint, widths[index] - 10f) + verticalPadding
            }.maxOrNull()?.coerceAtLeast(minRowHeight) ?: minRowHeight
            ensureSpace(rowHeight)
            var x = margin
            cells.forEachIndexed { index, cell ->
                val rect = RectF(x, y, x + widths[index], y + rowHeight)
                if (header) {
                    canvas?.drawRect(rect, headerFillPaint)
                } else if (theme.tableStyle == PdfTableStyle.STRIPED && ((y / rowHeight).toInt() % 2 == 0)) {
                    canvas?.drawRect(rect, fillPaint)
                }
                if (theme.tableStyle == PdfTableStyle.GRID || header) {
                    canvas?.drawRect(rect, linePaint)
                } else {
                    canvas?.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, linePaint)
                }
                drawTextAt(cell.ifBlank { "-" }, paint, x + 5f, y + verticalPadding / 2f, widths[index] - 10f)
                x += widths[index]
            }
            y += rowHeight
        }

        private fun drawImages(images: List<StructuredPdfImage>) {
            val gap = 10f
            val columns = theme.imageColumns.coerceIn(1, 3)
            val itemWidth = (contentWidth - gap * (columns - 1)) / columns
            val imageHeight = if (columns == 3) 92f else 120f
            images.chunked(columns).forEach { row ->
                ensureSpace(imageHeight + 34f)
                row.forEachIndexed { index, image ->
                    val x = margin + index * (itemWidth + gap)
                    val rect = RectF(x, y, x + itemWidth, y + imageHeight)
                    canvas?.drawRoundRect(rect, 6f, 6f, linePaint)
                    decodeBitmap(image.filePath, itemWidth.toInt(), imageHeight.toInt())?.let { bitmap ->
                        drawBitmapInside(bitmap, rect)
                        bitmap.recycle()
                    }
                    if (image.caption.isNotBlank()) {
                        drawTextAt(image.caption, mutedPaint, x, y + imageHeight + 6f, itemWidth)
                    }
                }
                y += imageHeight + 34f
            }
        }

        private fun drawBitmapInside(bitmap: Bitmap, rect: RectF) {
            val canvas = canvas ?: return
            val scale = maxOf(rect.width() / bitmap.width, rect.height() / bitmap.height)
            val width = bitmap.width * scale
            val height = bitmap.height * scale
            val left = rect.left + (rect.width() - width) / 2f
            val top = rect.top + (rect.height() - height) / 2f
            canvas.save()
            canvas.clipRect(rect)
            canvas.drawBitmap(bitmap, null, RectF(left, top, left + width, top + height), null)
            canvas.restore()
        }

        private fun decodeBitmap(path: String, targetWidth: Int, targetHeight: Int): Bitmap? {
            val file = File(path)
            if (!file.exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val sampleSize = maxOf(
                1,
                ceil(bounds.outWidth / targetWidth.toFloat()).toInt(),
                ceil(bounds.outHeight / targetHeight.toFloat()).toInt()
            )
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            return BitmapFactory.decodeFile(path, options)
        }

        private fun drawText(text: String, paint: TextPaint, bottomSpace: Float) {
            val height = textHeight(text, paint, contentWidth)
            ensureSpace(height + bottomSpace)
            drawTextAt(text, paint, margin, y, contentWidth)
            y += height + bottomSpace
        }

        private fun drawTextAt(text: String, paint: TextPaint, x: Float, top: Float, width: Float) {
            val canvas = canvas ?: return
            canvas.save()
            canvas.translate(x, top)
            buildLayout(text, paint, width).draw(canvas)
            canvas.restore()
        }

        private fun textHeight(text: String, paint: TextPaint, width: Float): Float {
            return buildLayout(text, paint, width).height.toFloat()
        }

        private fun buildLayout(text: String, paint: TextPaint, width: Float): StaticLayout {
            return StaticLayout.Builder
                .obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1f)
                .setIncludePad(false)
                .build()
        }

        private fun ensureSpace(requiredHeight: Float) {
            if (y + requiredHeight > pageHeight - margin) {
                newPage()
            }
        }

        private fun finishCurrentPage() {
            page?.let { pdf.finishPage(it) }
            page = null
            canvas = null
        }
    }

    private fun Int.toPdfPoints(): Int = (this * 72f / 1000f).toInt()
}
