package com.wkq.iptc.feature.press.metadata

import androidx.exifinterface.media.ExifInterface
import com.wkq.iptc.data.photo.PhotoRecordEntity
import com.wkq.iptc.feature.press.model.MetadataFieldDefinitions
import com.wkq.iptc.feature.press.model.MetadataTemplate
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * XMP 元数据写入工具。
 *
 * 输出包含 Dublin Core、Photoshop、IPTC Core、IPTC Extension 以及应用内部 fieldcam 命名空间。
 */
object XmpWriter {

    fun buildTemplatePacket(template: MetadataTemplate): String {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
        val author = template.iptcAuthor.ifBlank { template.creator }
        return buildXmpPacket(
            title = template.enabledValue("title", template.title.ifBlank { template.headline }),
            headline = template.enabledValue("headline", template.headline),
            description = template.enabledValue("caption", template.caption),
            creator = template.enabledValue("iptcAuthor", author),
            writer = template.enabledValue("writer", template.writer),
            credit = template.enabledValue("credit", template.credit),
            source = template.enabledValue("source", template.source),
            rights = template.enabledValue("copyright", template.copyright),
            country = template.enabledValue("country", template.country),
            state = template.enabledValue("state", template.state),
            city = template.enabledValue("city", template.city),
            location = template.enabledValue("location", template.location),
            countryCode = template.enabledValue("isoCountryCode", template.isoCountryCode),
            keywords = if (template.isFieldEnabled("keywords")) template.keywords else emptyList(),
            dateCreated = now,
            templateId = template.id,
            templateName = template.displayName,
            templateType = template.templateType,
            customFields = template.customFields,
            standardValue = { key, fallback ->
                if (template.isFieldEnabled(key)) template.standardValue(key).ifBlank { fallback } else fallback
            },
            standardEnabled = { key -> template.isFieldEnabled(key) }
        )
    }

    fun writeRecordMetadata(photoFile: File, record: PhotoRecordEntity): Result<Unit> {
        return runCatching {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
            val author = record.iptcAuthor.ifBlank { record.creator }
            val packet = buildXmpPacket(
                title = record.title.ifBlank { record.headline },
                headline = record.headline,
                description = record.caption,
                creator = author,
                writer = record.writer,
                credit = record.credit,
                source = record.source,
                rights = record.copyright,
                country = record.country,
                state = record.state,
                city = record.city,
                location = record.location,
                countryCode = record.isoCountryCode,
                keywords = record.keywords.split(",").map { it.trim() }.filter { it.isNotBlank() },
                dateCreated = now,
                templateId = record.templateId,
                templateName = record.templateName,
                templateType = "",
                customFields = parseCustomFields(record.customFields),
                standardValue = { _, fallback -> fallback },
                standardEnabled = { false }
            )
            val exif = ExifInterface(photoFile)
            exif.setAttribute(ExifInterface.TAG_XMP, packet)
            exif.saveAttributes()
        }
    }

