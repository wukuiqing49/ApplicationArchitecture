package com.wkq.iptc.data.photo

import android.content.Context

class PhotoRecordRepository private constructor(context: Context) {

    private val dao = PhotoRecordDatabase.getInstance(context).photoRecordDao()

    suspend fun insert(record: PhotoRecordEntity): Long = dao.insert(record)

    suspend fun queryAll(): List<PhotoRecordEntity> = dao.queryAll()

    suspend fun queryByAccount(accountId: String): List<PhotoRecordEntity> = dao.queryByAccount(accountId)

    suspend fun queryBySource(source: String): List<PhotoRecordEntity> = dao.queryBySource(source)

    suspend fun queryByAccountAndSource(accountId: String, source: String): List<PhotoRecordEntity> =
        dao.queryByAccountAndSource(accountId, source)

    suspend fun queryByProcessType(accountId: String, processType: String): List<PhotoRecordEntity> =
        dao.queryByProcessType(accountId, processType)

    suspend fun queryById(id: Long): PhotoRecordEntity? = dao.queryById(id)

    suspend fun update(record: PhotoRecordEntity) = dao.update(record)

    suspend fun delete(record: PhotoRecordEntity) = dao.delete(record)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun queryPendingUploads(): List<PhotoRecordEntity> = dao.queryPendingUploads()

    suspend fun queryPendingUploads(accountId: String): List<PhotoRecordEntity> =
        dao.queryPendingUploads(accountId)

    suspend fun queryUploadRecords(accountId: String): List<PhotoRecordEntity> =
        dao.queryUploadRecords(accountId)

    suspend fun upsertProcessRecord(record: PhotoRecordEntity): Long {
        val existing = dao.queryProcessRecord(
            accountId = record.accountId,
            sourceKey = record.sourceKey,
            processType = record.processType
        )
        return if (existing == null || record.sourceKey.isBlank()) {
            dao.insert(record)
        } else {
            dao.update(
                record.copy(
                    id = existing.id,
                    uploadRemotePath = record.uploadRemotePath ?: existing.uploadRemotePath,
                    uploadRetryCount = if (record.uploadRetryCount != 0) record.uploadRetryCount else existing.uploadRetryCount,
                    uploadLastAttemptAt = record.uploadLastAttemptAt ?: existing.uploadLastAttemptAt,
                    uploadResumeOffset = 0L
                )
            )
            existing.id
        }
    }

    suspend fun updateResumeOffset(id: Long, offset: Long) = dao.updateResumeOffset(id, offset)

    suspend fun updateUploadState(
        id: Long,
        status: String,
        error: String?,
        remotePath: String? = null,
        retryCount: Int? = null,
        lastAttemptAt: Long? = null
    ) {
        val current = dao.queryById(id) ?: return
        dao.update(
            current.copy(
                uploadStatus = status,
                uploadError = error,
                uploadRemotePath = remotePath ?: current.uploadRemotePath,
                uploadRetryCount = retryCount ?: current.uploadRetryCount,
                uploadLastAttemptAt = lastAttemptAt ?: current.uploadLastAttemptAt,
                uploadResumeOffset = if (status == "uploaded") 0L else current.uploadResumeOffset
            )
        )
    }

    fun observePendingUploads(): kotlinx.coroutines.flow.Flow<List<PhotoRecordEntity>> {
        return dao.observePendingUploads()
    }

    fun observePendingUploads(accountId: String): kotlinx.coroutines.flow.Flow<List<PhotoRecordEntity>> {
        return dao.observePendingUploads(accountId)
    }

    companion object {
        @Volatile
        private var instance: PhotoRecordRepository? = null

        fun getInstance(context: Context): PhotoRecordRepository {
            return instance ?: synchronized(this) {
                instance ?: PhotoRecordRepository(context).also { instance = it }
            }
        }
    }
}
