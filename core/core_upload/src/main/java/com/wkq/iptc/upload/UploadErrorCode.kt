package com.wkq.iptc.upload

enum class UploadErrorCode(val wireValue: String) {
    UNSUPPORTED_PROTOCOL("unsupported_protocol"),
    INVALID_CONFIG("invalid_config"),
    AUTH_FAILED("auth_failed"),
    CONNECTION_FAILED("connection_failed"),
    DIRECTORY_ACCESS_DENIED("directory_access_denied"),
    HOST_KEY_REJECTED("host_key_rejected"),
    HOST_KEY_CHANGED("host_key_changed"),
    TRANSFER_FAILED("transfer_failed"),
    TIMEOUT("timeout"),
    IO_ERROR("io_error"),
    UNKNOWN("unknown")
}

