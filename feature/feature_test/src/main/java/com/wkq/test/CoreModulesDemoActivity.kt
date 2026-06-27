package com.wkq.test

import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.wkq.base.activity.BaseActivity
import com.wkq.google.GoogleKitConfig
import com.wkq.iptc.camera.metadata.PhotoCryptographicSigner
import com.wkq.iptc.upload.SftpConfig
import com.wkq.iptc.upload.UploadProtocolType
import com.wkq.pdf.StructuredPdfDocument
import com.wkq.pdf.StructuredPdfRenderer
import com.wkq.pdf.StructuredPdfSection
import com.wkq.router.annotation.Route
import com.wkq.router.api.Router
import com.wkq.site.report.model.ReportData
import com.wkq.site.report.plan.ReportPlanPresets
import com.wkq.site.report.template.ReportTemplatePresets
import com.wkq.test.databinding.ActivityCoreModulesDemoBinding
import com.wkq.util.SpUtils
import com.wkq.util.exif.ImageMetadataInspector
import com.wkq.util.location.geo.util.GeoDistanceUtils
import java.io.File
import java.util.Locale

@Route(path = "/test/core_modules")
class CoreModulesDemoActivity : BaseActivity<ActivityCoreModulesDemoBinding>() {

    private val entries by lazy { createEntries() }

    override fun initView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = TestEntryAdapter(entries)
    }

    override fun initData() = Unit

    private fun createEntries(): List<TestEntry> {
        return listOf(
            TestEntry(
                group = "基础能力",
                icon = "B",
                title = "core_base",
                desc = "验证基础 Activity、Insets、通用 UI 基础能力"
            ) {
                Router.open("/test/core_base", this)
            },
            TestEntry(
                group = "基础能力",
                icon = "U",
                title = "core_util",
                desc = "验证 SP、缓存目录等通用工具能力"
            ) {
                showUtilDemo()
            },
            TestEntry(
                group = "媒体能力",
                icon = "C",
                title = "core_camera",
                desc = "验证相机签名数据模型和拍摄相关基础能力"
            ) {
                showCameraDemo()
            },
            TestEntry(
                group = "媒体能力",
                icon = "E",
                title = "core_exif",
                desc = "验证图片元数据读取和字段统计能力"
            ) {
                showExifDemo()
            },
            TestEntry(
                group = "平台能力",
                icon = "G",
                title = "core_google",
                desc = "验证 GoogleKit 配置对象和平台开关"
            ) {
                showGoogleDemo()
            },
            TestEntry(
                group = "平台能力",
                icon = "L",
                title = "core_location",
                desc = "验证地理距离计算等位置工具能力"
            ) {
                showLocationDemo()
            },
            TestEntry(
                group = "业务支撑",
                icon = "P",
                title = "core_pdf",
                desc = "生成结构化 PDF 文件并写入缓存目录"
            ) {
                showPdfDemo()
            },
            TestEntry(
                group = "业务支撑",
                icon = "R",
                title = "core_report",
                desc = "验证报告模板、预设计划和基础校验"
            ) {
                showReportDemo()
            },
            TestEntry(
                group = "业务支撑",
                icon = "S",
                title = "core_upload",
                desc = "验证 SFTP 上传配置和协议类型解析"
            ) {
                showUploadDemo()
            }
        )
    }

    private fun showUtilDemo() {
        SpUtils.init(application)
        SpUtils.put("core_demo_key", "core_util_ok")
        val value = SpUtils.getString("core_demo_key", "")
        showResult("core_util 可用\nSP: $value\nCache: ${cacheDir.absolutePath}")
    }

    private fun showCameraDemo() {
        val hash = PhotoCryptographicSigner.SignatureData(
            hash = "demo",
            latitude = 0.0,
            longitude = 0.0,
            timestamp = System.currentTimeMillis(),
            creator = "demo"
        )
        showResult("core_camera 可用\n签名数据模型: $hash")
    }

    private fun showExifDemo() {
        val fakeFile = File(cacheDir, "not_exists.jpg")
        val count = ImageMetadataInspector.countMetadataFields(fakeFile)
        showResult("core_exif 可用\n不存在图片的元数据数量兜底: $count")
    }

    private fun showGoogleDemo() {
        val config = GoogleKitConfig(
            enableAds = false,
            enableFirebaseAnalytics = false,
            appName = "Core Demo"
        )
        showResult("core_google 可用\nGoogleKitConfig: appName=${config.appName}, ads=${config.enableAds}")
    }

    private fun showLocationDemo() {
        val distance = GeoDistanceUtils.haversine(
            lat1 = 39.9042,
            lon1 = 116.4074,
            lat2 = 31.2304,
            lon2 = 121.4737
        )
        showResult("core_location 可用\n北京到上海约: ${String.format(Locale.US, "%.1f", distance)} km")
    }

    private fun showPdfDemo() {
        val outputFile = File(cacheDir, "core_pdf_demo.pdf")
        val document = StructuredPdfDocument(
            title = "Core PDF Demo",
            subtitle = "Generated by core_pdf",
            sections = listOf(
                StructuredPdfSection(
                    title = "Result",
                    paragraphs = listOf("PDF renderer works in ApplicationArchitecture.")
                )
            ),
            footer = "Demo only"
        )
        StructuredPdfRenderer(this).renderToFile(document, outputFile)
        showResult("core_pdf 可用\n已生成: ${outputFile.absolutePath}\n大小: ${outputFile.length()} bytes")
    }

    private fun showReportDemo() {
        val templates = ReportTemplatePresets.all
        val result = ReportPlanPresets.validate(
            ReportData(
                id = "demo",
                title = "Core Report Demo",
                templateId = templates.firstOrNull()?.template?.id ?: ReportData.DEFAULT_TEMPLATE_ID
            )
        )
        showResult("core_report 可用\n模板数量: ${templates.size}\n基础校验: ${result.valid}")
    }

    private fun showUploadDemo() {
        val config = SftpConfig(host = "example.com", username = "demo")
        val protocol = UploadProtocolType.fromValue("sftp")
        showResult("core_upload 可用\n协议: $protocol\n示例配置: ${config.host}:${config.port}/${config.remoteDir}")
    }

    private fun showResult(message: String) {
        binding.tvResult.text = message
        Toast.makeText(this, message.lineSequence().firstOrNull().orEmpty(), Toast.LENGTH_SHORT).show()
    }
}
