package com.wkq.util.exif

import com.drew.imaging.ImageMetadataReader
import java.io.File

object ImageMetadataInspector {

    data class MetadataSummary(
        val exifCount: Int,
        val iptcCount: Int,
        val xmpCount: Int,
        val otherCount: Int
    ) {
        val totalCount: Int
            get() = exifCount + iptcCount + xmpCount + otherCount
    }

    fun inspect(file: File): Result<MetadataSummary> = runCatching {
        var exifCount = 0
        var iptcCount = 0
        var xmpCount = 0
        var otherCount = 0

        ImageMetadataReader.readMetadata(file).directories.forEach { directory ->
            val tagCount = directory.tags.count()
            when (directory.name.toMetadataGroup()) {
                MetadataGroup.EXIF -> exifCount += tagCount
                MetadataGroup.IPTC -> iptcCount += tagCount
                MetadataGroup.XMP -> xmpCount += tagCount
                MetadataGroup.OTHER -> otherCount += tagCount
            }
        }

        MetadataSummary(
            exifCount = exifCount,
            iptcCount = iptcCount,
            xmpCount = xmpCount,
            otherCount = otherCount
        )
    }

    fun countMetadataFields(file: File): Int {
        return inspect(file).getOrNull()?.totalCount ?: 0
    }

    private fun String.toMetadataGroup(): MetadataGroup {
        val normalized = lowercase()
        return when {
            "iptc" in normalized -> MetadataGroup.IPTC
            "xmp" in normalized -> MetadataGroup.XMP
            "exif" in normalized || "gps" in normalized || "makernote" in normalized -> MetadataGroup.EXIF
            else -> MetadataGroup.OTHER
        }
    }

    private enum class MetadataGroup {
        EXIF,
        IPTC,
        XMP,
        OTHER
    }
}
