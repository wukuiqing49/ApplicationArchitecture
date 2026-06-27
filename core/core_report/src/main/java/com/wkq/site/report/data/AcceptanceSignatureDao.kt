package com.wkq.site.report.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AcceptanceSignatureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(signature: AcceptanceSignatureEntity): Long

    @Query("SELECT * FROM acceptance_signature WHERE inspection_id = :inspectionId ORDER BY signed_at ASC")
    suspend fun queryByInspectionId(inspectionId: Long): List<AcceptanceSignatureEntity>

    @Query("DELETE FROM acceptance_signature WHERE inspection_id = :inspectionId")
    suspend fun deleteByInspectionId(inspectionId: Long)
}
