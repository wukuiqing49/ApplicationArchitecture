package com.wkq.iptc.feature.press.metadata

import androidx.exifinterface.media.ExifInterface
import com.wkq.iptc.feature.press.model.MetadataFieldDefinitions
import com.wkq.iptc.feature.press.model.MetadataTemplate
import org.apache.commons.imaging.bytesource.ByteSource
import org.apache.commons.imaging.formats.jpeg.JpegImageParser
import org.apache.commons.imaging.formats.jpeg.JpegImagingParameters
import org.apache.commons.imaging.formats.jpeg.iptc.IptcType
import org.apache.commons.imaging.formats.jpeg.iptc.IptcTypes
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

object MetadataVerifier {
    fun verify(photoFile: File, template: MetadataTemplate, expectGps: Boolean): MetadataVerificationResult {
        val exif = runCatching { ExifInterface(photoFile) }.getOrNull()
        val xmp = exif?.getAttribute(ExifInterface.TAG_XMP).orEmpty()
        val exifVerified = exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.isNotBlank() == true &&
            (!expectGps || exif.latLong != null)
        val iptcVerified = verifyIptc(photoFile, template)
        val xmpVerified = verifyXmp(xmp, template)
        val error = if (exifVerified && iptcVerified && xmpVerified) null else "Metadata verification failed"
        return MetadataVerificationResult(exifVerified, iptcVerified, xmpVerified, error)
    }

    private fun verifyIptc(photoFile: File, template: MetadataTemplate): Boolean {
        return runCatching {
            val byteSource = ByteSource.file(photoFile)
            val metadata = JpegImageParser().getPhotoshopMetadata(byteSource, JpegImagingParameters())
            val records = metadata?.photoshopApp13Data?.records.orEmpty()

            fun hasRecord(type: IptcType, expectedValue: String): Boolean {
                val cleanValue = expectedValue.trim()
                if (cleanValue.isBlank()) return true
                return records.any { it.iptcType.type == type.type && it.value.trim() == cleanValue }
            }

            val coreChecks = listOf(
                IptcTypes.OBJECT_NAME to enabledValue(template, "title", template.title.ifBlank { template.headline }),
                IptcTypes.HEADLINE to enabledValue(template, "headline", template.headline),
                IptcTypes.CAPTION_ABSTRACT to enabledValue(template, "caption", template.caption),
                IptcTypes.BYLINE to enabledValue(template, "iptcAuthor", template.iptcAuthor.ifBlank { template.creator }),
                IptcTypes.WRITER_EDITOR to enabledValue(template, "writer", template.writer),
                IptcTypes.CATEGORY to enabledValue(template, "category", template.category),
                IptcTypes.CITY to enabledValue(template, "city", template.city),
                IptcTypes.PROVINCE_STATE to enabledValue(template, "state", template.state),
                IptcTypes.COUNTRY_PRIMARY_LOCATION_NAME to enabledValue(template, "country", template.country),
                IptcTypes.COUNTRY_PRIMARY_LOCATION_CODE to enabledValue(template, "isoCountryCode", template.isoCountryCode),
                IptcTypes.CREDIT to enabledValue(template, "credit", template.credit),
                IptcTypes.SOURCE to enabledValue(template, "source", template.source),
                IptcTypes.COPYRIGHT_NOTICE to enabledValue(template, "copyright", template.copyright)
            )

            val standardChecks = template.standardFields
                .mapNotNull { (field, value) -> IIM_FIELD_TYPES[field]?.let { it to value } }

            val keywordsVerified = if (template.isFieldEnabled("keywords") && template.keywords.isNotEmpty()) {
                template.keywords.all { keyword -> hasRecord(IptcTypes.KEYWORDS, keyword) }
            } else {
                true
            }
            val standardKeywordsVerified = template.standardFields[MetadataFieldDefinitions.IIM_KEYWORDS]
                .orEmpty()
                .split(",", "\uFF0C", ";", "\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .all { keyword -> hasRecord(IptcTypes.KEYWORDS, keyword) }

            keywordsVerified &&
                standardKeywordsVerified &&
                coreChecks.all { (type, value) -> hasRecord(type, value) } &&
                standardChecks.all { (type, value) -> hasRecord(type, value) }
        }.getOrElse { false }
    }

    private fun verifyXmp(xmp: String, template: MetadataTemplate): Boolean {
        if (xmp.isBlank()) return false
        return runCatching {
            val root = parseXml(xmp)
            val templateMarkerVerified = template.id.isBlank() || xmp.contains(template.id)
            val coreChecks = listOf(
                "dc:title" to enabledValue(template, "title", template.title.ifBlank { template.headline }),
                "photoshop:Headline" to enabledValue(template, "headline", template.headline),
                "dc:description" to enabledValue(template, "caption", template.caption),
                "dc:creator" to enabledValue(template, "iptcAuthor", template.iptcAuthor.ifBlank { template.creator }),
                "photoshop:CaptionWriter" to enabledValue(template, "writer", template.writer),
                "photoshop:Credit" to enabledValue(template, "credit", template.credit),
                "photoshop:Source" to enabledValue(template, "source", template.source),
                "dc:rights" to enabledValue(template, "copyright", template.copyright),
                "photoshop:City" to enabledValue(template, "city", template.city),
                "photoshop:State" to enabledValue(template, "state", template.state),
                "photoshop:Country" to enabledValue(template, "country", template.country),
                "Iptc4xmpCore:Location" to enabledValue(template, "location", template.location),
                "Iptc4xmpCore:CountryCode" to enabledValue(template, "isoCountryCode", template.isoCountryCode)
            )
            val standardChecks = template.standardFields
                .mapNotNull { (field, value) -> XMP_FIELD_TAGS[field]?.let { it to value } }

            templateMarkerVerified &&
                coreChecks.all { (tag, value) -> hasXmpValue(root, tag, value) } &&
                standardChecks.all { (tag, value) -> hasXmpValue(root, tag, value) }
        }.getOrElse { false }
    }

    private fun enabledValue(template: MetadataTemplate, field: String, value: String): String {
        return if (template.isFieldEnabled(field)) value.trim() else ""
    }

    private fun hasXmpValue(root: Element, tag: String, expectedValue: String): Boolean {
        val cleanValue = expectedValue.trim()
        if (cleanValue.isBlank()) return true
        val text = root.textFor(tag)
        return text == cleanValue || text.split(",", "\uFF0C", ";", "\n").map { it.trim() }.contains(cleanValue)
    }

    private fun parseXml(xml: String): Element {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        }
        return factory.newDocumentBuilder().parse(InputSource(StringReader(xml))).documentElement
    }

