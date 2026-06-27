package com.wkq.site.report.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inspection_photo",
    foreignKeys = [
        ForeignKey(
            entity = InspectionRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspection_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InspectionItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["inspection_id"]),
        Index(value = ["item_id"])
    ]
)
data class InspectionPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "inspection_id")
    val inspectionId: Long,
    @ColumnInfo(name = "item_id")
    val itemId: Long,
    @ColumnInfo(name = "file_path")
    val filePath: String,
    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,
    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,
    @ColumnInfo(name = "location_accuracy")
    val locationAccuracy: Float? = null,
    @ColumnInfo(name = "location_provider")
    val locationProvider: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
