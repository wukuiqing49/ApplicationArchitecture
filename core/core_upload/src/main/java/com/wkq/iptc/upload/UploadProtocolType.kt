package com.wkq.iptc.upload

enum class UploadProtocolType {
    FTP,
    FTPS,
    SFTP,
    HTTP,
    SMB,
    WEBDAV;

    companion object {
        fun fromValue(value: String?): UploadProtocolType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SFTP
        }
    }
}