    private fun buildXmpPacket(
        title: String,
        headline: String,
        description: String,
        creator: String,
        writer: String,
        credit: String,
        source: String,
        rights: String,
        country: String,
        state: String,
        city: String,
        location: String,
        countryCode: String,
        keywords: List<String>,
        dateCreated: String,
        templateId: String,
        templateName: String,
        templateType: String,
        customFields: Map<String, String>,
        standardValue: (String, String) -> String,
        standardEnabled: (String) -> Boolean
    ): String {
        val sb = StringBuilder()
        sb.appendLine("""<?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>""")
        sb.appendLine("""<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="PressIPTC">""")
        sb.appendLine("""  <rdf:RDF""")
        sb.appendLine("""    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"""")
        sb.appendLine("""    xmlns:dc="http://purl.org/dc/elements/1.1/"""")
        sb.appendLine("""    xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/"""")
        sb.appendLine("""    xmlns:Iptc4xmpCore="http://iptc.org/std/Iptc4xmpCore/1.0/xmlns/"""")
        sb.appendLine("""    xmlns:Iptc4xmpExt="http://iptc.org/std/Iptc4xmpExt/2008-02-29/"""")
        sb.appendLine("""    xmlns:fieldcam="https://wkq.com/ns/fieldcam/1.0/">""")
        sb.appendLine("""    <rdf:Description rdf:about="">""")

        appendDublinCore(sb, title, description, creator, rights, source, keywords, dateCreated, standardValue, standardEnabled)
        appendPhotoshop(sb, headline, credit, source, city, state, country, writer, dateCreated, standardValue)
        appendIptcCore(sb, location, countryCode, standardValue)
        appendIptcExtension(sb, standardValue)

        if (templateId.isNotBlank()) sb.appendLine("""      <fieldcam:templateId>${escape(templateId)}</fieldcam:templateId>""")
        if (templateName.isNotBlank()) sb.appendLine("""      <fieldcam:templateName>${escape(templateName)}</fieldcam:templateName>""")
        if (templateType.isNotBlank()) sb.appendLine("""      <fieldcam:templateType>${escape(templateType)}</fieldcam:templateType>""")
        appendSceneFields(sb, customFields)

        sb.appendLine("""    </rdf:Description>""")
        sb.appendLine("""  </rdf:RDF>""")
        sb.appendLine("""</x:xmpmeta>""")
        sb.appendLine("""<?xpacket end="w"?>""")
        return sb.toString()
    }

    private fun appendDublinCore(
        sb: StringBuilder,
        title: String,
        description: String,
        creator: String,
        rights: String,
        source: String,
        keywords: List<String>,
        dateCreated: String,
        standardValue: (String, String) -> String,
        standardEnabled: (String) -> Boolean
    ) {
        val dcTitle = standardValue(MetadataFieldDefinitions.DC_TITLE, title)
        val dcDescription = standardValue(MetadataFieldDefinitions.DC_DESCRIPTION, description)
        val dcCreator = standardValue(MetadataFieldDefinitions.DC_CREATOR, creator)
        val dcRights = standardValue(MetadataFieldDefinitions.DC_RIGHTS, rights)
        val dcSource = standardValue(MetadataFieldDefinitions.DC_SOURCE, source)
        val dcDate = if (standardEnabled(MetadataFieldDefinitions.DC_DATE)) {
            standardValue(MetadataFieldDefinitions.DC_DATE, dateCreated)
        } else {
            ""
        }
        val dcSubject = standardValue(MetadataFieldDefinitions.DC_SUBJECT, keywords.joinToString(","))
            .split(",", "\uFF0C", ";", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        appendAlt(sb, "dc:title", dcTitle)
        appendAlt(sb, "dc:description", dcDescription)
        if (dcCreator.isNotBlank()) {
            appendSeq(sb, "dc:creator", listOf(dcCreator))
        }
        appendAlt(sb, "dc:rights", dcRights)
        if (dcSource.isNotBlank()) sb.appendLine("""      <dc:source>${escape(dcSource)}</dc:source>""")
        if (dcDate.isNotBlank()) sb.appendLine("""      <dc:date>${escape(dcDate)}</dc:date>""")
        appendBag(sb, "dc:subject", dcSubject)
    }

    private fun appendPhotoshop(
        sb: StringBuilder,
        headline: String,
        credit: String,
        source: String,
        city: String,
        state: String,
        country: String,
        writer: String,
        dateCreated: String,
        standardValue: (String, String) -> String
    ) {
        appendSimple(sb, "photoshop:Headline", standardValue(MetadataFieldDefinitions.PHOTOSHOP_HEADLINE, headline))
        appendSimple(sb, "photoshop:Credit", standardValue(MetadataFieldDefinitions.PHOTOSHOP_CREDIT, credit))
        appendSimple(sb, "photoshop:Source", standardValue(MetadataFieldDefinitions.PHOTOSHOP_SOURCE, source))
        appendSimple(sb, "photoshop:City", standardValue(MetadataFieldDefinitions.PHOTOSHOP_CITY, city))
        appendSimple(sb, "photoshop:State", standardValue(MetadataFieldDefinitions.PHOTOSHOP_STATE, state))
        appendSimple(sb, "photoshop:Country", standardValue(MetadataFieldDefinitions.PHOTOSHOP_COUNTRY, country))
        appendSimple(sb, "photoshop:CaptionWriter", writer)
        appendSimple(sb, "photoshop:DateCreated", standardValue(MetadataFieldDefinitions.PHOTOSHOP_DATE_CREATED, dateCreated))
    }

    private fun appendIptcCore(
        sb: StringBuilder,
        location: String,
        countryCode: String,
        standardValue: (String, String) -> String
    ) {
        appendSimple(sb, "Iptc4xmpCore:Location", standardValue(MetadataFieldDefinitions.IPTC_CORE_LOCATION, location))
        appendSimple(sb, "Iptc4xmpCore:CountryCode", standardValue(MetadataFieldDefinitions.IPTC_CORE_COUNTRY_CODE, countryCode))
        appendSimple(sb, "Iptc4xmpCore:IntellectualGenre", standardValue(MetadataFieldDefinitions.IPTC_CORE_INTELLECTUAL_GENRE, ""))
        appendBagFromText(sb, "Iptc4xmpCore:Scene", standardValue(MetadataFieldDefinitions.IPTC_CORE_SCENE, ""))
        appendBagFromText(sb, "Iptc4xmpCore:SubjectCode", standardValue(MetadataFieldDefinitions.IPTC_CORE_SUBJECT_CODE, ""))

        val contactValues = listOf(
            "Iptc4xmpCore:CiAdrCity" to standardValue(MetadataFieldDefinitions.IPTC_CORE_CREATOR_CITY, ""),
            "Iptc4xmpCore:CiAdrRegion" to standardValue(MetadataFieldDefinitions.IPTC_CORE_CREATOR_REGION, ""),
            "Iptc4xmpCore:CiAdrCtry" to standardValue(MetadataFieldDefinitions.IPTC_CORE_CREATOR_COUNTRY, ""),
            "Iptc4xmpCore:CiAdrCtryCode" to standardValue(MetadataFieldDefinitions.IPTC_CORE_CREATOR_COUNTRY_CODE, ""),
            "Iptc4xmpCore:CiAdrExtadr" to standardValue(MetadataFieldDefinitions.IPTC_CORE_CREATOR_ADDRESS, ""),
            "Iptc4xmpCore:CiEmailWork" to standardValue(MetadataFieldDefinitions.IPTC_CORE_CREATOR_EMAIL, ""),
            "Iptc4xmpCore:CiTelWork" to standardValue(MetadataFieldDefinitions.IPTC_CORE_CREATOR_PHONE, ""),
            "Iptc4xmpCore:CiUrlWork" to standardValue(MetadataFieldDefinitions.IPTC_CORE_CREATOR_WEB_URL, "")
        ).filter { it.second.isNotBlank() }
        if (contactValues.isNotEmpty()) {
            sb.appendLine("""      <Iptc4xmpCore:CreatorContactInfo rdf:parseType="Resource">""")
            contactValues.forEach { (tag, value) -> appendSimple(sb, tag, value, indent = "        ") }
            sb.appendLine("""      </Iptc4xmpCore:CreatorContactInfo>""")
        }
    }

    private fun appendIptcExtension(
        sb: StringBuilder,
        standardValue: (String, String) -> String
    ) {
        appendSimple(sb, "Iptc4xmpExt:Event", standardValue(MetadataFieldDefinitions.IPTC_EXT_EVENT, ""))
        appendBagFromText(sb, "Iptc4xmpExt:PersonInImage", standardValue(MetadataFieldDefinitions.IPTC_EXT_PERSON_IN_IMAGE, ""))

        val locationValues = listOf(
            "Iptc4xmpExt:CountryCode" to standardValue(MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_COUNTRY_CODE, ""),
            "Iptc4xmpExt:CountryName" to standardValue(MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_COUNTRY_NAME, ""),
            "Iptc4xmpExt:ProvinceState" to standardValue(MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_REGION, ""),
            "Iptc4xmpExt:City" to standardValue(MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_CITY, ""),
            "Iptc4xmpExt:Sublocation" to standardValue(MetadataFieldDefinitions.IPTC_EXT_LOCATION_CREATED_SUBLOCATION, "")
        ).filter { it.second.isNotBlank() }
        if (locationValues.isNotEmpty()) {
            sb.appendLine("""      <Iptc4xmpExt:LocationCreated><rdf:Bag><rdf:li rdf:parseType="Resource">""")
            locationValues.forEach { (tag, value) -> appendSimple(sb, tag, value, indent = "        ") }
            sb.appendLine("""      </rdf:li></rdf:Bag></Iptc4xmpExt:LocationCreated>""")
        }

        val artworkValues = listOf(
            "Iptc4xmpExt:AOTitle" to standardValue(MetadataFieldDefinitions.IPTC_EXT_ARTWORK_TITLE, ""),
            "Iptc4xmpExt:AOCreator" to standardValue(MetadataFieldDefinitions.IPTC_EXT_ARTWORK_CREATOR, ""),
            "Iptc4xmpExt:AOSource" to standardValue(MetadataFieldDefinitions.IPTC_EXT_ARTWORK_SOURCE, "")
        ).filter { it.second.isNotBlank() }
        if (artworkValues.isNotEmpty()) {
            sb.appendLine("""      <Iptc4xmpExt:ArtworkOrObject><rdf:Bag><rdf:li rdf:parseType="Resource">""")
            artworkValues.forEach { (tag, value) -> appendSimple(sb, tag, value, indent = "        ") }
            sb.appendLine("""      </rdf:li></rdf:Bag></Iptc4xmpExt:ArtworkOrObject>""")
        }
    }

    private fun MetadataTemplate.enabledValue(field: String, value: String): String {
        return if (isFieldEnabled(field)) value else ""
    }

    private fun appendSceneFields(sb: StringBuilder, fields: Map<String, String>) {
        val cleanFields = fields
            .map { it.key.trim() to it.value.trim() }
            .filter { it.first.isNotBlank() }
        if (cleanFields.isEmpty()) return

        sb.appendLine("""      <fieldcam:sceneFields><rdf:Bag>""")
        cleanFields.forEach { (name, value) ->
            sb.appendLine("""        <rdf:li rdf:parseType="Resource">""")
            sb.appendLine("""          <fieldcam:name>${escape(name)}</fieldcam:name>""")
            if (value.isNotBlank()) {
                sb.appendLine("""          <fieldcam:value>${escape(value)}</fieldcam:value>""")
            }
            sb.appendLine("""        </rdf:li>""")
        }
        sb.appendLine("""      </rdf:Bag></fieldcam:sceneFields>""")
    }

    private fun parseCustomFields(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence()
                .mapNotNull { key ->
                    val value = json.optString(key, "")
                    if (key.isBlank()) null else key to value
                }
                .toMap()
        }.getOrDefault(emptyMap())
    }

    private fun appendSimple(sb: StringBuilder, tag: String, value: String, indent: String = "      ") {
        if (value.isNotBlank()) sb.appendLine("""$indent<$tag>${escape(value)}</$tag>""")
    }

    private fun appendAlt(sb: StringBuilder, tag: String, value: String) {
        if (value.isNotBlank()) {
            sb.appendLine("""      <$tag><rdf:Alt><rdf:li xml:lang="x-default">${escape(value)}</rdf:li></rdf:Alt></$tag>""")
        }
    }

    private fun appendSeq(sb: StringBuilder, tag: String, values: List<String>) {
        val clean = values.filter { it.isNotBlank() }
        if (clean.isEmpty()) return
        sb.appendLine("""      <$tag><rdf:Seq>""")
        clean.forEach { sb.appendLine("""        <rdf:li>${escape(it)}</rdf:li>""") }
        sb.appendLine("""      </rdf:Seq></$tag>""")
    }

    private fun appendBagFromText(sb: StringBuilder, tag: String, value: String) {
        appendBag(
            sb,
            tag,
            value.split(",", "\uFF0C", ";", "\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        )
    }

    private fun appendBag(sb: StringBuilder, tag: String, values: List<String>) {
        val clean = values.filter { it.isNotBlank() }
        if (clean.isEmpty()) return
        sb.appendLine("""      <$tag><rdf:Bag>""")
        clean.forEach { sb.appendLine("""        <rdf:li>${escape(it)}</rdf:li>""") }
        sb.appendLine("""      </rdf:Bag></$tag>""")
    }

    private fun escape(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
