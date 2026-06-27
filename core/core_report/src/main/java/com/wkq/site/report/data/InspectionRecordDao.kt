package com.wkq.site.report.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: InspectionRecordEntity): Long

    @Update
    suspend fun update(record: InspectionRecordEntity)

    @Query("SELECT * FROM inspection_record ORDER BY updated_at DESC")
    suspend fun queryAll(): List<InspectionRecordEntity>

    @Query("SELECT * FROM inspection_record ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<InspectionRecordEntity>>

    @Query("SELECT * FROM inspection_record WHERE id = :id LIMIT 1")
    suspend fun queryById(id: Long): InspectionRecordEntity?

    @Query("DELETE FROM inspection_record WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE inspection_record SET status = :status, progress = :progress, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateProgress(id: Long, status: String, progress: Int, updatedAt: Long = System.currentTimeMillis())

    @Query(
        "UPDATE inspection_record SET report_file_path = :reportFilePath, upload_status = :uploadStatus, " +
            "upload_remote_path = :uploadRemotePath, upload_error = :uploadError, updated_at = :updatedAt WHERE id = :id"
    )
    suspend fun updateUploadState(
        id: Long,
        reportFilePath: String,
        uploadStatus: String,
        uploadRemotePath: String,
        uploadError: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT COUNT(*) FROM inspection_record")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM inspection_record WHERE status != :doneStatus")
    suspend fun countActive(doneStatus: String = InspectionRecordEntity.STATUS_DONE): Int

    @Query("SELECT COUNT(*) FROM inspection_record WHERE status = :doneStatus")
    suspend fun countDone(doneStatus: String = InspectionRecordEntity.STATUS_DONE): Int
}
