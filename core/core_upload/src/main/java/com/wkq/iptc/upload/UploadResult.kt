package com.wkq.iptc.upload

sealed class UploadResult {
    data class Success(val remotePath: String) : UploadResult()
    data class Failure(val error: UploadFailure) : UploadResult()
}

