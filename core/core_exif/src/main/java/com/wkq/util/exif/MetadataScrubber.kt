package com.wkq.util.exif

import androidx.exifinterface.media.ExifInterface
import org.apache.commons.imaging.bytesource.ByteSource
import org.apache.commons.imaging.formats.jpeg.JpegImagingParameters
import org.apache.commons.imaging.formats.jpeg.iptc.JpegIptcRewriter
import org.apache.commons.imaging.formats.jpeg.iptc.PhotoshopApp13Data
import java.io.File

object MetadataScrubber {

    /**
     * 统计照片中包含的元数据字段数量 (EXIF + IPTC + XMP)。
     * 优先使用 ImageMetadataInspector（metadata-extractor 库，覆盖最全）；
     * 降级时使用扩展后的全字段白名单兜底统计。
     */
    fun countMetadataFields(photoFile: File): Int {
        ImageMetadataInspector.countMetadataFields(photoFile).takeIf { it > 0 }?.let {
            return it
        }

        var count = 0
        runCatching {
            val exif = ExifInterface(photoFile)
            // 与 scrubMetadata 保持一致的完整字段白名单
            val allTrackedTags = GPS_TAGS + EXIF_TECH_TAGS + PERSONAL_TAGS + listOf(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.TAG_XMP
            )
            allTrackedTags.forEach { tag ->
                if (exif.getAttribute(tag) != null) count++
            }
        }
        runCatching {
            val byteSource = ByteSource.file(photoFile)
            val metadata = org.apache.commons.imaging.formats.jpeg.JpegImageParser()
                .getPhotoshopMetadata(byteSource, JpegImagingParameters())
            count += metadata?.photoshopApp13Data?.records.orEmpty().size
        }
        return count
    }

    fun countSelectedMetadataFields(
        photoFile: File,
        scrubIptc: Boolean,
        scrubExif: Boolean,
        scrubPersonal: Boolean
    ): Int {
        var count = 0
        runCatching {
            val exif = ExifInterface(photoFile)
            val trackedTags = buildList {
                if (scrubIptc || scrubPersonal) add(ExifInterface.TAG_XMP)
                if (scrubExif) {
                    addAll(GPS_TAGS)
                    addAll(EXIF_TECH_TAGS)
                }
                if (scrubPersonal) addAll(PERSONAL_TAGS)
            }.distinct()
            trackedTags.forEach { tag ->
                if (exif.getAttribute(tag) != null) count++
            }
        }
        if (scrubIptc) {
            runCatching {
                val byteSource = ByteSource.file(photoFile)
                val metadata = org.apache.commons.imaging.formats.jpeg.JpegImageParser()
                    .getPhotoshopMetadata(byteSource, JpegImagingParameters())
                count += metadata?.photoshopApp13Data?.records.orEmpty().size
            }
        }
        return count
    }

    /**
     * 兼容原先无参数的擦除调用：默认擦除所有 IPTC、EXIF、个人信息，并保留旋转方向。
     */
    suspend fun scrubMetadata(photoFile: File): Result<Unit> {
        return scrubMetadata(
            photoFile = photoFile,
            scrubIptc = true,
            scrubExif = true,
            scrubPersonal = true,
            keepOrientation = true
        )
    }

    /**
     * 支持选择性擦除敏感元数据，且支持选择是否保留旋转方向 (Orientation)。
     *
     * - scrubIptc    : 清除 APP13 IPTC 块 + XMP（包含 IPTC Core/Extension、Dublin Core、Photoshop）
     * - scrubExif    : 清除 GPS 全字段 + 相机曝光/光学/场景等技术参数
     * - scrubPersonal: 清除拍摄时间、设备型号/序列号、创作者/版权、唯一图像 ID
     */
    suspend fun scrubMetadata(
        photoFile: File,
        scrubIptc: Boolean,
        scrubExif: Boolean,
        scrubPersonal: Boolean,
        keepOrientation: Boolean = true
    ): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            // 释放可能存在的文件句柄占用
            System.gc()

