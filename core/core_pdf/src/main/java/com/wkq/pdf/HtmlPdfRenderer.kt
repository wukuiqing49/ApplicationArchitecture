package com.wkq.pdf

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class HtmlPdfRenderer(private val context: Context) {

    suspend fun renderToFile(
        html: String,
        outputFile: File,
        config: HtmlPdfRenderConfig = HtmlPdfRenderConfig()
    ): File = withContext(Dispatchers.Main) {
        outputFile.parentFile?.mkdirs()
        val webView = createWebView()
        try {
            withTimeout(config.timeoutMillis) {
                // 1. 在加载前，就将 WebView 测量和布局为 A4 的标准宽度和高度，避免 viewport 折叠为 0
                val pageWidth = config.mediaSize.widthMils.toPdfPoints().coerceAtLeast(1)
                val pageHeight = config.mediaSize.heightMils.toPdfPoints().coerceAtLeast(1)
                val density = context.resources.displayMetrics.density
                val targetWidthPx = (pageWidth * density).toInt().coerceAtLeast(1)
                val targetHeightPx = (pageHeight * density).toInt().coerceAtLeast(1)
                
                webView.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(targetWidthPx, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(targetHeightPx, android.view.View.MeasureSpec.EXACTLY)
                )
                webView.layout(0, 0, targetWidthPx, targetHeightPx)

                // 2. 默认使用 "file:///" 作为 baseUrl 以解锁本地文件读取权限
                val baseUrl = config.baseUrl ?: "file:///"
                webView.loadHtmlAndAwait(html, baseUrl)
                
                webView.awaitRendered(config.renderDelayMillis)
                
                // 3. 执行 PDF 导出
                webView.drawToPdf(outputFile, config)
            }
            outputFile
        } catch (error: TimeoutCancellationException) {
            throw IllegalStateException("HTML PDF 生成超时", error)
        } finally {
            webView.stopLoading()
            webView.webViewClient = WebViewClient()
            webView.destroy()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        return WebView(context.applicationContext).apply {
            setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.loadsImagesAutomatically = true
        }
    }

    private suspend fun WebView.loadHtmlAndAwait(html: String, baseUrl: String?) {
        suspendCancellableCoroutine { continuation ->
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true

                override fun onPageFinished(view: WebView, url: String?) {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
            continuation.invokeOnCancellation {
                webViewClient = WebViewClient()
                stopLoading()
            }
            loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }
    }

    private suspend fun WebView.awaitRendered(renderDelayMillis: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            suspendCancellableCoroutine { continuation ->
                postVisualStateCallback(
                    System.nanoTime(),
                    object : WebView.VisualStateCallback() {
                        override fun onComplete(requestId: Long) {
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    }
                )
            }
        }
        if (renderDelayMillis > 0L) delay(renderDelayMillis)
    }

    private suspend fun WebView.drawToPdf(outputFile: File, config: HtmlPdfRenderConfig) {
        val pageWidth = config.mediaSize.widthMils.toPdfPoints().coerceAtLeast(1)
        val pageHeight = config.mediaSize.heightMils.toPdfPoints().coerceAtLeast(1)
        val density = context.resources.displayMetrics.density
        val targetWidthPx = (pageWidth * density).toInt().coerceAtLeast(1)

        // 重新测绘获取页面的实际内容高度
        measure(
            android.view.View.MeasureSpec.makeMeasureSpec(targetWidthPx, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        )
        val measuredContentHeight = measuredHeight.coerceAtLeast((pageHeight * density).toInt())
        layout(0, 0, targetWidthPx, measuredContentHeight)
        invalidate()
        
        // 挂起短暂时间，让 WebView 彻底重绘和排版
        delay(150L)

        applyPdfPageBreaks(config)
        measure(
            android.view.View.MeasureSpec.makeMeasureSpec(targetWidthPx, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        )
        val paginatedContentHeight = measuredHeight.coerceAtLeast((pageHeight * density).toInt())
        layout(0, 0, targetWidthPx, paginatedContentHeight)
        delay(80L)

        val contentWidthPx = width.coerceAtLeast(targetWidthPx)
        val contentHeightPx = (contentHeight * density).toInt().coerceAtLeast(paginatedContentHeight)
        val pageHeightPx = (contentWidthPx * pageHeight.toFloat() / pageWidth.toFloat()).toInt().coerceAtLeast(1)
        val pageCount = ((contentHeightPx + pageHeightPx - 1) / pageHeightPx).coerceAtLeast(1)

        val document = PdfDocument()
        try {
            for (pageIndex in 0 until pageCount) {
                val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create())
                page.canvas.save()
                val scale = pageWidth.toFloat() / contentWidthPx.toFloat()
                page.canvas.scale(scale, scale)
                page.canvas.translate(0f, -(pageIndex * pageHeightPx).toFloat())
                draw(page.canvas)
                page.canvas.restore()
                document.finishPage(page)
            }
            FileOutputStream(outputFile).use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    private suspend fun WebView.applyPdfPageBreaks(config: HtmlPdfRenderConfig) {
        val pageWidth = config.mediaSize.widthMils.toPdfPoints().coerceAtLeast(1)
        val pageHeight = config.mediaSize.heightMils.toPdfPoints().coerceAtLeast(1)
        val script = """
            (function() {
                var pageHeight = Math.max(1, window.innerWidth * $pageHeight / $pageWidth);
                var guard = 0;
                function crossesPage(el) {
                    var rect = el.getBoundingClientRect();
                    var top = rect.top + window.scrollY;
                    var height = rect.height;
                    if (!height || height >= pageHeight * 0.92) return 0;
                    var offset = top % pageHeight;
                    var remaining = pageHeight - offset;
                    return height > remaining ? remaining : 0;
                }
                function insertBlockSpacer(el, height) {
                    var spacer = document.createElement('div');
                    spacer.className = 'pdf-page-spacer';
                    spacer.style.height = Math.ceil(height + 1) + 'px';
                    spacer.style.breakAfter = 'avoid';
                    spacer.style.pageBreakAfter = 'avoid';
                    el.parentNode.insertBefore(spacer, el);
                }
                function insertTableSpacer(row, height) {
                    var cols = 1;
                    var cells = row.children || [];
                    for (var i = 0; i < cells.length; i++) {
                        cols += Math.max(0, parseInt(cells[i].getAttribute('colspan') || '1', 10) - 1);
                    }
                    cols = Math.max(cols, cells.length || 1);
                    var spacer = document.createElement('tr');
                    spacer.className = 'pdf-page-spacer-row';
                    var td = document.createElement('td');
                    td.setAttribute('colspan', cols);
                    td.style.height = Math.ceil(height + 1) + 'px';
                    td.style.padding = '0';
                    td.style.border = '0';
                    td.style.background = 'transparent';
                    spacer.appendChild(td);
                    row.parentNode.insertBefore(spacer, row);
                }
                document.querySelectorAll('.pdf-page-spacer,.pdf-page-spacer-row').forEach(function(el) {
                    el.parentNode && el.parentNode.removeChild(el);
                });
                var selectors = [
                    '.section-title',
                    '.summary-item',
                    '.paragraph',
                    '.image-card',
                    'tbody tr'
                ].join(',');
                while (guard++ < 8) {
                    var changed = false;
                    var elements = Array.prototype.slice.call(document.querySelectorAll(selectors))
                        .filter(function(el) { return !el.classList.contains('pdf-page-spacer-row'); });
                    for (var i = 0; i < elements.length; i++) {
                        var el = elements[i];
                        var spacerHeight = crossesPage(el);
                        if (spacerHeight > 0) {
                            if (el.tagName && el.tagName.toLowerCase() === 'tr') {
                                insertTableSpacer(el, spacerHeight);
                            } else {
                                insertBlockSpacer(el, spacerHeight);
                            }
                            changed = true;
                            break;
                        }
                    }
                    if (!changed) break;
                }
                return document.body.scrollHeight;
            })();
        """.trimIndent()
        suspendCancellableCoroutine { continuation ->
            evaluateJavascript(script) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
        delay(80L)
    }

    private fun Int.toPdfPoints(): Int = (this * 72f / 1000f).toInt()
}
