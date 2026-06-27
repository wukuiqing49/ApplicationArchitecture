package com.wkq.site.report.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface InspectionPhotoDao {

    @Insert
    suspend fun insert(photo: InspectionPhotoEntity): Long

    @Query("SELECT * FROM inspection_photo WHERE inspection_id = :inspectionId ORDER BY created_at ASC")
    suspend fun queryByInspectionId(inspectionId: Long): List<InspectionPhotoEntity>

    @Query("SELECT * FROM inspection_photo WHERE item_id = :itemId ORDER BY created_at ASC")
    suspend fun queryByItemId(itemId: Long): List<InspectionPhotoEntity>

    @Query("SELECT * FROM inspection_photo WHERE id = :photoId LIMIT 1")
    suspend fun queryById(photoId: Long): InspectionPhotoEntity?

    @Query("SELECT COUNT(*) FROM inspection_photo WHERE inspection_id = :inspectionId")
    suspend fun countByInspectionId(inspectionId: Long): Int

    @Query("SELECT COUNT(*) FROM inspection_photo WHERE item_id = :itemId")
    suspend fun countByItemId(itemId: Long): Int

    @Query("DELETE FROM inspection_photo WHERE id = :photoId")
    suspend fun deleteById(photoId: Long)
}
