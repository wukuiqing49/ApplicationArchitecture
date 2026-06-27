package com.wkq.iptc.feature.press.metadata

import androidx.exifinterface.media.ExifInterface
import com.wkq.iptc.feature.press.model.MetadataFieldDefinitions
import com.wkq.iptc.feature.press.model.MetadataStandard
import com.wkq.iptc.feature.press.model.MetadataTemplate
import org.apache.commons.imaging.bytesource.ByteSource
import org.apache.commons.imaging.formats.jpeg.JpegImageParser
import org.apache.commons.imaging.formats.jpeg.JpegImagingParameters
import org.apache.commons.imaging.formats.jpeg.iptc.IptcTypes
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File

object MetadataTemplateImporter {

    data class ImportResult(
        val template: MetadataTemplate,
        val summary: ImportSummary
    )

    data class ImportSummary(
        val commonFieldCount: Int,
        val standardCounts: Map<MetadataStandard, Int>,
        val singlePhotoFieldCount: Int
    ) {
        val standardFieldCount: Int = standardCounts.values.sum()
        val totalFieldCount: Int = commonFieldCount + standardFieldCount
    }

    fun importFromPhoto(photoFile: File, displayName: String): Result<ImportResult> {
        return runCatching {
            val builder = TemplateBuilder(displayName)
            readIptc(photoFile, builder)
            readXmp(photoFile, builder)
            builder.build()
        }
    }

    private fun readIptc(photoFile: File, builder: TemplateBuilder) {
        runCatching {
            val metadata = JpegImageParser().getPhotoshopMetadata(
                ByteSource.file(photoFile),
                JpegImagingParameters()
            )
            metadata?.photoshopApp13Data?.records.orEmpty()
        }.getOrDefault(emptyList()).forEach { record ->
            val value = record.value.trim()
            if (value.isBlank()) return@forEach
            when (record.iptcType) {
                IptcTypes.RECORD_VERSION -> builder.standard(MetadataFieldDefinitions.IIM_RECORD_VERSION, value)
                IptcTypes.OBJECT_TYPE_REFERENCE -> builder.standard(MetadataFieldDefinitions.IIM_OBJECT_TYPE, value)
                IptcTypes.OBJECT_ATTRIBUTE_REFERENCE -> builder.standard(MetadataFieldDefinitions.IIM_OBJECT_ATTRIBUTE, value)
                IptcTypes.OBJECT_NAME -> {
                    builder.standard(MetadataFieldDefinitions.IIM_OBJECT_NAME, value)
                }
                IptcTypes.EDIT_STATUS -> builder.standard(MetadataFieldDefinitions.IIM_EDIT_STATUS, value)
                IptcTypes.URGENCY -> builder.standard(MetadataFieldDefinitions.IIM_URGENCY, value)
                IptcTypes.CATEGORY -> {
                    builder.common("category", value)
                    builder.standard(MetadataFieldDefinitions.IIM_CATEGORY, value)
                }
                IptcTypes.SUPPLEMENTAL_CATEGORY -> builder.appendStandard(MetadataFieldDefinitions.IIM_SUPPLEMENTAL_CATEGORY, value)
                IptcTypes.KEYWORDS -> {
                    builder.keyword(value)
                    builder.appendStandard(MetadataFieldDefinitions.IIM_KEYWORDS, value)
                }
                IptcTypes.SPECIAL_INSTRUCTIONS -> builder.standard(MetadataFieldDefinitions.IIM_SPECIAL_INSTRUCTIONS, value)
                IptcTypes.DATE_CREATED -> builder.standard(MetadataFieldDefinitions.IIM_DATE_CREATED, value)
                IptcTypes.TIME_CREATED -> builder.standard(MetadataFieldDefinitions.IIM_TIME_CREATED, value)
                IptcTypes.BYLINE -> {
                    builder.common("iptcAuthor", value)
                    builder.common("creator", value)
                    builder.standard(MetadataFieldDefinitions.IIM_BYLINE, value)
                }
                IptcTypes.BYLINE_TITLE -> builder.standard(MetadataFieldDefinitions.IIM_BYLINE_TITLE, value)
                IptcTypes.CITY -> {
                    builder.common("city", value)
                    builder.standard(MetadataFieldDefinitions.IIM_CITY, value)
                }
                IptcTypes.SUBLOCATION -> builder.standard(MetadataFieldDefinitions.IIM_SUBLOCATION, value)
                IptcTypes.PROVINCE_STATE -> {
                    builder.common("state", value)
                    builder.standard(MetadataFieldDefinitions.IIM_STATE, value)
                }
                IptcTypes.COUNTRY_PRIMARY_LOCATION_CODE -> {
                    builder.common("isoCountryCode", value)
                    builder.standard(MetadataFieldDefinitions.IIM_COUNTRY_CODE, value)
                }
                IptcTypes.COUNTRY_PRIMARY_LOCATION_NAME -> {
                    builder.common("country", value)
                    builder.standard(MetadataFieldDefinitions.IIM_COUNTRY_NAME, value)
                }
                IptcTypes.HEADLINE -> {
                    builder.standard(MetadataFieldDefinitions.IIM_HEADLINE, value)
                }
                IptcTypes.CREDIT -> {
                    builder.common("credit", value)
                    builder.standard(MetadataFieldDefinitions.IIM_CREDIT, value)
                }
                IptcTypes.SOURCE -> {
                    builder.common("source", value)
                    builder.standard(MetadataFieldDefinitions.IIM_SOURCE, value)
                }
                IptcTypes.COPYRIGHT_NOTICE -> {
                    builder.common("copyright", value)
                    builder.standard(MetadataFieldDefinitions.IIM_COPYRIGHT, value)
                }
                IptcTypes.CONTACT -> builder.standard(MetadataFieldDefinitions.IIM_CONTACT, value)
                IptcTypes.CAPTION_ABSTRACT -> {
                    builder.standard(MetadataFieldDefinitions.IIM_CAPTION, value)
                }
                IptcTypes.WRITER_EDITOR -> {
                    builder.common("writer", value)
                    builder.standard(MetadataFieldDefinitions.IIM_WRITER, value)
                }
                else -> Unit
            }
        }
    }

