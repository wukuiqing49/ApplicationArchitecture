package com.wkq.iptc.upload

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.UnknownServiceException

internal fun invalidConfig(message: String): UploadResult.Failure {
    return UploadResult.Failure(
        UploadFailure(
            code = UploadErrorCode.INVALID_CONFIG,
            message = message,
            retryable = false
        )
    )
}

internal fun authFailure(message: String): UploadResult.Failure {
    return UploadResult.Failure(
        UploadFailure(
            code = UploadErrorCode.AUTH_FAILED,
            message = message,
            retryable = false
        )
    )
}

internal fun connectionFailure(message: String): UploadResult.Failure {
    return UploadResult.Failure(
        UploadFailure(
            code = UploadErrorCode.CONNECTION_FAILED,
            message = message,
            retryable = true
        )
    )
}

internal fun transferFailure(message: String, retryable: Boolean): UploadResult.Failure {
    return UploadResult.Failure(
        UploadFailure(
            code = UploadErrorCode.TRANSFER_FAILED,
            message = message,
            retryable = retryable
        )
    )
}

internal fun translateThrowable(protocol: String, throwable: Throwable): UploadResult.Failure {
    val message = throwable.message.orEmpty().ifBlank { "$protocol upload failed" }
    return when (throwable) {
        is UnknownHostException -> connectionFailure("$protocol host not found")
        is SocketTimeoutException -> UploadResult.Failure(
            UploadFailure(
                code = UploadErrorCode.TIMEOUT,
                message = "$protocol timeout: $message",
                retryable = true
            )
        )
        is RemoteDirectoryAccessException -> UploadResult.Failure(
            UploadFailure(
                code = UploadErrorCode.DIRECTORY_ACCESS_DENIED,
                message = "$protocol remote directory error: $message",
                retryable = false
            )
        )
        is UnknownServiceException -> {
            if (message.contains("CLEARTEXT", ignoreCase = true)) {
                invalidConfig("$protocol HTTP 明文连接被系统网络安全策略拦截，请允许明文流量或改用 HTTPS")
            } else {
                connectionFailure("$protocol service error: $message")
            }
        }
        is IOException -> UploadResult.Failure(
            UploadFailure(
                code = UploadErrorCode.IO_ERROR,
                message = "$protocol I/O error: $message",
                retryable = true
            )
        )
        is IllegalArgumentException, is IllegalStateException -> invalidConfig(message)
        else -> UploadResult.Failure(
            UploadFailure(
                code = UploadErrorCode.UNKNOWN,
                message = "$protocol error: $message",
                retryable = false
            )
        )
    }
}

internal class RemoteDirectoryAccessException(message: String) : IOException(message)

