package com.wkq.iptc.data.photo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PhotoRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PhotoRecordEntity): Long

    @Query("SELECT * FROM photo_record ORDER BY captured_at DESC")
    suspend fun queryAll(): List<PhotoRecordEntity>

    @Query("SELECT * FROM photo_record WHERE account_id = :accountId ORDER BY updated_at DESC, captured_at DESC")
    suspend fun queryByAccount(accountId: String): List<PhotoRecordEntity>

    @Query("SELECT * FROM photo_record WHERE id = :id LIMIT 1")
    suspend fun queryById(id: Long): PhotoRecordEntity?

    @Delete
    suspend fun delete(record: PhotoRecordEntity)

    @Update
    suspend fun update(record: PhotoRecordEntity)

    @Query("UPDATE photo_record SET upload_resume_offset = :offset WHERE id = :id")
    suspend fun updateResumeOffset(id: Long, offset: Long)

    @Query("DELETE FROM photo_record")
    suspend fun deleteAll()

    @Query("SELECT * FROM photo_record WHERE upload_status = 'queued' OR upload_status = 'failed' ORDER BY captured_at ASC")
    suspend fun queryPendingUploads(): List<PhotoRecordEntity>

    @Query("SELECT * FROM photo_record WHERE upload_status = 'queued' OR upload_status = 'connecting' OR upload_status = 'uploading' OR upload_status = 'paused'")
    fun observePendingUploads(): kotlinx.coroutines.flow.Flow<List<PhotoRecordEntity>>

    @Query("SELECT * FROM photo_record WHERE account_id = :accountId AND (upload_status = 'queued' OR upload_status = 'connecting' OR upload_status = 'uploading' OR upload_status = 'paused')")
    fun observePendingUploads(accountId: String): kotlinx.coroutines.flow.Flow<List<PhotoRecordEntity>>

    @Query("SELECT * FROM photo_record WHERE photo_source = :source ORDER BY captured_at DESC")
    suspend fun queryBySource(source: String): List<PhotoRecordEntity>

    @Query("SELECT * FROM photo_record WHERE account_id = :accountId AND photo_source = :source ORDER BY updated_at DESC, captured_at DESC")
    suspend fun queryByAccountAndSource(accountId: String, source: String): List<PhotoRecordEntity>

    @Query("SELECT * FROM photo_record WHERE account_id = :accountId AND process_type = :processType ORDER BY updated_at DESC, captured_at DESC")
    suspend fun queryByProcessType(accountId: String, processType: String): List<PhotoRecordEntity>

    @Query("SELECT * FROM photo_record WHERE account_id = :accountId AND source_key = :sourceKey AND process_type = :processType LIMIT 1")
    suspend fun queryProcessRecord(
        accountId: String,
        sourceKey: String,
        processType: String
    ): PhotoRecordEntity?

    @Query("SELECT * FROM photo_record WHERE account_id = :accountId AND upload_status != 'local_only' ORDER BY updated_at DESC, captured_at DESC")
    suspend fun queryUploadRecords(accountId: String): List<PhotoRecordEntity>

    @Query("SELECT * FROM photo_record WHERE account_id = :accountId AND (upload_status = 'queued' OR upload_status = 'failed') ORDER BY captured_at ASC")
    suspend fun queryPendingUploads(accountId: String): List<PhotoRecordEntity>
}
