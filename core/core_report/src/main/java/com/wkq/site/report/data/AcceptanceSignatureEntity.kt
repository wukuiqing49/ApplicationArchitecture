package com.wkq.site.report.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "acceptance_signature",
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
        Index(value = ["inspection_id", "role"], unique = true)
    ]
)
data class AcceptanceSignatureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "inspection_id")
    val inspectionId: Long,
    val role: String,
    @ColumnInfo(name = "signer_name")
    val signerName: String,
    @ColumnInfo(name = "company_name")
    val companyName: String = "",
    val position: String = "",
    @ColumnInfo(name = "signature_image_path")
    val signatureImagePath: String = "",
    @ColumnInfo(name = "signed_at")
    val signedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "location_text")
    val locationText: String = "",
    val note: String = ""
) {
    companion object {
        const val ROLE_INSPECTOR = "inspector"
        const val ROLE_CLIENT = "client"
        const val ROLE_CONTRACTOR = "contractor"
        const val ROLE_SUPERVISOR = "supervisor"
    }
}