    private fun Element.textFor(tagName: String): String {
        val nodes = getElementsByTagName(tagName)
        return nodes.item(0)?.textContent?.trim().orEmpty()
    }

    private val IIM_FIELD_TYPES = mapOf(
        MetadataFieldDefinitions.IIM_RECORD_VERSION to IptcTypes.RECORD_VERSION,
        MetadataFieldDefinitions.IIM_OBJECT_TYPE to IptcTypes.OBJECT_TYPE_REFERENCE,
        MetadataFieldDefinitions.IIM_OBJECT_ATTRIBUTE to IptcTypes.OBJECT_ATTRIBUTE_REFERENCE,
        MetadataFieldDefinitions.IIM_OBJECT_NAME to IptcTypes.OBJECT_NAME,
        MetadataFieldDefinitions.IIM_EDIT_STATUS to IptcTypes.EDIT_STATUS,
        MetadataFieldDefinitions.IIM_URGENCY to IptcTypes.URGENCY,
        MetadataFieldDefinitions.IIM_CATEGORY to IptcTypes.CATEGORY,
        MetadataFieldDefinitions.IIM_SUPPLEMENTAL_CATEGORY to IptcTypes.SUPPLEMENTAL_CATEGORY,
        MetadataFieldDefinitions.IIM_SPECIAL_INSTRUCTIONS to IptcTypes.SPECIAL_INSTRUCTIONS,
        MetadataFieldDefinitions.IIM_DATE_CREATED to IptcTypes.DATE_CREATED,
        MetadataFieldDefinitions.IIM_TIME_CREATED to IptcTypes.TIME_CREATED,
        MetadataFieldDefinitions.IIM_BYLINE to IptcTypes.BYLINE,
        MetadataFieldDefinitions.IIM_BYLINE_TITLE to IptcTypes.BYLINE_TITLE,
        MetadataFieldDefinitions.IIM_CITY to IptcTypes.CITY,
        MetadataFieldDefinitions.IIM_SUBLOCATION to IptcTypes.SUBLOCATION,
        MetadataFieldDefinitions.IIM_STATE to IptcTypes.PROVINCE_STATE,
        MetadataFieldDefinitions.IIM_COUNTRY_CODE to IptcTypes.COUNTRY_PRIMARY_LOCATION_CODE,
        MetadataFieldDefinitions.IIM_COUNTRY_NAME to IptcTypes.COUNTRY_PRIMARY_LOCATION_NAME,
        MetadataFieldDefinitions.IIM_HEADLINE to IptcTypes.HEADLINE,
        MetadataFieldDefinitions.IIM_CREDIT to IptcTypes.CREDIT,
        MetadataFieldDefinitions.IIM_SOURCE to IptcTypes.SOURCE,
        MetadataFieldDefinitions.IIM_COPYRIGHT to IptcTypes.COPYRIGHT_NOTICE,
        MetadataFieldDefinitions.IIM_CONTACT to IptcTypes.CONTACT,
        MetadataFieldDefinitions.IIM_CAPTION to IptcTypes.CAPTION_ABSTRACT,
        MetadataFieldDefinitions.IIM_WRITER to IptcTypes.WRITER_EDITOR
    )

