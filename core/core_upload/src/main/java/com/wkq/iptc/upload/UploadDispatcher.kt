package com.wkq.iptc.upload

import android.util.Log

class UploadDispatcher(
    clients: List<UploadClient>
) {
    private val clientMap = clients.associateBy { it.protocol }

    suspend fun upload(
        task: UploadTask,
        profile: UploadServerProfile,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)? = null
    ): UploadResult {
        val client = clientMap[profile.protocol]
            ?: run {
                Log.e(TAG, "upload unsupported protocol=${profile.protocol.name} record=${task.recordId}")
                return UploadResult.Failure(
                    UploadFailure(
                        code = UploadErrorCode.UNSUPPORTED_PROTOCOL,
                        message = "Unsupported protocol: ${profile.protocol}",
                        retryable = false
                    )
                )
            }
        Log.d(
            TAG,
            "dispatcher upload start record=${task.recordId} protocol=${profile.protocol.name} " +
                "host=${profile.config.host} remoteDir=${profile.config.remoteDir} file=${task.fileName}"
        )
        val result = client.upload(task, profile, onProgress)
        Log.d(TAG, "dispatcher upload result record=${task.recordId} result=${result.javaClass.simpleName}")
        return result
    }

    suspend fun testConnection(profile: UploadServerProfile): UploadResult {
        val client = clientMap[profile.protocol]
            ?: return UploadResult.Failure(
                UploadFailure(
                    code = UploadErrorCode.UNSUPPORTED_PROTOCOL,
                    message = "Unsupported protocol: ${profile.protocol}",
                    retryable = false
                )
            )
        return client.testConnection(profile)
    }

    private companion object {
        const val TAG = "SiteReportUpload"
    }
}

