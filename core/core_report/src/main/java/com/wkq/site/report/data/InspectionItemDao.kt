package com.wkq.site.report.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface InspectionItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<InspectionItemEntity>)

    @Update
    suspend fun update(item: InspectionItemEntity)

    @Query("SELECT * FROM inspection_item WHERE inspection_id = :inspectionId ORDER BY sort_order ASC, id ASC")
    suspend fun queryByInspectionId(inspectionId: Long): List<InspectionItemEntity>

    @Query("SELECT * FROM inspection_item WHERE id = :id LIMIT 1")
    suspend fun queryById(id: Long): InspectionItemEntity?

    @Query("SELECT COUNT(*) FROM inspection_item WHERE inspection_id = :inspectionId")
    suspend fun countByInspectionId(inspectionId: Long): Int

    @Query("SELECT COUNT(*) FROM inspection_item WHERE inspection_id = :inspectionId AND (status = :passStatus OR status = :naStatus OR status = :recheckPassStatus)")
    suspend fun countDone(
        inspectionId: Long,
        passStatus: String = InspectionItemEntity.STATUS_DONE,
        naStatus: String = InspectionItemEntity.STATUS_NOT_APPLICABLE,
        recheckPassStatus: String = InspectionItemEntity.STATUS_RECHECK_PASS
    ): Int

    @Query("SELECT COUNT(*) FROM inspection_item WHERE inspection_id = :inspectionId AND status != :pendingStatus")
    suspend fun countCompleted(inspectionId: Long, pendingStatus: String = InspectionItemEntity.STATUS_PENDING): Int
}
