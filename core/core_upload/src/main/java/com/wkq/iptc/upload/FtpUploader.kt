package com.wkq.iptc.upload

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.ByteArrayInputStream
import java.time.Duration

class FtpUploader(
    private val context: Context
) : UploadClient {

    private companion object {
        private const val TAG = "FtpUploader"
    }

    override val protocol: UploadProtocolType = UploadProtocolType.FTP

    override suspend fun upload(
        task: UploadTask,
        profile: UploadServerProfile,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)?
    ): UploadResult {
        val config = profile.config as? FtpConfig
            ?: return invalidConfig("FTP profile config is invalid")
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
        val config = profile.config as? FtpConfig
            ?: return invalidConfig("FTP profile config is invalid")
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
        config: FtpConfig,
        sourceUri: String?,
        fileName: String?,
        resumeOffset: Long,
        validateOnly: Boolean,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)?
    ): UploadResult {
        if (config.host.isBlank() || config.username.isBlank() || config.password.isBlank()) {
            return invalidConfig("FTP config is incomplete")
        }
        val client = FTPClient()
        return try {
            client.connectTimeout = config.connectTimeoutMs
            client.defaultTimeout = config.connectTimeoutMs
            client.dataTimeout = Duration.ofMillis(config.readTimeoutMs.toLong())
            client.controlEncoding = Charsets.UTF_8.name()
            client.connect(config.host, config.port)
            if (!FTPReply.isPositiveCompletion(client.replyCode)) {
                return connectionFailure("FTP server refused connection: ${client.replyString}")
            }
            if (!client.login(config.username, config.password)) {
                return authFailure("FTP authentication failed")
            }
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
                val safeFileName = fileName ?: return invalidConfig("FTP upload file name is missing")
                val safeSourceUri = sourceUri ?: return invalidConfig("FTP upload source is missing")
                val remotePath = buildRemotePath(config.remoteDir, safeFileName)
                val partName = "$safeFileName.part"
                val uri = Uri.parse(safeSourceUri)
                val localSize = getUriLength(context, uri)
                if (localSize < 0) {
                    return transferFailure("FTP upload source size is invalid", retryable = false)
                }
                
                // 閺屻儴顕楁潻婊咁伂 .part 閺傚洣娆㈠韫炊婢堆冪毈
                val remotePartSize = runCatching {
                    client.listFiles(partName).firstOrNull()?.size ?: 0L
                }.getOrElse { 0L }
                val actualOffset = minOf(remotePartSize, localSize)
                
                Log.i(TAG, "FTP upload: file=$safeFileName, local=${localSize}B, " +
                    "resumeOffset=${resumeOffset}B, remotePartSize=${remotePartSize}B, actualOffset=${actualOffset}B")
                
                val success = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
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
                }.getOrElse {
                    Log.w(
                        TAG,
                        "FTP .part upload threw: ${it.javaClass.name}: ${it.message}; " +
                            "replyCode=${client.replyCode}, reply=${client.replyString}"
                    )
                    false
                }
                
                if (!success) {
                    val directSuccess = uploadDirect(client, uri, safeFileName, localSize, onProgress)
                    if (!directSuccess) {
                        return transferFailure(
                            "FTP upload failed: ${client.replyString}",
                            retryable = isRetryableReply(client.replyCode)
                        )
                    }
                    Log.i(TAG, "FTP direct upload success after .part failed: remotePath=$remotePath, size=$localSize")
                    return UploadResult.Success(remotePath)
                }
                
                // 閺嶏繝鐛欐径褍鐨?
                val remoteSize = client.listFiles(partName).firstOrNull()?.size ?: -1L
                if (remoteSize != localSize) {
                    return transferFailure(
                        "FTP remote file size mismatch: path=$partName, local=$localSize, remote=$remoteSize",
                        retryable = true
                    )
                }
                
                // 閺嶏繝鐛欓柅姘崇箖閿涘矂鍣搁崨钘夋倳娑撶儤顒滃蹇旀瀮娴?
                if (!client.rename(partName, safeFileName)) {
                    val directSuccess = uploadDirect(client, uri, safeFileName, localSize, onProgress)
                    if (!directSuccess) {
                        return transferFailure(
                            "FTP rename .part to final failed: ${client.replyString}",
                            retryable = true
                        )
                    }
                    Log.i(TAG, "FTP direct upload success after rename failed: remotePath=$remotePath, size=$localSize")
                    return UploadResult.Success(remotePath)
                }
                Log.i(TAG, "FTP upload success: remotePath=$remotePath, size=$localSize")
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
            translateThrowable("FTP", throwable)
        } finally {
            disconnectQuietly(client)
        }
    }

    private fun ensureRemoteDir(client: FTPClient, remoteDir: String) {
        val normalized = remoteDir.trim().replace('\\', '/')
        if (normalized.isBlank() || normalized == ".") {
            return
        }

        if (normalized.startsWith("/")) {
            if (!client.changeWorkingDirectory("/")) {
                throw RemoteDirectoryAccessException("Unable to enter FTP root directory")
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

    private fun disconnectQuietly(client: FTPClient) {
        runCatching {
            if (client.isConnected) {
                client.logout()
                client.disconnect()
            }
        }
    }

    private fun testWritable(client: FTPClient): UploadResult.Failure? {
        val fileName = "press_iptc_test_${System.currentTimeMillis()}.tmp"
        val bytes = "ok".toByteArray(Charsets.UTF_8)
        return runCatching {
            client.restartOffset = 0L
            val success = ByteArrayInputStream(bytes).use { input ->
                client.storeFile(fileName, input)
            }
            if (!success) {
                return transferFailure(
                    "FTP upload test failed: ${client.replyString}",
                    retryable = isRetryableReply(client.replyCode)
                ) as UploadResult.Failure
            }
            runCatching { client.deleteFile(fileName) }
            null
        }.getOrElse {
            Log.w(
                TAG,
                "FTP upload test threw: ${it.javaClass.name}: ${it.message}; " +
                    "replyCode=${client.replyCode}, reply=${client.replyString}"
            )
            UploadResult.Failure(
                UploadFailure(
                    code = UploadErrorCode.TRANSFER_FAILED,
                    message = "FTP upload test failed: ${client.replyString.ifBlank { it.message.orEmpty() }}",
                    retryable = isRetryableReply(client.replyCode)
                )
            )
        }
    }

    private fun uploadDirect(
        client: FTPClient,
        uri: Uri,
        remotePath: String,
        localSize: Long,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)?
    ): Boolean {
        return runCatching {
            client.restartOffset = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                val progressInput = ProgressInputStream(input, 0L) { bytesWritten ->
                    onProgress?.invoke(bytesWritten, localSize)
                }
                client.storeFile(remotePath, progressInput)
            } ?: false
        }.getOrElse {
            Log.w(
                TAG,
                "FTP direct upload threw: ${it.javaClass.name}: ${it.message}; " +
                    "replyCode=${client.replyCode}, reply=${client.replyString}"
            )
            false
        }.also { success ->
            if (!success) {
                Log.w(
                    TAG,
                    "FTP direct upload failed: remotePath=$remotePath, " +
                        "replyCode=${client.replyCode}, reply=${client.replyString}"
                )
            }
        }
    }

    private fun isRetryableReply(replyCode: Int): Boolean {
        return replyCode >= 400 && replyCode < 500
    }
}



