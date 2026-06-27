package com.wkq.iptc.feature.press.metadata

import com.wkq.iptc.data.photo.PhotoRecordEntity
import com.wkq.iptc.feature.press.model.MetadataFieldDefinitions
import com.wkq.iptc.feature.press.model.MetadataTemplate
import org.apache.commons.imaging.bytesource.ByteSource
import org.apache.commons.imaging.formats.jpeg.JpegImagingParameters
import org.apache.commons.imaging.formats.jpeg.iptc.IptcRecord
import org.apache.commons.imaging.formats.jpeg.iptc.IptcTypes
import org.apache.commons.imaging.formats.jpeg.iptc.JpegIptcRewriter
import org.apache.commons.imaging.formats.jpeg.iptc.PhotoshopApp13Data
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object IptcWriter {

    fun writeTemplateMetadata(photoFile: File, template: MetadataTemplate): Result<Unit> {
        return writeRecords(photoFile, templateToRecords(template))
    }

    fun writeRecordMetadata(photoFile: File, record: PhotoRecordEntity): Result<Unit> {
        return writeRecords(photoFile, recordToRecords(record))
    }

    private fun writeRecords(photoFile: File, records: List<IptcRecord>): Result<Unit> {
        return runCatching {
            val imageBytes = photoFile.readBytes()
            val byteSource = ByteSource.array(imageBytes)
            val metadata = org.apache.commons.imaging.formats.jpeg.JpegImageParser()
                .getPhotoshopMetadata(byteSource, JpegImagingParameters())
            val nonIptcBlocks = metadata?.photoshopApp13Data?.nonIptcBlocks.orEmpty()
            val app13Data = PhotoshopApp13Data(records, nonIptcBlocks)
            val updatedBytes = ByteArrayOutputStream().use { output ->
                JpegIptcRewriter().writeIptc(byteSource, output, app13Data)
                output.toByteArray()
            }
            photoFile.writeBytes(updatedBytes)
        }
    }

    private fun templateToRecords(template: MetadataTemplate): List<IptcRecord> {
        val now = Date()
        return buildList {
            addEnabled(template, IptcTypes.OBJECT_NAME, "title", template.title.ifBlank { template.headline })
            addEnabled(template, IptcTypes.HEADLINE, "headline", template.headline)
            addEnabled(template, IptcTypes.CAPTION_ABSTRACT, "caption", template.caption)
            addEnabled(template, IptcTypes.BYLINE, "iptcAuthor", template.iptcAuthor.ifBlank { template.creator })
            addEnabled(template, IptcTypes.WRITER_EDITOR, "writer", template.writer)
            addEnabled(template, IptcTypes.CATEGORY, "category", template.category)
            addEnabled(template, IptcTypes.CITY, "city", template.city)
            addEnabled(template, IptcTypes.PROVINCE_STATE, "state", template.state)
            addEnabled(template, IptcTypes.COUNTRY_PRIMARY_LOCATION_NAME, "country", template.country)
            addEnabled(template, IptcTypes.COUNTRY_PRIMARY_LOCATION_CODE, "isoCountryCode", template.isoCountryCode)
            addEnabled(template, IptcTypes.CREDIT, "credit", template.credit)
            addEnabled(template, IptcTypes.SOURCE, "source", template.source)
            addEnabled(template, IptcTypes.COPYRIGHT_NOTICE, "copyright", template.copyright)
            addEnabled(template, IptcTypes.CONTACT, "email", template.email)
            if (template.isFieldEnabled("keywords")) {
                template.keywords.forEach { add(IptcRecord(IptcTypes.KEYWORDS, it)) }
            }

            addStandard(template, IptcTypes.RECORD_VERSION, MetadataFieldDefinitions.IIM_RECORD_VERSION)
            addStandard(template, IptcTypes.OBJECT_TYPE_REFERENCE, MetadataFieldDefinitions.IIM_OBJECT_TYPE)
            addStandard(template, IptcTypes.OBJECT_ATTRIBUTE_REFERENCE, MetadataFieldDefinitions.IIM_OBJECT_ATTRIBUTE)
            addStandard(template, IptcTypes.OBJECT_NAME, MetadataFieldDefinitions.IIM_OBJECT_NAME)
            addStandard(template, IptcTypes.EDIT_STATUS, MetadataFieldDefinitions.IIM_EDIT_STATUS)
            addStandard(template, IptcTypes.URGENCY, MetadataFieldDefinitions.IIM_URGENCY)
            addStandard(template, IptcTypes.CATEGORY, MetadataFieldDefinitions.IIM_CATEGORY)
            addStandard(template, IptcTypes.SUPPLEMENTAL_CATEGORY, MetadataFieldDefinitions.IIM_SUPPLEMENTAL_CATEGORY)
            addStandardList(template, IptcTypes.KEYWORDS, MetadataFieldDefinitions.IIM_KEYWORDS)
            addStandard(template, IptcTypes.SPECIAL_INSTRUCTIONS, MetadataFieldDefinitions.IIM_SPECIAL_INSTRUCTIONS)
            addStandard(template, IptcTypes.DATE_CREATED, MetadataFieldDefinitions.IIM_DATE_CREATED, SimpleDateFormat("yyyyMMdd", Locale.US).format(now))
            addStandard(template, IptcTypes.TIME_CREATED, MetadataFieldDefinitions.IIM_TIME_CREATED, SimpleDateFormat("HHmmss", Locale.US).format(now))
            addStandard(template, IptcTypes.BYLINE, MetadataFieldDefinitions.IIM_BYLINE)
            addStandard(template, IptcTypes.BYLINE_TITLE, MetadataFieldDefinitions.IIM_BYLINE_TITLE)
            addStandard(template, IptcTypes.CITY, MetadataFieldDefinitions.IIM_CITY)
            addStandard(template, IptcTypes.SUBLOCATION, MetadataFieldDefinitions.IIM_SUBLOCATION)
            addStandard(template, IptcTypes.PROVINCE_STATE, MetadataFieldDefinitions.IIM_STATE)
            addStandard(template, IptcTypes.COUNTRY_PRIMARY_LOCATION_CODE, MetadataFieldDefinitions.IIM_COUNTRY_CODE)
            addStandard(template, IptcTypes.COUNTRY_PRIMARY_LOCATION_NAME, MetadataFieldDefinitions.IIM_COUNTRY_NAME)
            addStandard(template, IptcTypes.HEADLINE, MetadataFieldDefinitions.IIM_HEADLINE)
            addStandard(template, IptcTypes.CREDIT, MetadataFieldDefinitions.IIM_CREDIT)
            addStandard(template, IptcTypes.SOURCE, MetadataFieldDefinitions.IIM_SOURCE)
            addStandard(template, IptcTypes.COPYRIGHT_NOTICE, MetadataFieldDefinitions.IIM_COPYRIGHT)
            addStandard(template, IptcTypes.CONTACT, MetadataFieldDefinitions.IIM_CONTACT)
            addStandard(template, IptcTypes.CAPTION_ABSTRACT, MetadataFieldDefinitions.IIM_CAPTION)
            addStandard(template, IptcTypes.WRITER_EDITOR, MetadataFieldDefinitions.IIM_WRITER)
        }.filter { it.value.isNotBlank() }
    }

    private fun recordToRecords(record: PhotoRecordEntity): List<IptcRecord> {
        return buildList {
            add(IptcRecord(IptcTypes.OBJECT_NAME, record.title.ifBlank { record.headline }))
            add(IptcRecord(IptcTypes.HEADLINE, record.headline))
            add(IptcRecord(IptcTypes.CAPTION_ABSTRACT, record.caption))
            add(IptcRecord(IptcTypes.BYLINE, record.iptcAuthor.ifBlank { record.creator }))
            add(IptcRecord(IptcTypes.WRITER_EDITOR, record.writer))
            add(IptcRecord(IptcTypes.CATEGORY, record.category))
            add(IptcRecord(IptcTypes.CITY, record.city))
            add(IptcRecord(IptcTypes.PROVINCE_STATE, record.state))
            add(IptcRecord(IptcTypes.COUNTRY_PRIMARY_LOCATION_NAME, record.country))
            add(IptcRecord(IptcTypes.COUNTRY_PRIMARY_LOCATION_CODE, record.isoCountryCode))
            add(IptcRecord(IptcTypes.CREDIT, record.credit))
            add(IptcRecord(IptcTypes.SOURCE, record.source))
            add(IptcRecord(IptcTypes.COPYRIGHT_NOTICE, record.copyright))
            record.keywords.split(",").map { it.trim() }.filter { it.isNotBlank() }
                .forEach { add(IptcRecord(IptcTypes.KEYWORDS, it)) }
        }.filter { it.value.isNotBlank() }
    }

    private fun MutableList<IptcRecord>.addEnabled(
        template: MetadataTemplate,
        type: IptcTypes,
        field: String,
        value: String
    ) {
        if (template.isFieldEnabled(field) && value.isNotBlank()) {
            add(IptcRecord(type, value))
        }
    }

    private fun MutableList<IptcRecord>.addStandard(
        template: MetadataTemplate,
        type: IptcTypes,
        field: String,
        fallback: String = ""
    ) {
        if (!template.isFieldEnabled(field)) return
        val value = template.standardValue(field).ifBlank { fallback }
        if (value.isNotBlank()) {
            add(IptcRecord(type, value))
        }
    }

    private fun MutableList<IptcRecord>.addStandardList(
        template: MetadataTemplate,
        type: IptcTypes,
        field: String
    ) {
        if (!template.isFieldEnabled(field)) return
        template.standardValue(field)
            .split(",", "\uFF0C", ";", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { add(IptcRecord(type, it)) }
    }
}