    private val XMP_FIELD_TAGS = mapOf(
        MetadataFieldDefinitions.IPTC_CORE_CREATOR_CITY to "Iptc4xmpCore:CiAdrCity",
        MetadataFieldDefinitions.IPTC_CORE_CREATOR_REGION to "Iptc4xmpCore:CiAdrRegion",
        MetadataFieldDefinitions.IPTC_CORE_CREATOR_COUNTRY to "Iptc4xmpCore:CiAdrCtry",
        MetadataFieldDefinitions.IPTC_CORE_CREATOR_COUNTRY_CODE to "Iptc4xmpCore:CiAdrCtryCode",
        MetadataFieldDefinitions.IPTC_CORE_CREATOR_ADDRESS to "Iptc4xmpCore:CiAdrExtadr",
        MetadataFieldDefinitions.IPTC_CORE_CREATOR_EMAIL to "Iptc4xmpCore:CiEmailWork",
        MetadataFieldDefinitions.IPTC_CORE_CREATOR_PHONE to "Iptc4xmpCore:CiTelWork",
        MetadataFieldDefinitions.IPTC_CORE_CREATOR_WEB_URL to "Iptc4xmpCore:CiUrlWork",
        MetadataFieldDefinitions.IPTC_CORE_LOCATION to "Iptc4xmpCore:Location",
        MetadataFieldDefinitions.IPTC_CORE_COUNTRY_CODE to "Iptc4xmpCore:CountryCode",
        MetadataFieldDefinitions.IPTC_CORE_INTELLECTUAL_GENRE to "Iptc4xmpCore:IntellectualGenre",
        MetadataFieldDefinitions.IPTC_CORE_SCENE to "Iptc4xmpCore:Scene",
        MetadataFieldDefinitions.IPTC_CORE_SUBJECT_CODE to "Iptc4xmpCore:SubjectCode",
        MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_COUNTRY_CODE to "Iptc4xmpExt:CountryCode",
        MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_COUNTRY_NAME to "Iptc4xmpExt:CountryName",
        MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_REGION to "Iptc4xmpExt:ProvinceState",
        MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_CITY to "Iptc4xmpExt:City",
        MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_SUBLOCATION to "Iptc4xmpExt:Sublocation",
        MetadataFieldDefinitions.IPTC_EXT_EVENT to "Iptc4xmpExt:Event",
        MetadataFieldDefinitions.IPTC_EXT_PERSON_IN_IMAGE to "Iptc4xmpExt:PersonInImage",
        MetadataFieldDefinitions.IPTC_EXT_ARTWORK_TITLE to "Iptc4xmpExt:AOTitle",
        MetadataFieldDefinitions.IPTC_EXT_ARTWORK_CREATOR to "Iptc4xmpExt:AOCreator",
        MetadataFieldDefinitions.IPTC_EXT_ARTWORK_SOURCE to "Iptc4xmpExt:AOSource",
        MetadataFieldDefinitions.DC_TITLE to "dc:title",
        MetadataFieldDefinitions.DC_CREATOR to "dc:creator",
        MetadataFieldDefinitions.DC_DESCRIPTION to "dc:description",
        MetadataFieldDefinitions.DC_RIGHTS to "dc:rights",
        MetadataFieldDefinitions.DC_SOURCE to "dc:source",
        MetadataFieldDefinitions.DC_SUBJECT to "dc:subject",
        MetadataFieldDefinitions.DC_DATE to "dc:date",
        MetadataFieldDefinitions.PHOTOSHOP_HEADLINE to "photoshop:Headline",
        MetadataFieldDefinitions.PHOTOSHOP_CREDIT to "photoshop:Credit",
        MetadataFieldDefinitions.PHOTOSHOP_SOURCE to "photoshop:Source",
        MetadataFieldDefinitions.PHOTOSHOP_CITY to "photoshop:City",
        MetadataFieldDefinitions.PHOTOSHOP_STATE to "photoshop:State",
        MetadataFieldDefinitions.PHOTOSHOP_COUNTRY to "photoshop:Country",
        MetadataFieldDefinitions.PHOTOSHOP_DATE_CREATED to "photoshop:DateCreated"
    )
}
