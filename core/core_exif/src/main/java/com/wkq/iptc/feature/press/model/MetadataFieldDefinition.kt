package com.wkq.iptc.feature.press.model

enum class MetadataStandard(
    val displayName: String,
    val groupName: String,
    val accentColor: String
) {
    IIM_APPLICATION("IPTC IIM", "IIM Application Record", "#F59E0B"),
    IPTC_CORE("IPTC Core", "XMP Iptc4xmpCore", "#22C55E"),
    IPTC_EXTENSION("IPTC Extension", "XMP Iptc4xmpExt", "#65A30D"),
    DUBLIN_CORE("Dublin Core", "XMP Dublin Core", "#DC2626"),
    PHOTOSHOP("Photoshop", "XMP Photoshop", "#7C3AED")
}

data class MetadataFieldDefinition(
    val key: String,
    val label: String,
    val standard: MetadataStandard,
    val detail: String,
    val defaultValue: String = "",
    val multiline: Boolean = false,
    val autofill: Boolean = false
)

object MetadataFieldDefinitions {
    const val IIM_RECORD_VERSION = "iim.recordVersion"
    const val IIM_OBJECT_TYPE = "iim.objectType"
    const val IIM_OBJECT_ATTRIBUTE = "iim.objectAttribute"
    const val IIM_OBJECT_NAME = "iim.objectName"
    const val IIM_EDIT_STATUS = "iim.editStatus"
    const val IIM_URGENCY = "iim.urgency"
    const val IIM_CATEGORY = "iim.category"
    const val IIM_SUPPLEMENTAL_CATEGORY = "iim.supplementalCategory"
    const val IIM_KEYWORDS = "iim.keywords"
    const val IIM_SPECIAL_INSTRUCTIONS = "iim.specialInstructions"
    const val IIM_DATE_CREATED = "iim.dateCreated"
    const val IIM_TIME_CREATED = "iim.timeCreated"
    const val IIM_BYLINE = "iim.byline"
    const val IIM_BYLINE_TITLE = "iim.bylineTitle"
    const val IIM_CITY = "iim.city"
    const val IIM_SUBLOCATION = "iim.sublocation"
    const val IIM_STATE = "iim.state"
    const val IIM_COUNTRY_CODE = "iim.countryCode"
    const val IIM_COUNTRY_NAME = "iim.countryName"
    const val IIM_HEADLINE = "iim.headline"
    const val IIM_CREDIT = "iim.credit"
    const val IIM_SOURCE = "iim.source"
    const val IIM_COPYRIGHT = "iim.copyright"
    const val IIM_CONTACT = "iim.contact"
    const val IIM_CAPTION = "iim.caption"
    const val IIM_WRITER = "iim.writer"

    const val IPTC_CORE_CREATOR_CITY = "iptcCore.creatorCity"
    const val IPTC_CORE_CREATOR_REGION = "iptcCore.creatorRegion"
    const val IPTC_CORE_CREATOR_COUNTRY = "iptcCore.creatorCountry"
    const val IPTC_CORE_CREATOR_COUNTRY_CODE = "iptcCore.creatorCountryCode"
    const val IPTC_CORE_CREATOR_ADDRESS = "iptcCore.creatorAddress"
    const val IPTC_CORE_CREATOR_EMAIL = "iptcCore.creatorEmail"
    const val IPTC_CORE_CREATOR_PHONE = "iptcCore.creatorPhone"
    const val IPTC_CORE_CREATOR_WEB_URL = "iptcCore.creatorWebUrl"
    const val IPTC_CORE_LOCATION = "iptcCore.location"
    const val IPTC_CORE_COUNTRY_CODE = "iptcCore.countryCode"
    const val IPTC_CORE_INTELLECTUAL_GENRE = "iptcCore.intellectualGenre"
    const val IPTC_CORE_SCENE = "iptcCore.scene"
    const val IPTC_CORE_SUBJECT_CODE = "iptcCore.subjectCode"

