package com.wkq.iptc.upload

interface UploadClient {
    val protocol: UploadProtocolType

    suspend fun upload(
        task: UploadTask,
        profile: UploadServerProfile,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)? = null
    ): UploadResult

    suspend fun testConnection(profile: UploadServerProfile): UploadResult
}

