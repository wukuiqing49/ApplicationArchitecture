package com.wkq.iptc.upload

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import org.apache.commons.net.util.TrustManagerUtils
import java.io.ByteArrayInputStream
import java.time.Duration

class FtpsUploader(
    private val context: Context
) : UploadClient {

    private companion object {
        private const val TAG = "FtpsUploader"
    }

    override val protocol: UploadProtocolType = UploadProtocolType.FTPS

    override suspend fun upload(
        task: UploadTask,
        profile: UploadServerProfile,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)?
    ): UploadResult {
        val config = profile.config as? FtpsConfig
            ?: return invalidConfig("FTPS profile config is invalid")
        return withContext(Dispatchers.IO) {
            runTransfer(
                config = config,
                sourceUri = task.recordUri,
                fileName = task.fileName,
                resumeOffset = task.resumeOffset,
                validateOnly = false,
                onProgress = onProgress
            )
        }
    }

    override suspend fun testConnection(profile: UploadServerProfile): UploadResult {
        val config = profile.config as? FtpsConfig
            ?: return invalidConfig("FTPS profile config is invalid")
        return withContext(Dispatchers.IO) {
            runTransfer(
                config = config,
                sourceUri = null,
                fileName = null,
                resumeOffset = 0L,
                validateOnly = true,
                onProgress = null
            )
        }
    }

    private fun runTransfer(
        config: FtpsConfig,
        sourceUri: String?,
        fileName: String?,
        resumeOffset: Long,
        validateOnly: Boolean,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)?
    ): UploadResult {
        if (config.host.isBlank() || config.username.isBlank() || config.password.isBlank()) {
            return invalidConfig("FTPS config is incomplete")
        }
        val client = FTPSClient(config.securityMode == FtpsSecurityMode.IMPLICIT)
        return try {
            if (config.allowInsecureCertificate) {
                client.trustManager = TrustManagerUtils.getAcceptAllTrustManager()
                client.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
            }
            client.connectTimeout = config.connectTimeoutMs
            client.defaultTimeout = config.connectTimeoutMs
            client.dataTimeout = Duration.ofMillis(config.readTimeoutMs.toLong())
            client.controlEncoding = Charsets.UTF_8.name()
            client.connect(config.host, config.port)
            if (!FTPReply.isPositiveCompletion(client.replyCode)) {
                return connectionFailure("FTPS server refused connection: ${client.replyString}")
            }
            if (!client.login(config.username, config.password)) {
                return authFailure("FTPS authentication failed")
            }
            client.execPBSZ(0)
            client.execPROT("P")
            // 閸ュ墽澧栭弬鍥︽韫囧懘銆忕挧棰佺癌鏉╂稑鍩楁导鐘虹翻閿涘矂浼╅崗?ASCII 濡€崇础閻潙娼栫€涙濡ù浣碘偓?
            client.setFileType(FTP.BINARY_FILE_TYPE)
            if (config.passiveMode) {
                client.setIpAddressFromPasvResponse(false)
                client.enterLocalPassiveMode()
            } else {
                client.enterLocalActiveMode()
            }
            ensureRemoteDir(client, config.remoteDir)
            if (validateOnly) {
                testWritable(client)?.let { return it }
                UploadResult.Success(config.remoteDir)
            } else {
                val safeFileName = fileName ?: return invalidConfig("FTPS upload file name is missing")
                val safeSourceUri = sourceUri ?: return invalidConfig("FTPS upload source is missing")
                val remotePath = buildRemotePath(config.remoteDir, safeFileName)
                val partName = "$safeFileName.part"
                val uri = Uri.parse(safeSourceUri)
                val localSize = getUriLength(context, uri)
                if (localSize < 0) {
                    return transferFailure("FTPS upload source size is invalid", retryable = false)
                }

                val remotePartSize = runCatching {
                    client.listFiles(partName).firstOrNull()?.size ?: 0L
                }.getOrElse { 0L }
                val actualOffset = minOf(remotePartSize, localSize)

                Log.i(TAG, "FTPS upload: file=$safeFileName, local=${localSize}B, " +
                    "resumeOffset=${resumeOffset}B, remotePartSize=${remotePartSize}B, actualOffset=${actualOffset}B")

                val success = context.contentResolver.openInputStream(uri)?.use { input ->
                    if (actualOffset > 0L) {
                        client.restartOffset = actualOffset
                        input.safeSkip(actualOffset)

                        val progressInput = ProgressInputStream(input, actualOffset) { bytesWritten ->
                            onProgress?.invoke(bytesWritten, localSize)
                        }
                        client.appendFile(partName, progressInput)
                    } else {
                        val progressInput = ProgressInputStream(input, 0L) { bytesWritten ->
                            onProgress?.invoke(bytesWritten, localSize)
                        }
                        client.storeFile(partName, progressInput)
                    }
                } ?: false

                if (!success) {
                    Log.w(
                        TAG,
                        "FTPS .part upload failed: replyCode=${client.replyCode}, reply=${client.replyString}"
                    )
                    val directSuccess = uploadDirect(client, uri, safeFileName, localSize, onProgress)
                    if (!directSuccess) {
                        return transferFailure(
                            "FTPS upload failed: ${client.replyString}",
                            retryable = client.replyCode >= 400 && client.replyCode < 500
                        )
                    }
                    Log.i(TAG, "FTPS direct upload success after .part failed: remotePath=$remotePath, size=$localSize")
                    return UploadResult.Success(remotePath)
                }

                val remoteSize = client.listFiles(partName).firstOrNull()?.size ?: -1L
                if (remoteSize != localSize) {
                    return transferFailure(
                        "FTPS remote file size mismatch: path=$partName, local=$localSize, remote=$remoteSize",
                        retryable = true
                    )
                }
                if (!client.rename(partName, safeFileName)) {
                    val directSuccess = uploadDirect(client, uri, safeFileName, localSize, onProgress)
                    if (!directSuccess) {
                        return transferFailure(
                            "FTPS rename .part to final failed: ${client.replyString}",
                            retryable = true
                        )
                    }
                    Log.i(TAG, "FTPS direct upload success after rename failed: remotePath=$remotePath, size=$localSize")
                    return UploadResult.Success(remotePath)
                }
                Log.i(TAG, "FTPS upload success: remotePath=$remotePath, size=$localSize")
                UploadResult.Success(remotePath)
            }
        } catch (throwable: Throwable) {
            Log.e(
                TAG,
                "runTransfer failed: validateOnly=$validateOnly, host=${config.host}, " +
                    "port=${config.port}, remoteDir=${config.remoteDir}, " +
                    "type=${throwable.javaClass.name}, message=${throwable.message.orEmpty()}\n" +
                    throwable.stackTraceToString()
            )
            translateThrowable("FTPS", throwable)
        } finally {
            disconnectQuietly(client)
        }
    }

    private fun ensureRemoteDir(client: FTPSClient, remoteDir: String) {
        val normalized = remoteDir.trim().replace('\\', '/')
        if (normalized.isBlank() || normalized == ".") {
            return
        }

        if (normalized.startsWith("/")) {
            if (!client.changeWorkingDirectory("/")) {
                throw RemoteDirectoryAccessException("Unable to enter FTPS root directory")
            }
        }

        normalized.trim('/').split('/').forEach { segment ->
            if (segment.isBlank()) return@forEach
            if (!client.changeWorkingDirectory(segment)) {
                if (!client.makeDirectory(segment)) {
                    throw RemoteDirectoryAccessException("Unable to create remote directory: $segment")
                }
                if (!client.changeWorkingDirectory(segment)) {
                    throw RemoteDirectoryAccessException("Unable to enter remote directory: $segment")
                }
            }
        }
    }

    private fun testWritable(client: FTPSClient): UploadResult.Failure? {
        val fileName = "press_iptc_test_${System.currentTimeMillis()}.tmp"
        val bytes = "ok".toByteArray(Charsets.UTF_8)
        return runCatching {
            client.restartOffset = 0L
            val success = ByteArrayInputStream(bytes).use { input ->
                client.storeFile(fileName, input)
            }
            if (!success) {
                return transferFailure(
                    "FTPS upload test failed: ${client.replyString}",
                    retryable = client.replyCode >= 400 && client.replyCode < 500
                ) as UploadResult.Failure
            }
            runCatching { client.deleteFile(fileName) }
            null
        }.getOrElse {
            Log.w(
                TAG,
                "FTPS upload test threw: ${it.javaClass.name}: ${it.message}; " +
                    "replyCode=${client.replyCode}, reply=${client.replyString}"
            )
            UploadResult.Failure(
                UploadFailure(
                    code = UploadErrorCode.TRANSFER_FAILED,
                    message = "FTPS upload test failed: ${client.replyString.ifBlank { it.message.orEmpty() }}",
                    retryable = client.replyCode >= 400 && client.replyCode < 500
                )
            )
        }
    }

    private fun uploadDirect(
        client: FTPSClient,
        uri: Uri,
        remotePath: String,
        localSize: Long,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)?
    ): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val progressInput = ProgressInputStream(input, 0L) { bytesWritten ->
                    onProgress?.invoke(bytesWritten, localSize)
                }
                client.storeFile(remotePath, progressInput)
            } ?: false
        }.getOrElse {
            Log.w(
                TAG,
                "FTPS direct upload threw: ${it.javaClass.name}: ${it.message}; " +
                    "replyCode=${client.replyCode}, reply=${client.replyString}"
            )
            false
        }.also { success ->
            if (!success) {
                Log.w(
                    TAG,
                    "FTPS direct upload failed: remotePath=$remotePath, " +
                        "replyCode=${client.replyCode}, reply=${client.replyString}"
                )
            }
        }
    }

    private fun disconnectQuietly(client: FTPSClient) {
        runCatching {
            if (client.isConnected) {
                client.logout()
                client.disconnect()
            }
        }
    }
}