    private fun readXmp(photoFile: File, builder: TemplateBuilder) {
        val xmp = runCatching { ExifInterface(photoFile).getAttribute(ExifInterface.TAG_XMP) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: return
        val leaves = parseXmpLeaves(xmp)

        builder.xmpAlt(leaves, MetadataFieldDefinitions.DC_TITLE, "dc:title")
        builder.xmpSeqOrText(leaves, MetadataFieldDefinitions.DC_CREATOR, "dc:creator").also {
            builder.common("creator", it)
            builder.common("iptcAuthor", it)
        }
        builder.xmpAlt(leaves, MetadataFieldDefinitions.DC_DESCRIPTION, "dc:description")
        builder.xmpAlt(leaves, MetadataFieldDefinitions.DC_RIGHTS, "dc:rights").also { builder.common("copyright", it) }
        builder.xmpText(leaves, MetadataFieldDefinitions.DC_SOURCE, "dc:source").also { builder.common("source", it) }
        builder.xmpList(leaves, MetadataFieldDefinitions.DC_SUBJECT, "dc:subject")
            .forEach { builder.keyword(it) }
        builder.xmpText(leaves, MetadataFieldDefinitions.DC_DATE, "dc:date")

        builder.xmpText(leaves, MetadataFieldDefinitions.PHOTOSHOP_HEADLINE, "photoshop:Headline")
        builder.xmpText(leaves, MetadataFieldDefinitions.PHOTOSHOP_CREDIT, "photoshop:Credit").also { builder.common("credit", it) }
        builder.xmpText(leaves, MetadataFieldDefinitions.PHOTOSHOP_SOURCE, "photoshop:Source").also { builder.common("source", it) }
        builder.xmpText(leaves, MetadataFieldDefinitions.PHOTOSHOP_CITY, "photoshop:City").also { builder.common("city", it) }
        builder.xmpText(leaves, MetadataFieldDefinitions.PHOTOSHOP_STATE, "photoshop:State").also { builder.common("state", it) }
        builder.xmpText(leaves, MetadataFieldDefinitions.PHOTOSHOP_COUNTRY, "photoshop:Country").also { builder.common("country", it) }
        builder.xmpText(leaves, MetadataFieldDefinitions.PHOTOSHOP_DATE_CREATED, "photoshop:DateCreated")

        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_CORE_LOCATION, "Iptc4xmpCore:Location").also { builder.common("location", it) }
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_CORE_COUNTRY_CODE, "Iptc4xmpCore:CountryCode").also { builder.common("isoCountryCode", it) }
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_CORE_INTELLECTUAL_GENRE, "Iptc4xmpCore:IntellectualGenre")
        builder.xmpList(leaves, MetadataFieldDefinitions.IPTC_CORE_SCENE, "Iptc4xmpCore:Scene")
        builder.xmpList(leaves, MetadataFieldDefinitions.IPTC_CORE_SUBJECT_CODE, "Iptc4xmpCore:SubjectCode")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_CORE_CREATOR_CITY, "Iptc4xmpCore:CiAdrCity")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_CORE_CREATOR_REGION, "Iptc4xmpCore:CiAdrRegion")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_CORE_CREATOR_COUNTRY, "Iptc4xmpCore:CiAdrCtry")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_CORE_CREATOR_COUNTRY_CODE, "Iptc4xmpCore:CiAdrCtryCode")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_CORE_CREATOR_ADDRESS, "Iptc4xmpCore:CiAdrExtadr")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_CORE_CREATOR_EMAIL, "Iptc4xmpCore:CiEmailWork")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_CORE_CREATOR_PHONE, "Iptc4xmpCore:CiTelWork")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_CORE_CREATOR_WEB_URL, "Iptc4xmpCore:CiUrlWork")

        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_EXT_EVENT, "Iptc4xmpExt:Event")
        builder.xmpList(leaves, MetadataFieldDefinitions.IPTC_EXT_PERSON_IN_IMAGE, "Iptc4xmpExt:PersonInImage")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_COUNTRY_CODE, "Iptc4xmpExt:CountryCode")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_COUNTRY_NAME, "Iptc4xmpExt:CountryName")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_REGION, "Iptc4xmpExt:ProvinceState")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_CITY, "Iptc4xmpExt:City")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_SUBLOCATION, "Iptc4xmpExt:Sublocation")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_EXT_ARTWORK_TITLE, "Iptc4xmpExt:AOTitle")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_EXT_ARTWORK_CREATOR, "Iptc4xmpExt:AOCreator")
        builder.xmpText(leaves, MetadataFieldDefinitions.IPTC_EXT_ARTWORK_SOURCE, "Iptc4xmpExt:AOSource")
    }

    private fun parseXmpLeaves(xmp: String): Map<String, List<String>> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(xmp.reader())
        val path = mutableListOf<String>()
        val leaves = linkedMapOf<String, MutableList<String>>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> path += parser.qualifiedName()
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotBlank() && path.isNotEmpty()) {
                        leaves.getOrPut(path.joinToString("/")) { mutableListOf() } += text
                    }
                }
                XmlPullParser.END_TAG -> if (path.isNotEmpty()) path.removeAt(path.lastIndex)
            }
            event = parser.next()
        }
        return leaves
    }

    private fun XmlPullParser.qualifiedName(): String {
        val prefix = prefix
        return if (prefix.isNullOrBlank()) name else "$prefix:$name"
    }

    private class TemplateBuilder(private val displayName: String) {
        private val common = linkedMapOf<String, String>()
        private val standard = linkedMapOf<String, String>()
        private val keywords = linkedSetOf<String>()
        private val enabled = linkedSetOf<String>()
        private val singlePhotoStandardFields = linkedSetOf<String>()

        fun common(key: String, value: String) {
            if (value.isBlank() || common[key].isNullOrBlank().not()) return
            common[key] = value
            enabled += key
        }

        fun keyword(value: String) {
            if (value.isBlank()) return
            keywords += value
            enabled += "keywords"
        }

        fun standard(key: String, value: String) {
            if (value.isBlank() || standard[key].isNullOrBlank().not()) return
            standard[key] = value
            enabled += key
            if (key in SINGLE_PHOTO_STANDARD_KEYS) {
                singlePhotoStandardFields += key
            }
        }

        fun appendStandard(key: String, value: String) {
            if (value.isBlank()) return
            val old = standard[key].orEmpty()
            val parts = old.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            if (!parts.contains(value)) parts += value
            standard[key] = parts.joinToString(", ")
            enabled += key
            if (key in SINGLE_PHOTO_STANDARD_KEYS) {
                singlePhotoStandardFields += key
            }
        }

        fun xmpText(leaves: Map<String, List<String>>, key: String, pathSuffix: String): String {
            val value = leaves.findFirst(pathSuffix)
            standard(key, value)
            return value
        }

        fun xmpAlt(leaves: Map<String, List<String>>, key: String, pathPrefix: String): String {
            val value = leaves.findFirst("$pathPrefix/rdf:Alt/rdf:li", "$pathPrefix/Alt/li", pathPrefix)
            standard(key, value)
            return value
        }

        fun xmpSeqOrText(leaves: Map<String, List<String>>, key: String, pathPrefix: String): String {
            val value = leaves.findFirst("$pathPrefix/rdf:Seq/rdf:li", "$pathPrefix/Seq/li", pathPrefix)
            standard(key, value)
            return value
        }

        fun xmpList(leaves: Map<String, List<String>>, key: String, pathPrefix: String): List<String> {
            val values = leaves.findAll("$pathPrefix/rdf:Bag/rdf:li", "$pathPrefix/rdf:Seq/rdf:li", "$pathPrefix/Bag/li", "$pathPrefix/Seq/li", pathPrefix)
            values.forEach { appendStandard(key, it) }
            return values
        }

        fun build(): ImportResult {
            val template = MetadataTemplate(
                id = "",
                displayName = displayName,
                templateType = "IPTC",
                creator = common["creator"].orEmpty(),
                title = common["title"].orEmpty(),
                iptcAuthor = common["iptcAuthor"].orEmpty(),
                writer = common["writer"].orEmpty(),
                category = common["category"].orEmpty(),
                isoCountryCode = common["isoCountryCode"].orEmpty(),
                credit = common["credit"].orEmpty(),
                source = common["source"].orEmpty(),
                copyright = common["copyright"].orEmpty(),
                country = common["country"].orEmpty(),
                city = common["city"].orEmpty(),
                state = common["state"].orEmpty(),
                location = common["location"].orEmpty(),
                headline = common["headline"].orEmpty(),
                caption = common["caption"].orEmpty(),
                keywords = keywords.toList(),
                enabledFields = enabled.toList(),
                standardFields = standard,
                visibleStandardFields = standard.keys.toList()
            )
            val standardCounts = standard.keys
                .mapNotNull { MetadataFieldDefinitions.byKey[it]?.standard }
                .groupingBy { it }
                .eachCount()
            return ImportResult(
                template = template,
                summary = ImportSummary(
                    commonFieldCount = common.size + if (keywords.isNotEmpty()) 1 else 0,
                    standardCounts = standardCounts,
                    singlePhotoFieldCount = singlePhotoStandardFields.size
                )
            )
        }
    }

    private val SINGLE_PHOTO_STANDARD_KEYS = setOf(
        MetadataFieldDefinitions.IIM_OBJECT_NAME,
        MetadataFieldDefinitions.IIM_SPECIAL_INSTRUCTIONS,
        MetadataFieldDefinitions.IIM_DATE_CREATED,
        MetadataFieldDefinitions.IIM_TIME_CREATED,
        MetadataFieldDefinitions.IIM_HEADLINE,
        MetadataFieldDefinitions.IIM_CAPTION,
        MetadataFieldDefinitions.DC_TITLE,
        MetadataFieldDefinitions.DC_DESCRIPTION,
        MetadataFieldDefinitions.DC_DATE,
        MetadataFieldDefinitions.PHOTOSHOP_HEADLINE,
        MetadataFieldDefinitions.PHOTOSHOP_DATE_CREATED,
        MetadataFieldDefinitions.IPTC_EXT_EVENT,
        MetadataFieldDefinitions.IPTC_EXT_PERSON_IN_IMAGE,
        MetadataFieldDefinitions.IPTC_EXT_ARTWORK_TITLE,
        MetadataFieldDefinitions.IPTC_EXT_ARTWORK_CREATOR,
        MetadataFieldDefinitions.IPTC_EXT_ARTWORK_SOURCE
    )

    private fun Map<String, List<String>>.findFirst(vararg suffixes: String): String {
        return findAll(*suffixes).firstOrNull().orEmpty()
    }

    private fun Map<String, List<String>>.findAll(vararg suffixes: String): List<String> {
        return entries
            .filter { entry -> suffixes.any { entry.key.endsWith(it) } }
            .flatMap { it.value }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}