            // 1. 暂存原图的旋转方向
            val sourceExif = ExifInterface(photoFile)
            val orientation = sourceExif.getAttribute(ExifInterface.TAG_ORIENTATION)
                ?: ExifInterface.ORIENTATION_NORMAL.toString()

            // 2. 用 ExifInterface 擦除 EXIF 和 XMP 字段并保存
            val targetExif = ExifInterface(photoFile)
            val sensitiveTags = mutableListOf<String>()

            if (scrubIptc) {
                sensitiveTags += ExifInterface.TAG_XMP
            }

            if (scrubExif) {
                sensitiveTags.addAll(GPS_TAGS)
                sensitiveTags.addAll(EXIF_TECH_TAGS)
            }

            if (scrubPersonal) {
                sensitiveTags.addAll(PERSONAL_TAGS)
                sensitiveTags += ExifInterface.TAG_XMP
            }

            sensitiveTags.distinct().forEach { tag ->
                targetExif.setAttribute(tag, null)
            }

            if (keepOrientation) {
                targetExif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation)
            }
            targetExif.saveAttributes()

            // 3. 最后一步：物理抹除 IPTC (APP13 区) 记录
            // 必须在 ExifInterface 保存后进行，避免其 saveAttributes 时将内存中的旧 APP13 覆写回去
            if (scrubIptc && CleanJpegExporter.isJpeg(photoFile)) {
                val imageBytes = photoFile.readBytes()
                val byteSource = ByteSource.array(imageBytes)
                val metadata = org.apache.commons.imaging.formats.jpeg.JpegImageParser()
                    .getPhotoshopMetadata(byteSource, JpegImagingParameters())
                val nonIptcBlocks = metadata?.photoshopApp13Data?.nonIptcBlocks.orEmpty()
                val emptyApp13Data = PhotoshopApp13Data(emptyList(), nonIptcBlocks)

                val updatedBytes = java.io.ByteArrayOutputStream().use { output ->
                    JpegIptcRewriter().writeIptc(byteSource, output, emptyApp13Data)
                    output.toByteArray()
                }
                photoFile.writeBytes(updatedBytes)
            }
        }
    }

    // -------------------------------------------------------------------------
    // 字段分组常量（与 UI 勾选项一一对应）
    // -------------------------------------------------------------------------

    /** GPS 定位全字段：经纬度、高度、速度、方向、目标点等 */
    private val GPS_TAGS = listOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
        ExifInterface.TAG_GPS_DEST_BEARING,
        ExifInterface.TAG_GPS_DEST_BEARING_REF,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_MEASURE_MODE,
        ExifInterface.TAG_GPS_DOP
    )

    /** 相机技术参数：曝光、光学、场景等（不含 GPS） */
    private val EXIF_TECH_TAGS = listOf(
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
        ExifInterface.TAG_APERTURE_VALUE,
        ExifInterface.TAG_SHUTTER_SPEED_VALUE,
        ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
        ExifInterface.TAG_MAX_APERTURE_VALUE,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_METERING_MODE,
        ExifInterface.TAG_EXPOSURE_PROGRAM,
        ExifInterface.TAG_SCENE_TYPE,
        ExifInterface.TAG_SCENE_CAPTURE_TYPE,
        ExifInterface.TAG_LIGHT_SOURCE,
        ExifInterface.TAG_SUBJECT_DISTANCE,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_LENS_SPECIFICATION
    )

    /** 个人身份信息：创作者、设备序列号、拍摄时间、唯一图像 ID 等 */
    private val PERSONAL_TAGS = listOf(
        // 拍摄时间（含亚秒级时间戳）
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
        // 设备制造商、型号、软件与序列号
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_SERIAL_NUMBER,
        ExifInterface.TAG_CAMERA_OWNER_NAME,
        // 创作者与版权
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        // 注释
        ExifInterface.TAG_USER_COMMENT,
        // 可追踪的唯一图像 ID（相机内生成，可用于追踪拍摄者）
        ExifInterface.TAG_IMAGE_UNIQUE_ID
    )
}
