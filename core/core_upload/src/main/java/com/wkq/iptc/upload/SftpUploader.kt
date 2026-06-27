package com.wkq.iptc.upload

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.hierynomus.sshj.transport.kex.DHGroups
import com.hierynomus.sshj.transport.kex.ExtInfoClientFactory
import com.hierynomus.sshj.transport.kex.ExtendedDHGroups
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.kex.DHGexSHA1
import net.schmizz.sshj.transport.kex.DHGexSHA256
import net.schmizz.sshj.transport.kex.ECDHNistP
import net.schmizz.sshj.transport.TransportException
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.UserAuthException

import java.io.IOException
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.security.PublicKey

class SftpUploader(
    private val context: Context
) : UploadClient {

    private companion object {
        private const val TAG = "SftpUploader"
        private const val MIN_READ_TIMEOUT_MS = 90_000
        /** 閸掑棗娼￠崘娆忓弳濮ｅ繑顐兼导鐘虹翻閻ㄥ嫮绱﹂崘鎻掑隘婢堆冪毈閿?2KB閿涘矂妲诲銏ｇТ婢堆冨瀻閸ф袝閸欐垶鐓囨禍?SFTP 閺堝秴濮熼崳銊ф畱濠с垹鍤幏锔藉焻閸滃矁绻涢幒銉╁櫢缂?*/
        private const val CHUNK_SIZE = 32 * 1024
    }

    override val protocol: UploadProtocolType = UploadProtocolType.SFTP

    override suspend fun upload(
        task: UploadTask,
        profile: UploadServerProfile,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)?
    ): UploadResult {
        val config = profile.config as? SftpConfig
            ?: return invalidConfig("SFTP profile config is invalid")
        val uri = Uri.parse(task.recordUri)
        val localSize = getUriLength(context, uri)
        return withContext(Dispatchers.IO) {
            runTransfer(
                config = config,
                sourceUri = task.recordUri,
                fileName = task.fileName,
                resumeOffset = task.resumeOffset,
                validateOnly = false,
                onProgress = { bytesWritten ->
                    onProgress?.invoke(bytesWritten, localSize)
                }
            )
        }
    }

    override suspend fun testConnection(profile: UploadServerProfile): UploadResult {
        val config = profile.config as? SftpConfig
            ?: return invalidConfig("SFTP profile config is invalid")
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

    /**
     * 娑撳﹣绱堕崙鑺ユ殶閿涘本鏁幐浣规焽閻愬湱鐢绘导鐘偓?
     *
     * @param onProgress 鏉╂稑瀹抽崶鐐剁殶閿涘苯寮弫棰佽礋瑜版挸澧犲韫炊鐎涙濡幀缁樻殶閿涙稐璐?null 閺冩湹绗夐崶鐐剁殶閵?
     */
    suspend fun uploadWithResume(
        task: UploadTask,
        profile: UploadServerProfile,
        onProgress: ((uploadedBytes: Long) -> Unit)?
    ): UploadResult {
        val config = profile.config as? SftpConfig
            ?: return invalidConfig("SFTP profile config is invalid")
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

    private fun runTransfer(
        config: SftpConfig,
        sourceUri: String?,
        fileName: String?,
        resumeOffset: Long,
        validateOnly: Boolean,
        onProgress: ((uploadedBytes: Long) -> Unit)?
    ): UploadResult {
        if (config.host.isBlank() || config.username.isBlank() || config.password.isBlank()) {
            return invalidConfig("SFTP config is incomplete")
        }
        if (config.port == 21) {
            return invalidConfig("SFTP uses SSH port. Port 21 is FTP; please use port 22 or the server's SFTP port")
        }
        return try {
            createSshClient().use { ssh ->
                ssh.connectTimeout = config.connectTimeoutMs
                ssh.timeout = maxOf(config.readTimeoutMs, MIN_READ_TIMEOUT_MS)
                if (config.allowInsecureHostKey) {
                    ssh.addHostKeyVerifier(PromiscuousVerifier())
                } else {
                    ssh.addHostKeyVerifier(FirstUseHostKeyVerifier())
                }

                sftpStage("connect") {
                    ssh.connect(config.host, config.port)
                }
                sftpStage("authenticate") {
                    Log.i(TAG, "SFTP Debug Auth: user='${config.username}', pwdLength=${config.password.length}")
                    ssh.authPassword(config.username, config.password)
                }
                sftpStage("open SFTP subsystem") {
                    ssh.newSFTPClient()
                }.use { sftp ->
                    val remoteDir = sftpStage("prepare remote directory") {
                        resolveRemoteDir(sftp, config.remoteDir)
                    }
                    if (validateOnly) {
                        UploadResult.Success(remoteDir)
                    } else {
                        val safeFileName = fileName ?: return invalidConfig("SFTP upload file name is missing")
                        val safeSourceUri = sourceUri ?: return invalidConfig("SFTP upload source is missing")
                        val remotePath = buildSftpRemotePath(remoteDir, safeFileName)
                        val partPath = "$remotePath.part" // 缂侇厺绱舵稉瀛樻閺傚洣娆?
                        val uri = Uri.parse(safeSourceUri)
                        val localSize = getUriLength(context, uri)
                        if (localSize < 0) {
                            return transferFailure("SFTP upload source size is invalid", retryable = false)
                        }

                        // 绾喖鐣剧€圭偤妾紒顓濈炊鐠у嘲顫愭担宥囩枂閿?
                        val remotePartSize = runCatching {
                            sftp.statExistence(partPath)?.getSize() ?: 0L
                        }.getOrElse { 0L }
                        val actualOffset = minOf(resumeOffset, remotePartSize, localSize)

                        Log.i(TAG, "SFTP upload: file=$safeFileName, local=${localSize}B, " +
                            "resumeOffset=${resumeOffset}B, remotePartSize=${remotePartSize}B, actualOffset=${actualOffset}B")

                        val partUploadResult = runCatching {
                            sftpStage("upload file") {
                                if (actualOffset == 0L) {
                                    runCatching { sftp.rm(partPath) }
                                }
                                uploadFile(sftp, uri, partPath, actualOffset, onProgress)
                            }
                        }
                        if (partUploadResult.isFailure) {
                            Log.w(
                                TAG,
                                "SFTP .part upload failed, falling back to direct upload: " +
                                    "${partUploadResult.exceptionOrNull()?.javaClass?.name}: " +
                                    partUploadResult.exceptionOrNull()?.message.orEmpty()
                            )
                            val directResult = runCatching {
                                runCatching { sftp.rm(remotePath) }
                                uploadFile(sftp, uri, remotePath, 0L, onProgress)
                            }
                            if (directResult.isSuccess) {
                                Log.i(TAG, "SFTP direct upload success after .part failed: remotePath=$remotePath, size=$localSize")
                                return UploadResult.Success(remotePath)
                            }
                            throw partUploadResult.exceptionOrNull() ?: directResult.exceptionOrNull()
                                ?: IOException("SFTP upload failed")
                        }

                        // 閺嶏繝鐛欐径褍鐨?
                        val remoteAttributes = sftpStage("verify remote file") {
                            sftp.statExistence(partPath)
                        } ?: return transferFailure(
                            message = "SFTP upload finished but .part file not found: $partPath",
                            retryable = true
                        )
                        val remoteSize = remoteAttributes.getSize()
                        if (remoteSize != localSize) {
                            return transferFailure(
                                message = "SFTP remote file size mismatch: path=$partPath, local=$localSize, remote=$remoteSize",
                                retryable = true
                            )
                        }

                        // 閺嶏繝鐛欓柅姘崇箖閿涘苯甯€涙劙鍣搁崨钘夋倳娑撶儤顒滃蹇旀瀮娴?
                        sftpStage("rename to final") {
                            // 婵″倹鐏夐惄顔界垼閺傚洣娆㈠鎻掔摠閸︻煉绱濋崗鍫濆灩闂?
                            runCatching { sftp.rm(remotePath) }
                            sftp.rename(partPath, remotePath)
                        }
                        Log.i(TAG, "upload success: remotePath=$remotePath, size=$localSize")
                        UploadResult.Success(remotePath)
                    }
                }
            }
        } catch (throwable: Throwable) {
            Log.e(
                TAG,
                "runTransfer failed: validateOnly=$validateOnly, host=${config.host}, " +
                    "port=${config.port}, remoteDir=${config.remoteDir}, " +
                    "type=${throwable.javaClass.name}, message=${throwable.message.orEmpty()}\n" +
                    throwable.stackTraceToString()
            )
            when {
                throwable is SocketTimeoutException -> UploadResult.Failure(
                    UploadFailure(
                        code = UploadErrorCode.TIMEOUT,
                        message = "SFTP timeout: ${throwable.message.orEmpty()}",
                        retryable = true
                    )
                )

                throwable is SftpStageTimeoutException -> UploadResult.Failure(
                    UploadFailure(
                        code = UploadErrorCode.TIMEOUT,
                        message = "SFTP ${throwable.stage} timed out: ${throwable.cause?.message.orEmpty()}",
                        retryable = true
                    )
                )

                // 閺堝秴濮熼崳銊ョ槕闁姐儱鍑￠崣妯绘纯閿涙艾灏崚顐＄艾妫ｆ牗顐兼穱鈥叉崲閹锋帞绮烽敍鍦歄ST_KEY_REJECTED閿?
                // 濮濄倝鏁婄拠顖欑瑝鎼存棁鍤滈崝銊╁櫢鐠囨洩绱濇惔鏂跨穿鐎佃偐鏁ら幋宄板煂鐠佸墽鐤嗛悾宀勬桨绾喛顓婚獮鑸电闂勩倖妫幐鍥╂睏
                throwable is UserAuthException -> authFailure(
                    "SFTP authentication failed. Please check the username and password, or confirm that the server allows password login."
                )

                throwable is HostKeyChangedException -> UploadResult.Failure(
                    UploadFailure(
                        code = UploadErrorCode.HOST_KEY_CHANGED,
                        message = throwable.message.orEmpty(),
                        retryable = false
                    )
                )

                throwable is TransportException && throwable.message.orEmpty().contains("host key", ignoreCase = true) -> {
                    UploadResult.Failure(
                        UploadFailure(
                            code = UploadErrorCode.HOST_KEY_REJECTED,
                            message = "SFTP host key verification failed: ${throwable.message.orEmpty()}",
                            retryable = false
                        )
                    )
                }

                else -> translateThrowable("SFTP", throwable)
            }
        }
    }

    private fun createSshClient(): SSHClient {
        // On Android, forcing sshj onto the platform JCE provider is more stable than BC.
        SecurityUtils.setRegisterBouncyCastle(false)
        SecurityUtils.setSecurityProvider(null)
        val config = DefaultConfig().apply {
            // 閺勬儳绱￠惂钘夋倳閸楁洩绱濊ぐ璇茬俺闁灝绱戣ぐ鎾冲 Android 閻滎垰顣ㄧ紓鍝勩亼閻?X25519/curve25519閵?
            setKeyExchangeFactories(
                listOf(
                    DHGexSHA256.Factory(),
                    ECDHNistP.Factory521(),
                    ECDHNistP.Factory384(),
                    ECDHNistP.Factory256(),
                    DHGexSHA1.Factory(),
                    DHGroups.Group14SHA256(),
                    DHGroups.Group14SHA1(),
                    DHGroups.Group15SHA512(),
                    DHGroups.Group16SHA512(),
                    ExtendedDHGroups.Group14SHA256AtSSH(),
                    ExtendedDHGroups.Group15SHA256(),
                    ExtendedDHGroups.Group15SHA256AtSSH(),
                    ExtendedDHGroups.Group15SHA384AtSSH(),
                    ExtendedDHGroups.Group16SHA256(),
                    ExtendedDHGroups.Group16SHA384AtSSH(),
                    ExtendedDHGroups.Group16SHA512AtSSH(),
                    ExtendedDHGroups.Group18SHA512AtSSH(),
                    ExtInfoClientFactory()
                )
            )
        }
        return SSHClient(config)
    }

    private inline fun <T> sftpStage(stage: String, block: () -> T): T {
        return try {
            block()
        } catch (throwable: SocketTimeoutException) {
            throw SftpStageTimeoutException(stage, throwable)
        }
    }

    private fun resolveRemoteDir(sftp: SFTPClient, remoteDir: String): String {
        val normalized = remoteDir.trim().replace('\\', '/').trimEnd('/')
        if (normalized.isBlank() || normalized == ".") {
            return "."
        }
        if (normalized == "/") {
            return "/"
        }

        val candidates = buildList {
            add(normalized)
            if (normalized.startsWith("/")) {
                add(normalized.trimStart('/').ifBlank { "." })
            }
        }.distinct()

        var lastFailure: Throwable? = null
        for (candidate in candidates) {
            try {
                ensureRemoteDir(sftp, candidate)
                return candidate
            } catch (throwable: Throwable) {
                lastFailure = throwable
            }
        }
        throw lastFailure ?: IllegalStateException("Unable to resolve SFTP remote directory")
    }

    private fun ensureRemoteDir(sftp: SFTPClient, remoteDir: String) {
        val existing = runCatching { sftp.statExistence(remoteDir) }.getOrNull()
        if (existing?.type == FileMode.Type.DIRECTORY) {
            return
        }
        sftp.mkdirs(remoteDir)
    }

    private fun buildSftpRemotePath(remoteDir: String, fileName: String): String {
        val normalizedDir = remoteDir.trim().trimEnd('/')
        return when {
            normalizedDir.isBlank() || normalizedDir == "." -> fileName
            normalizedDir == "/" -> "/$fileName"
            else -> "$normalizedDir/$fileName"
        }
    }

    private fun testWritable(sftp: SFTPClient, remoteDir: String): UploadResult.Failure? {
        val probePath = buildSftpRemotePath(remoteDir, "press_iptc_probe_${System.currentTimeMillis()}.tmp")
        return runCatching {
            sftp.open(probePath, setOf(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)).use { remoteFile ->
                val bytes = "ok".toByteArray(Charsets.UTF_8)
                remoteFile.write(0L, bytes, 0, bytes.size)
            }
            runCatching { sftp.rm(probePath) }
        }.fold(
            onSuccess = { null },
            onFailure = {
                UploadResult.Failure(
                    UploadFailure(
                        code = UploadErrorCode.DIRECTORY_ACCESS_DENIED,
                        message = "SFTP directory is not writable: $remoteDir (${it.message.orEmpty()})",
                        retryable = false
                    )
                )
            }
        )
    }

    private fun uploadFile(
        sftp: SFTPClient,
        uri: Uri,
        remotePath: String,
        offset: Long,
        onProgress: ((uploadedBytes: Long) -> Unit)?
    ) {
        val openModes = if (offset > 0L) {
            setOf(OpenMode.WRITE, OpenMode.CREAT)
        } else {
            setOf(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)
        }
        sftp.open(remotePath, openModes).use { remoteFile ->
            val buf = ByteArray(CHUNK_SIZE)
            var fileOffset = offset
            context.contentResolver.openInputStream(uri)?.use { input ->
                if (offset > 0L) {
                    input.safeSkip(offset)
                }
                var read: Int
                while (input.read(buf).also { read = it } != -1) {
                    remoteFile.write(fileOffset, buf, 0, read)
                    fileOffset += read
                    onProgress?.invoke(fileOffset)
                }
            } ?: throw IOException("Failed to open source stream")
        }
    }
    /**
     * TOFU閿涘牓顩诲▎鈥蹭繆娴犱紮绱氭稉缁樻簚鐎靛棝鎸滄宀冪槈閸ｃ劊鈧?
     *
     * - 妫ｆ牗顐兼潻鐐村复閺冭绱伴懛顏勫З娣囨繂鐡ㄩ幐鍥╂睏閿涘奔淇婃禒姹団偓?
     * - 閹稿洨姹楁稉鈧懛杈剧窗娣団€叉崲閵?
     * - 閹稿洨姹楅崣妯绘纯閿涙碍濮忛崙?[HostKeyChangedException]閿涘奔绗傜仦鍌涘礋閼惧嘲鎮楃亸鍡楀従鏉烆剚宕叉稉?
     *   [UploadErrorCode.HOST_KEY_CHANGED] 闁挎瑨顕ら敍灞肩返 UI 鐏炲倻绨跨涵顔藉絹缁€铏规暏閹存灚鈧?
     */
    private class FirstUseHostKeyVerifier : HostKeyVerifier {
        override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
            val fingerprint = key.sha256Fingerprint()
            val saved = UploadSecretStore.loadHostFingerprint(hostname, port).ifBlank { null }
            when {
                saved == null -> {
                    // 妫ｆ牗顐兼潻鐐村复閿涙俺鍤滈崝銊唶瑜版洘瀵氱痪鐧哥礉娣団€叉崲
                    UploadSecretStore.saveHostFingerprint(hostname, port, fingerprint)
                    return true
                }
                saved == fingerprint -> return true
                else -> {
                    // 閹稿洨姹楀鎻掑綁閺囪揪绱版稉宥夋饯姒涙ê銇戠拹銉礉閹舵稑鍤崣顖涘妳閻儱绱撶敮?
                    // 闁挎瑨顕ゅ☉鍫熶紖閸栧懎鎯堥弮?閺傜増瀵氱痪鐧哥礉閺傞€涚┒鏉╂劗娣幒鎺撶叀
                    throw HostKeyChangedException(
                        hostname = hostname,
                        port = port,
                        savedFingerprint = saved,
                        currentFingerprint = fingerprint
                    )
                }
            }
        }

        override fun findExistingAlgorithms(hostname: String, port: Int): List<String> = emptyList()

        private fun PublicKey.sha256Fingerprint(): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(encoded)
            return Base64.encodeToString(digest, Base64.NO_WRAP)
        }
    }

    /**
     * SFTP 閺堝秴濮熼崳銊ョ槕闁姐儱鍑￠崣妯绘纯瀵倸鐖堕妴?
     * 閸栧搫鍨庢禍?[TransportException]閿涘苯濮忓Ч鍌氭躬 UI 鐏炲倻绨跨涵顔肩潔缁€琛♀偓婊勬箛閸斺€虫珤鐎靛棝鎸滃鎻掑綁閺囪揪绱濈拠宄板煂鐠佸墽鐤嗘稉顓犫€樼拋銈呰嫙濞撳懘娅庨弮褎瀵氱痪鍏夆偓婵堟畱閹绘劗銇氶妴?
     */
    class HostKeyChangedException(
        val hostname: String,
        val port: Int,
        val savedFingerprint: String,
        val currentFingerprint: String
    ) : IOException(
        "SFTP host key changed for $hostname:$port. " +
            "Saved: $savedFingerprint, Current: $currentFingerprint. " +
            "If the server has been reinstalled or its key regenerated, please clear the saved fingerprint in Settings > Upload Config."
    )

    private class SftpStageTimeoutException(
        val stage: String,
        cause: SocketTimeoutException
    ) : IOException(cause)
}


