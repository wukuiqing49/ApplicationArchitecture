package com.wkq.site.report.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inspection_record",
    indices = [
        Index(value = ["status"]),
        Index(value = ["template_id"]),
        Index(value = ["updated_at"])
    ]
)
data class InspectionRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "project_name")
    val projectName: String,
    val location: String,
    @ColumnInfo(name = "template_id")
    val templateId: String = DEFAULT_TEMPLATE_ID,
    @ColumnInfo(name = "template_name")
    val templateName: String = "",
    @ColumnInfo(name = "pdf_template_id")
    val pdfTemplateId: String = "",
    @ColumnInfo(name = "server_profile_id")
    val serverProfileId: String = "",
    @ColumnInfo(name = "auto_upload_enabled")
    val autoUploadEnabled: Boolean = false,
    @ColumnInfo(name = "signature_profile_id")
    val signatureProfileId: String = "",
    val status: String = STATUS_DRAFT,
    val progress: Int = 0,
    @ColumnInfo(name = "photo_count")
    val photoCount: Int = 0,
    @ColumnInfo(name = "report_file_path")
    val reportFilePath: String = "",
    @ColumnInfo(name = "upload_status")
    val uploadStatus: String = UPLOAD_NONE,
    @ColumnInfo(name = "upload_remote_path")
    val uploadRemotePath: String = "",
    @ColumnInfo(name = "upload_error")
    val uploadError: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_TEMPLATE_ID = "default_site_inspection"
        const val STATUS_DRAFT = "draft"
        const val STATUS_ACTIVE = "active"
        const val STATUS_DONE = "done"

        const val UPLOAD_NONE = "none"
        const val UPLOAD_QUEUED = "queued"
        const val UPLOAD_GENERATED = "generated"
        const val UPLOAD_UPLOADED = "uploaded"
        const val UPLOAD_FAILED = "failed"
    }
}