    const val IPTC_EXT_LOCATION_CREATED_COUNTRY_CODE = "iptcExt.locationCreatedCountryCode"
    const val IPTC_EXT_LOCATION_CREATED_COUNTRY_NAME = "iptcExt.locationCreatedCountryName"
    const val IPTC_EXT_LOCATION_CREATED_REGION = "iptcExt.locationCreatedRegion"
    const val IPTC_EXT_LOCATION_CREATED_CITY = "iptcExt.locationCreatedCity"
    const val IPTC_EXT_LOCATION_CREATED_SUBLOCATION = "iptcExt.locationCreatedSublocation"
    const val IPTC_EXT_EVENT = "iptcExt.event"
    const val IPTC_EXT_PERSON_IN_IMAGE = "iptcExt.personInImage"
    const val IPTC_EXT_ARTWORK_TITLE = "iptcExt.artworkTitle"
    const val IPTC_EXT_ARTWORK_CREATOR = "iptcExt.artworkCreator"
    const val IPTC_EXT_ARTWORK_SOURCE = "iptcExt.artworkSource"

    const val DC_TITLE = "dc.title"
    const val DC_CREATOR = "dc.creator"
    const val DC_DESCRIPTION = "dc.description"
    const val DC_RIGHTS = "dc.rights"
    const val DC_SOURCE = "dc.source"
    const val DC_SUBJECT = "dc.subject"
    const val DC_DATE = "dc.date"

    const val PHOTOSHOP_HEADLINE = "photoshop.headline"
    const val PHOTOSHOP_CREDIT = "photoshop.credit"
    const val PHOTOSHOP_SOURCE = "photoshop.source"
    const val PHOTOSHOP_CITY = "photoshop.city"
    const val PHOTOSHOP_STATE = "photoshop.state"
    const val PHOTOSHOP_COUNTRY = "photoshop.country"
    const val PHOTOSHOP_DATE_CREATED = "photoshop.dateCreated"

