package com.wkq.iptc.upload

data class UploadFailure(
    val code: UploadErrorCode,
    val message: String,
    val retryable: Boolean
)

