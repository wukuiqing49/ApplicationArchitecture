package com.wkq.site.report.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signature_profile")
data class SignatureProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "company_name")
    val companyName: String = "",
    @ColumnInfo(name = "company_address")
    val companyAddress: String = "",
    @ColumnInfo(name = "company_phone")
    val companyPhone: String = "",
    @ColumnInfo(name = "company_logo_path")
    val companyLogoPath: String = "",
    @ColumnInfo(name = "submitter_name")
    val submitterName: String = "",
    @ColumnInfo(name = "submitter_role")
    val submitterRole: String = "",
    @ColumnInfo(name = "signature_image_path")
    val signatureImagePath: String = "",
    @ColumnInfo(name = "selected")
    val selected: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