    val all: List<MetadataFieldDefinition> = listOf(
        MetadataFieldDefinition(IIM_RECORD_VERSION, "Record Version", MetadataStandard.IIM_APPLICATION, "[2:0]", "4"),
        MetadataFieldDefinition(IIM_OBJECT_TYPE, "Object Type", MetadataStandard.IIM_APPLICATION, "[2:3]"),
        MetadataFieldDefinition(IIM_OBJECT_ATTRIBUTE, "Object Attribute", MetadataStandard.IIM_APPLICATION, "[2:4]"),
        MetadataFieldDefinition(IIM_OBJECT_NAME, "Object Name", MetadataStandard.IIM_APPLICATION, "[2:5]"),
        MetadataFieldDefinition(IIM_EDIT_STATUS, "Edit Status", MetadataStandard.IIM_APPLICATION, "[2:7]"),
        MetadataFieldDefinition(IIM_URGENCY, "Urgency", MetadataStandard.IIM_APPLICATION, "[2:10]"),
        MetadataFieldDefinition(IIM_CATEGORY, "Category", MetadataStandard.IIM_APPLICATION, "[2:15]"),
        MetadataFieldDefinition(IIM_SUPPLEMENTAL_CATEGORY, "Supplemental Category", MetadataStandard.IIM_APPLICATION, "[2:20]"),
        MetadataFieldDefinition(IIM_KEYWORDS, "Keywords", MetadataStandard.IIM_APPLICATION, "[2:25]"),
        MetadataFieldDefinition(IIM_SPECIAL_INSTRUCTIONS, "Special Instructions", MetadataStandard.IIM_APPLICATION, "[2:40]", multiline = true),
        MetadataFieldDefinition(IIM_DATE_CREATED, "Date Created", MetadataStandard.IIM_APPLICATION, "[2:55]", autofill = true),
        MetadataFieldDefinition(IIM_TIME_CREATED, "Time Created", MetadataStandard.IIM_APPLICATION, "[2:60]", autofill = true),
        MetadataFieldDefinition(IIM_BYLINE, "Byline", MetadataStandard.IIM_APPLICATION, "[2:80]"),
        MetadataFieldDefinition(IIM_BYLINE_TITLE, "Byline Title", MetadataStandard.IIM_APPLICATION, "[2:85]"),
        MetadataFieldDefinition(IIM_CITY, "City", MetadataStandard.IIM_APPLICATION, "[2:90]"),
        MetadataFieldDefinition(IIM_SUBLOCATION, "Sublocation", MetadataStandard.IIM_APPLICATION, "[2:92]"),
        MetadataFieldDefinition(IIM_STATE, "Province / State", MetadataStandard.IIM_APPLICATION, "[2:95]"),
        MetadataFieldDefinition(IIM_COUNTRY_CODE, "Country Code", MetadataStandard.IIM_APPLICATION, "[2:100]"),
        MetadataFieldDefinition(IIM_COUNTRY_NAME, "Country Name", MetadataStandard.IIM_APPLICATION, "[2:101]"),
        MetadataFieldDefinition(IIM_HEADLINE, "Headline", MetadataStandard.IIM_APPLICATION, "[2:105]"),
        MetadataFieldDefinition(IIM_CREDIT, "Credit", MetadataStandard.IIM_APPLICATION, "[2:110]"),
        MetadataFieldDefinition(IIM_SOURCE, "Source", MetadataStandard.IIM_APPLICATION, "[2:115]"),
        MetadataFieldDefinition(IIM_COPYRIGHT, "Copyright Notice", MetadataStandard.IIM_APPLICATION, "[2:116]"),
        MetadataFieldDefinition(IIM_CONTACT, "Contact", MetadataStandard.IIM_APPLICATION, "[2:118]"),
        MetadataFieldDefinition(IIM_CAPTION, "Caption / Abstract", MetadataStandard.IIM_APPLICATION, "[2:120]", multiline = true),
        MetadataFieldDefinition(IIM_WRITER, "Writer / Editor", MetadataStandard.IIM_APPLICATION, "[2:122]"),

        MetadataFieldDefinition(IPTC_CORE_CREATOR_CITY, "Contact Info - City", MetadataStandard.IPTC_CORE, "CreatorContactInfo/CiAdrCity"),
        MetadataFieldDefinition(IPTC_CORE_CREATOR_REGION, "Contact Info - Region", MetadataStandard.IPTC_CORE, "CreatorContactInfo/CiAdrRegion"),
        MetadataFieldDefinition(IPTC_CORE_CREATOR_COUNTRY, "Contact Info - Country", MetadataStandard.IPTC_CORE, "CreatorContactInfo/CiAdrCtry"),
        MetadataFieldDefinition(IPTC_CORE_CREATOR_COUNTRY_CODE, "Contact Info - ISO Country Code", MetadataStandard.IPTC_CORE, "CreatorContactInfo/CiAdrCtryCode"),
        MetadataFieldDefinition(IPTC_CORE_CREATOR_ADDRESS, "Contact Info - Address", MetadataStandard.IPTC_CORE, "CreatorContactInfo/CiAdrExtadr", multiline = true),
        MetadataFieldDefinition(IPTC_CORE_CREATOR_EMAIL, "Contact Info - Email", MetadataStandard.IPTC_CORE, "CreatorContactInfo/CiEmailWork"),
        MetadataFieldDefinition(IPTC_CORE_CREATOR_PHONE, "Contact Info - Phone", MetadataStandard.IPTC_CORE, "CreatorContactInfo/CiTelWork"),
        MetadataFieldDefinition(IPTC_CORE_CREATOR_WEB_URL, "Contact Info - Web URL", MetadataStandard.IPTC_CORE, "CreatorContactInfo/CiUrlWork"),
        MetadataFieldDefinition(IPTC_CORE_LOCATION, "Location", MetadataStandard.IPTC_CORE, "Iptc4xmpCore:Location"),
        MetadataFieldDefinition(IPTC_CORE_COUNTRY_CODE, "Country Code", MetadataStandard.IPTC_CORE, "Iptc4xmpCore:CountryCode"),
        MetadataFieldDefinition(IPTC_CORE_INTELLECTUAL_GENRE, "Intellectual Genre", MetadataStandard.IPTC_CORE, "Iptc4xmpCore:IntellectualGenre"),
        MetadataFieldDefinition(IPTC_CORE_SCENE, "Scene", MetadataStandard.IPTC_CORE, "Iptc4xmpCore:Scene"),
        MetadataFieldDefinition(IPTC_CORE_SUBJECT_CODE, "Subject Code", MetadataStandard.IPTC_CORE, "Iptc4xmpCore:SubjectCode"),

        MetadataFieldDefinition(IPTC_EXT_LOCATION_CREATED_COUNTRY_CODE, "Location Created - ISO Code", MetadataStandard.IPTC_EXTENSION, "Iptc4xmpExt:LocationCreated/CountryCode"),
        MetadataFieldDefinition(IPTC_EXT_LOCATION_CREATED_COUNTRY_NAME, "Location Created - Country Name", MetadataStandard.IPTC_EXTENSION, "Iptc4xmpExt:LocationCreated/CountryName"),
        MetadataFieldDefinition(IPTC_EXT_LOCATION_CREATED_REGION, "Location Created - Province / State", MetadataStandard.IPTC_EXTENSION, "Iptc4xmpExt:LocationCreated/ProvinceState"),
        MetadataFieldDefinition(IPTC_EXT_LOCATION_CREATED_CITY, "Location Created - City", MetadataStandard.IPTC_EXTENSION, "Iptc4xmpExt:LocationCreated/City"),
        MetadataFieldDefinition(IPTC_EXT_LOCATION_CREATED_SUBLOCATION, "Location Created - Sublocation", MetadataStandard.IPTC_EXTENSION, "Iptc4xmpExt:LocationCreated/Sublocation"),
        MetadataFieldDefinition(IPTC_EXT_EVENT, "Event", MetadataStandard.IPTC_EXTENSION, "Iptc4xmpExt:Event"),
        MetadataFieldDefinition(IPTC_EXT_PERSON_IN_IMAGE, "Person In Image", MetadataStandard.IPTC_EXTENSION, "Iptc4xmpExt:PersonInImage"),
        MetadataFieldDefinition(IPTC_EXT_ARTWORK_TITLE, "Artwork or Object - Title", MetadataStandard.IPTC_EXTENSION, "Iptc4xmpExt:ArtworkOrObject/AOTitle"),
        MetadataFieldDefinition(IPTC_EXT_ARTWORK_CREATOR, "Artwork or Object - Creator", MetadataStandard.IPTC_EXTENSION, "Iptc4xmpExt:ArtworkOrObject/AOCreator"),
        MetadataFieldDefinition(IPTC_EXT_ARTWORK_SOURCE, "Artwork or Object - Source", MetadataStandard.IPTC_EXTENSION, "Iptc4xmpExt:ArtworkOrObject/AOSource"),

        MetadataFieldDefinition(DC_TITLE, "Title", MetadataStandard.DUBLIN_CORE, "dc:title"),
        MetadataFieldDefinition(DC_CREATOR, "Creator", MetadataStandard.DUBLIN_CORE, "dc:creator"),
        MetadataFieldDefinition(DC_DESCRIPTION, "Description", MetadataStandard.DUBLIN_CORE, "dc:description", multiline = true),
        MetadataFieldDefinition(DC_RIGHTS, "Rights", MetadataStandard.DUBLIN_CORE, "dc:rights", multiline = true),
        MetadataFieldDefinition(DC_SOURCE, "Source", MetadataStandard.DUBLIN_CORE, "dc:source"),
        MetadataFieldDefinition(DC_SUBJECT, "Subject", MetadataStandard.DUBLIN_CORE, "dc:subject"),
        MetadataFieldDefinition(DC_DATE, "Date", MetadataStandard.DUBLIN_CORE, "dc:date", autofill = true),

        MetadataFieldDefinition(PHOTOSHOP_HEADLINE, "Headline", MetadataStandard.PHOTOSHOP, "photoshop:Headline"),
        MetadataFieldDefinition(PHOTOSHOP_CREDIT, "Credit", MetadataStandard.PHOTOSHOP, "photoshop:Credit"),
        MetadataFieldDefinition(PHOTOSHOP_SOURCE, "Source", MetadataStandard.PHOTOSHOP, "photoshop:Source"),
        MetadataFieldDefinition(PHOTOSHOP_CITY, "City", MetadataStandard.PHOTOSHOP, "photoshop:City"),
        MetadataFieldDefinition(PHOTOSHOP_STATE, "State", MetadataStandard.PHOTOSHOP, "photoshop:State"),
        MetadataFieldDefinition(PHOTOSHOP_COUNTRY, "Country", MetadataStandard.PHOTOSHOP, "photoshop:Country"),
        MetadataFieldDefinition(PHOTOSHOP_DATE_CREATED, "Date Created", MetadataStandard.PHOTOSHOP, "photoshop:DateCreated", autofill = true)
    )

    val byKey: Map<String, MetadataFieldDefinition> = all.associateBy { it.key }

    fun isStandardField(field: String): Boolean = byKey.containsKey(field)
}
