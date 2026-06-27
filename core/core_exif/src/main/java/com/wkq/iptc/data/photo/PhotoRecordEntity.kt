package com.wkq.iptc.data.photo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photo_record",
    indices = [
        Index(
            value = ["account_id", "source_key", "process_type"],
            unique = true
        )
    ]
)
data class PhotoRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "uri")
    val uri: String,
    @ColumnInfo(name = "account_id")
    val accountId: String = "local",
    @ColumnInfo(name = "source_uri")
    val sourceUri: String = uri,
    @ColumnInfo(name = "source_key")
    val sourceKey: String = "",
    @ColumnInfo(name = "process_type")
    val processType: String = "legacy",
    @ColumnInfo(name = "output_path")
    val outputPath: String = "",
    @ColumnInfo(name = "output_mime_type")
    val outputMimeType: String = "image/jpeg",
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = 0L,
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "local_only",
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "template_id")
    val templateId: String,
    @ColumnInfo(name = "template_name")
    val templateName: String,
    @ColumnInfo(name = "headline")
    val headline: String,
    @ColumnInfo(name = "caption")
    val caption: String,
    @ColumnInfo(name = "title")
    val title: String = "",
    @ColumnInfo(name = "subtitle")
    val subtitle: String = "",
    @ColumnInfo(name = "keywords")
    val keywords: String,
    @ColumnInfo(name = "custom_fields")
    val customFields: String = "",
    @ColumnInfo(name = "creator")
    val creator: String,
    @ColumnInfo(name = "artist")
    val artist: String = "",
    @ColumnInfo(name = "job_title")
    val jobTitle: String = "",
    @ColumnInfo(name = "email")
    val email: String = "",
    @ColumnInfo(name = "iptc_author")
    val iptcAuthor: String = "",
    @ColumnInfo(name = "writer")
    val writer: String = "",
    @ColumnInfo(name = "category")
    val category: String = "",
    @ColumnInfo(name = "state")
    val state: String = "",
    @ColumnInfo(name = "iso_country_code")
    val isoCountryCode: String = "",
    @ColumnInfo(name = "credit")
    val credit: String = "",
    @ColumnInfo(name = "source")
    val source: String = "",
    @ColumnInfo(name = "copyright_status")
    val copyrightStatus: String = "",
    @ColumnInfo(name = "rights_usage_terms")
    val rightsUsageTerms: String = "",
    @ColumnInfo(name = "copyright")
    val copyright: String,
    @ColumnInfo(name = "country")
    val country: String,
    @ColumnInfo(name = "city")
    val city: String,
    @ColumnInfo(name = "location")
    val location: String,
    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,
    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,
    @ColumnInfo(name = "altitude")
    val altitude: Double? = null,
    @ColumnInfo(name = "bearing")
    val bearing: Float? = null,
    @ColumnInfo(name = "captured_at")
    val capturedAt: Long,
    @ColumnInfo(name = "processed_at")
    val processedAt: Long? = null,
    @ColumnInfo(name = "exif_verified")
    val exifVerified: Boolean = false,
    @ColumnInfo(name = "iptc_verified")
    val iptcVerified: Boolean = false,
    @ColumnInfo(name = "xmp_verified")
    val xmpVerified: Boolean = false,
    @ColumnInfo(name = "metadata_error")
    val metadataError: String? = null,
    @ColumnInfo(name = "anti_tamper_signed")
    val antiTamperSigned: Boolean = false,
    @ColumnInfo(name = "upload_status")
    val uploadStatus: String = "local_only",
    @ColumnInfo(name = "upload_remote_path")
    val uploadRemotePath: String? = null,
    @ColumnInfo(name = "upload_retry_count")
    val uploadRetryCount: Int = 0,
    @ColumnInfo(name = "upload_last_attempt_at")
    val uploadLastAttemptAt: Long? = null,
    @ColumnInfo(name = "upload_error")
    val uploadError: String? = null,
    @ColumnInfo(name = "upload_resume_offset")
    val uploadResumeOffset: Long = 0L,
    @ColumnInfo(name = "local_file_available")
    val localFileAvailable: Boolean = true,
    @ColumnInfo(name = "photo_source")
    val photoSource: String = "batch_processing"
)
