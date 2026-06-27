package com.wkq.iptc.upload

import java.util.UUID

data class UploadServerProfile(
    /** 鍞竴鏍囪瘑锛岃嚜鍔ㄧ敓鎴愶紝涓嶅彲閲嶅 */
    val id: String = UUID.randomUUID().toString(),
    /** 鐢ㄦ埛鑷畾涔夊娉ㄥ悕锛涗负绌烘椂 UI 鑷姩鏄剧ず"鍗忚路host" */
    val name: String = "",
    val protocol: UploadProtocolType = UploadProtocolType.SFTP,
    val autoUploadEnabled: Boolean = false,
    val keepLocalCopyAfterUpload: Boolean = true,
    val config: UploadServerConfig = SftpConfig()
) {

    fun isReady(): Boolean {
        val commonReady = config.host.isNotBlank() &&
            (config is HttpConfig || config.remoteDir.isNotBlank())
        if (!commonReady) {
            return false
        }
        return when (val current = config) {
            is FtpConfig -> current.username.isNotBlank() && current.password.isNotBlank()
            is FtpsConfig -> current.username.isNotBlank() && current.password.isNotBlank()
            is SftpConfig -> current.username.isNotBlank() && current.password.isNotBlank()
            is HttpConfig -> true
            is SmbConfig -> true
            is WebDavConfig -> true
        }
    }

    fun defaultPort(): Int {
        return when (protocol) {
            UploadProtocolType.FTP -> 21
            UploadProtocolType.FTPS -> 21
            UploadProtocolType.SFTP -> 22
            UploadProtocolType.HTTP -> 80
            UploadProtocolType.SMB -> 445
            // WebDAV锛欻TTP 鐢?80锛孒TTPS 鐢?443
            UploadProtocolType.WEBDAV -> if ((config as? WebDavConfig)?.useHttps == true) 443 else 80
        }
    }
}

