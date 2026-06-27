package com.wkq.util.exif

import android.location.Location
import androidx.exifinterface.media.ExifInterface
import com.wkq.iptc.camera.metadata.ExifWriter
import com.wkq.iptc.feature.press.metadata.IptcWriter
import com.wkq.iptc.feature.press.metadata.XmpWriter
import com.wkq.iptc.feature.press.model.MetadataTemplate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.Ignore
import java.io.File

class MetadataScrubberTest {

    private val testFile = File("src/test/resources/test.jpg")

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
        testFile.parentFile?.mkdirs()
        testFile.writeBytes(minimalJpegBytes)
    }

    @Test
    @Ignore("由于本地 JVM 单元测试环境缺乏真实 Android 运行环境，ExifInterface 内部的 android.util.Pair 会因为 returnDefaultValues 返回 null 导致 guess.first NPE。本集成测试需要真机、模拟器或 Robolectric 环境。")
    fun testScrubMetadataKeepOrientation() {
        runBlocking {
        // 1. 准备 mock 的 MetadataTemplate 
        val template = MetadataTemplate(
            id = "test_scrub_template",
            displayName = "测试模版",
            creator = "小明",
            title = "测试标题",
            copyright = "CopyRight 2026",
            country = "中国",
            state = "河南省",
            city = "郑州市",
            location = "郑州东站",
            headline = "测试副标题",
            caption = "这是测试描述",
            keywords = listOf("测试", "Scrub", "IPTC")
        )

        // 2. 先写入 IPTC
        IptcWriter.writeTemplateMetadata(testFile, template).getOrThrow()

        // 3. 写入 XMP 并创建基础 EXIF
        val xmpPacket = XmpWriter.buildTemplatePacket(template)
        ExifWriter.writeAllMetadata(testFile, null, xmpPacket).getOrThrow()

        // 4. 手动写入 GPS 与 Orientation，避免引入 Location 类
        val exifSetter = ExifInterface(testFile)
        exifSetter.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
        exifSetter.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "34/1,44/1,47/1")
        exifSetter.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
        exifSetter.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "113/1,37/1,31/1")
        exifSetter.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
        exifSetter.saveAttributes()

        // 5. 校验擦除前元数据存在
        val beforeExif = ExifInterface(testFile)
        assertEquals(ExifInterface.ORIENTATION_ROTATE_90.toString(), beforeExif.getAttribute(ExifInterface.TAG_ORIENTATION))
        assertNotNull(beforeExif.latLong)
        assertNotNull(beforeExif.getAttribute(ExifInterface.TAG_XMP))

        // 6. 核心：执行安全擦除
        val result = MetadataScrubber.scrubMetadata(testFile)
        assertTrue(result.isSuccess)

        // 7. 断言擦除后的属性状态
        val afterExif = ExifInterface(testFile)
        
        // 旋转方向必须完美保留
        assertEquals(ExifInterface.ORIENTATION_ROTATE_90.toString(), afterExif.getAttribute(ExifInterface.TAG_ORIENTATION))
        
        // 经纬度和海拔信息必须全空
        assertNull(afterExif.latLong)
        assertEquals(0.0, afterExif.getAltitude(0.0), 0.0001)

        // 时间、厂商等敏感 EXIF 标签应被擦除
        assertNull(afterExif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
        assertNull(afterExif.getAttribute(ExifInterface.TAG_MAKE))

        // XMP 应被剔除
        assertNull(afterExif.getAttribute(ExifInterface.TAG_XMP))

        // 验证 IPTC 段是否也已被清洗 (解析 PhotoshopMetadata 应为空记录)
        val byteSource = org.apache.commons.imaging.bytesource.ByteSource.file(testFile)
        val metadata = org.apache.commons.imaging.formats.jpeg.JpegImageParser()
            .getPhotoshopMetadata(byteSource, org.apache.commons.imaging.formats.jpeg.JpegImagingParameters())
        val records = metadata?.photoshopApp13Data?.records.orEmpty()
        assertTrue(records.isEmpty())

        // 清理测试临时文件
        testFile.delete()
        }
    }
}
