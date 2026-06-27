package com.wkq.site.report.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inspection_item",
    foreignKeys = [
        ForeignKey(
            entity = InspectionRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspection_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["inspection_id"]),
        Index(value = ["inspection_id", "item_key"], unique = true)
    ]
)
data class InspectionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "inspection_id")
    val inspectionId: Long,
    @ColumnInfo(name = "section_key")
    val sectionKey: String = "default",
    @ColumnInfo(name = "section_title")
    val sectionTitle: String = "Inspection Items",
    @ColumnInfo(name = "item_key")
    val itemKey: String,
    val title: String,
    val description: String = "",
    val required: Boolean = false,
    @ColumnInfo(name = "photo_required")
    val photoRequired: Boolean = false,
    @ColumnInfo(name = "min_photo_count")
    val minPhotoCount: Int = 0,
    @ColumnInfo(name = "allow_not_applicable")
    val allowNotApplicable: Boolean = true,
    @ColumnInfo(name = "require_note")
    val requireNote: Boolean = false,
    @ColumnInfo(name = "require_issue")
    val requireIssue: Boolean = true,
    @ColumnInfo(name = "require_suggestion")
    val requireSuggestion: Boolean = false,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    val status: String = STATUS_PENDING,
    @ColumnInfo(name = "photo_count")
    val photoCount: Int = 0,
    val result: String = "",
    val note: String = "",
    val issue: String = "",
    val suggestion: String = "",
    @ColumnInfo(name = "responsible_party")
    val responsibleParty: String = "",
    @ColumnInfo(name = "rectification_deadline")
    val rectificationDeadline: String = "",
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_DONE = "pass"
        const val STATUS_FAILED = "fail"
        const val STATUS_NOT_APPLICABLE = "not_applicable"
        const val STATUS_RECTIFY = "rectify"
        const val STATUS_RECTIFIED = "rectified"
        const val STATUS_RECHECK_PASS = "recheck_pass"
    }
}
