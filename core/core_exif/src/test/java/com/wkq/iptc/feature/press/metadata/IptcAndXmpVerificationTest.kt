package com.wkq.iptc.feature.press.metadata

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.iptc.IptcDirectory
import com.wkq.iptc.feature.press.model.MetadataTemplate
import org.apache.commons.imaging.bytesource.ByteSource
import org.apache.commons.imaging.formats.jpeg.JpegImagingParameters
import org.apache.commons.imaging.formats.jpeg.iptc.JpegIptcRewriter
import org.apache.commons.imaging.formats.jpeg.iptc.PhotoshopApp13Data
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class IptcAndXmpVerificationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testFile: File

    // 最简合法的 JPEG 字节数组 (包含 SOI, APP0, JFIF 标志以及 EOI)
    private val minimalJpegBytes = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(), // SOI
        0xFF.toByte(), 0xE0.toByte(), 0x00.toByte(), 0x10.toByte(), // APP0
        0x4A.toByte(), 0x46.toByte(), 0x49.toByte(), 0x46.toByte(), 0x00.toByte(), // JFIF
        0x01.toByte(), 0x01.toByte(), 0x01.toByte(), 0x00.toByte(), 0x60.toByte(), 0x00.toByte(), 0x60.toByte(), 0x00.toByte(), 0x00.toByte(),
        0xFF.toByte(), 0xD9.toByte()  // EOI
    )

    @Before
    fun setUp() {
        testFile = tempFolder.newFile("test_write.jpg")
        testFile.writeBytes(minimalJpegBytes)
    }

    /**
     * 1. 验证“不选择模板，只填快捷 IPTC”组合。
     * 预期：只有快捷填写的字段写入，其他未填字段为空，不凭空写默认脏数据。
     */
    @Test
    fun testWriteOnlyQuickMetadata() {
        val quickTemplate = MetadataTemplate(
            id = "quick_test",
            displayName = "Quick IPTC",
            headline = "突发新闻现场",
            caption = "救援人员正在展开搜救",
            keywords = listOf("突发", "救援"),
            creator = "张记者",
            iptcAuthor = "张记者",
            enabledFields = listOf("headline", "caption", "keywords", "creator", "iptcAuthor")
        )

        // 1. 验证 IPTC 写入与读取 (纯 Java)
        IptcWriter.writeTemplateMetadata(testFile, quickTemplate).getOrThrow()

        val iptc = readIptc(testFile)
        assertNotNull(iptc)
        assertEquals("突发新闻现场", iptc!!.getString(IptcDirectory.TAG_HEADLINE))
        assertEquals("救援人员正在展开搜救", iptc.getString(IptcDirectory.TAG_CAPTION))
        assertArrayEquals(arrayOf("突发", "救援"), iptc.getStringArray(IptcDirectory.TAG_KEYWORDS))
        assertEquals("张记者", iptc.getString(IptcDirectory.TAG_BY_LINE))

        // 验证未填字段在 IPTC 中应全为空，没有默认脏数据（例如城市和国家没有被默认写入）
        assertNull(iptc.getString(IptcDirectory.TAG_CITY))
        assertNull(iptc.getString(IptcDirectory.TAG_COUNTRY_OR_PRIMARY_LOCATION_NAME))

        // 2. 验证 XMP 包生成 (纯 Java 内存校验)
        val xmpXml = XmpWriter.buildTemplatePacket(quickTemplate)
        val doc = parseXml(xmpXml)
        
        assertEquals("突发新闻现场", doc.firstText("photoshop:Headline"))
        assertEquals("救援人员正在展开搜救", doc.firstText("dc:description"))
        assertEquals("张记者", doc.firstText("dc:creator"))
        assertEquals("", doc.firstText("photoshop:City")) // 应该为空
    }

    /**
     * 2. 验证“选择模板，不填快捷字段”组合。
     * 预期：先用模板字段。
     */
    @Test
    fun testWriteOnlyTemplate() {
        val template = MetadataTemplate(
            id = "news_template",
            displayName = "新闻通用模板",
            copyright = "XX通讯社 版权所有",
            credit = "XX通讯社",
            source = "社报部",
            city = "郑州",
            country = "中国",
            isoCountryCode = "CN",
            enabledFields = listOf("copyright", "credit", "source", "city", "country", "isoCountryCode")
        )

        // 1. 验证 IPTC 写入
        IptcWriter.writeTemplateMetadata(testFile, template).getOrThrow()

        val iptc = readIptc(testFile)
        assertNotNull(iptc)
        assertEquals("XX通讯社 版权所有", iptc!!.getString(IptcDirectory.TAG_COPYRIGHT_NOTICE))
        assertEquals("XX通讯社", iptc.getString(IptcDirectory.TAG_CREDIT))
        assertEquals("社报部", iptc.getString(IptcDirectory.TAG_SOURCE))
        assertEquals("郑州", iptc.getString(IptcDirectory.TAG_CITY))
        assertEquals("中国", iptc.getString(IptcDirectory.TAG_COUNTRY_OR_PRIMARY_LOCATION_NAME))
        assertEquals("CN", iptc.getString(IptcDirectory.TAG_COUNTRY_OR_PRIMARY_LOCATION_CODE))

        // 模板未填的 Headline/Caption 在 IPTC 中应该为空
        assertNull(iptc.getString(IptcDirectory.TAG_HEADLINE))
        assertNull(iptc.getString(IptcDirectory.TAG_CAPTION))

        // 2. 验证 XMP 包生成
        val xmpXml = XmpWriter.buildTemplatePacket(template)
        val doc = parseXml(xmpXml)
        
        assertEquals("XX通讯社 版权所有", doc.firstText("dc:rights"))
        assertEquals("XX通讯社", doc.firstText("photoshop:Credit"))
        assertEquals("社报部", doc.firstText("photoshop:Source"))
        assertEquals("郑州", doc.firstText("photoshop:City"))
        assertEquals("中国", doc.firstText("photoshop:Country"))
        assertEquals("CN", doc.firstText("Iptc4xmpCore:CountryCode"))
        assertEquals("", doc.firstText("photoshop:Headline"))
    }

    /**
     * 3. 验证“选择模板，同时填快捷字段覆盖模板字段”组合。
     * 预期：快捷字段不为空时，覆盖模板同名字段；快捷字段为空时，不应该清掉模板字段。
     */
    @Test
    fun testWriteTemplateWithQuickOverrides() {
        val baseTemplate = MetadataTemplate(
            id = "sports_template",
            displayName = "体育预设",
            headline = "体育赛事",
            caption = "精彩瞬间记录",
            keywords = listOf("体育", "赛事"),
            credit = "新华社",
            source = "摄影部",
            copyright = "Copyright 2026",
            city = "杭州",
            country = "中国",
            isoCountryCode = "CN",
            enabledFields = listOf("headline", "caption", "keywords", "credit", "source", "copyright", "city", "country", "isoCountryCode")
        )

        // 模拟快捷字段输入：用户覆盖了 Headline、Caption 和 Keywords，但 credit/source/copyright/city 等保持空。
        val quickTemplate = MetadataTemplate(
            id = "quick_override",
            displayName = "Quick Input",
            headline = "羽毛球男单决赛",
            caption = "选手在比赛中腾空扣杀",
            keywords = listOf("羽毛球", "决赛", "扣杀"),
            enabledFields = listOf("headline", "caption", "keywords")
        )

        // 模拟合并
        val mergedTemplate = baseTemplate.copy(
            headline = quickTemplate.headline.ifBlank { baseTemplate.headline },
            caption = quickTemplate.caption.ifBlank { baseTemplate.caption },
            keywords = quickTemplate.keywords.ifEmpty { baseTemplate.keywords },
            credit = quickTemplate.credit.ifBlank { baseTemplate.credit },
            source = quickTemplate.source.ifBlank { baseTemplate.source },
            copyright = quickTemplate.copyright.ifBlank { baseTemplate.copyright },
            city = quickTemplate.city.ifBlank { baseTemplate.city },
            country = quickTemplate.country.ifBlank { baseTemplate.country },
            isoCountryCode = quickTemplate.isoCountryCode.ifBlank { baseTemplate.isoCountryCode },
            enabledFields = (baseTemplate.enabledFields + quickTemplate.enabledFields).distinct()
        )

        // 1. 验证 IPTC 写入
        IptcWriter.writeTemplateMetadata(testFile, mergedTemplate).getOrThrow()

        val iptc = readIptc(testFile)
        assertNotNull(iptc)
        // 被快捷覆盖了的字段，显示快捷的内容
        assertEquals("羽毛球男单决赛", iptc!!.getString(IptcDirectory.TAG_HEADLINE))
        assertEquals("选手在比赛中腾空扣杀", iptc.getString(IptcDirectory.TAG_CAPTION))
        assertArrayEquals(arrayOf("羽毛球", "决赛", "扣杀"), iptc.getStringArray(IptcDirectory.TAG_KEYWORDS))
        
        // 快捷没有填写的字段，保持模板内容不被清空
        assertEquals("新华社", iptc.getString(IptcDirectory.TAG_CREDIT))
        assertEquals("摄影部", iptc.getString(IptcDirectory.TAG_SOURCE))
        assertEquals("Copyright 2026", iptc.getString(IptcDirectory.TAG_COPYRIGHT_NOTICE))
        assertEquals("杭州", iptc.getString(IptcDirectory.TAG_CITY))
        assertEquals("中国", iptc.getString(IptcDirectory.TAG_COUNTRY_OR_PRIMARY_LOCATION_NAME))

        // 2. 验证 XMP 包生成
        val xmpXml = XmpWriter.buildTemplatePacket(mergedTemplate)
        val doc = parseXml(xmpXml)
        
        assertEquals("羽毛球男单决赛", doc.firstText("photoshop:Headline"))
        assertEquals("选手在比赛中腾空扣杀", doc.firstText("dc:description"))
        assertEquals("新华社", doc.firstText("photoshop:Credit"))
        assertEquals("摄影部", doc.firstText("photoshop:Source"))
        assertEquals("Copyright 2026", doc.firstText("dc:rights"))
        assertEquals("杭州", doc.firstText("photoshop:City"))
    }

    /**
     * 4. 验证清理元数据：物理清除 IPTC (APP13 段) 并在测试中彻底清除干净。
     */
    @Test
    fun testMetadataScrubbing() {
        val template = MetadataTemplate(
            id = "scrub_test_temp",
            displayName = "测试模板",
            headline = "需要被擦除的标题",
            caption = "需要被擦除的内容",
            keywords = listOf("擦除"),
            copyright = "版权信息",
            city = "杭州",
            enabledFields = listOf("headline", "caption", "keywords", "copyright", "city")
        )

        // 写入 IPTC
        IptcWriter.writeTemplateMetadata(testFile, template).getOrThrow()
        
        // 校验写入成功
        val beforeIptc = readIptc(testFile)
        assertNotNull(beforeIptc)
        assertEquals("需要被擦除的标题", beforeIptc!!.getString(IptcDirectory.TAG_HEADLINE))

        // 执行 IPTC 物理清理 (使用 MetadataScrubber 内部的重写逻辑)
        val byteSource = ByteSource.file(testFile)
        val metadata = org.apache.commons.imaging.formats.jpeg.JpegImageParser()
            .getPhotoshopMetadata(byteSource, JpegImagingParameters())
        val nonIptcBlocks = metadata?.photoshopApp13Data?.nonIptcBlocks.orEmpty()
        val emptyApp13Data = PhotoshopApp13Data(emptyList(), nonIptcBlocks)

        val tempFile = tempFolder.newFile("test_scrubbed.jpg")
        tempFile.outputStream().buffered().use { output ->
            JpegIptcRewriter().writeIptc(byteSource, output, emptyApp13Data)
        }

        // 校验物理清洗后的照片，核心内容字段完全被清空
        val afterIptc = readIptc(tempFile)
        if (afterIptc != null) {
            assertNull("Headline 应该被清空", afterIptc.getString(IptcDirectory.TAG_HEADLINE))
            assertNull("Caption 应该被清空", afterIptc.getString(IptcDirectory.TAG_CAPTION))
            assertNull("Copyright 应该被清空", afterIptc.getString(IptcDirectory.TAG_COPYRIGHT_NOTICE))
            assertNull("City 应该被清空", afterIptc.getString(IptcDirectory.TAG_CITY))
        }
    }

    // 辅助读取 IPTC 方法
    private fun readIptc(photoFile: File): IptcDirectory? {
        val metadata = ImageMetadataReader.readMetadata(photoFile)
        return metadata.getFirstDirectoryOfType(IptcDirectory::class.java)
    }

    // 解析 XML 树
    private fun parseXml(xml: String): Element {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
        }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        return document.documentElement
    }

    private fun Element.firstText(tagName: String): String {
        val nodes = getElementsByTagName(tagName)
        if (nodes.length == 0) return ""
        return nodes.item(0).textContent.trim()
    }
}
