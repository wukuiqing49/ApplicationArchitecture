package com.wkq.iptc.upload

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * WebDAV 涓婁紶瀹㈡埛绔€?
 *
 * 涓婁紶娴佺▼锛?
 * 1. 灏嗘簮鏂囦欢鍐呭閫氳繃 HTTP PUT 鍐欏叆杩滅 <remotePath>.part 涓存椂鏂囦欢銆?
 * 2. 浣跨敤 WebDAV MOVE 鏂规硶灏?.part 閲嶅懡鍚嶄负鏈€缁堣矾寰勶紙鍘熷瓙鎬т繚璇侊級銆?
 * 3. 閫氳繃 PROPFIND 鏌ヨ杩滅鏂囦欢澶у皬锛屼笌鏈湴姣旇緝鍋氬畬鏁存€ф牎楠屻€?
 *
 * 鏀寔閫夐」锛?
 * - HTTP / HTTPS锛坲seHttps锛?
 * - 鍏佽鑷鍚嶈瘉涔︼紙allowInsecureCert锛屼粎 useHttps 鏃舵湁鏁堬級
 * - Basic Auth锛堢敤鎴峰悕 + 瀵嗙爜锛屽潎涓虹┖鏃跺尶鍚嶈闂級
 */
class WebDavUploader(
    private val context: Context
) : UploadClient {

    private companion object {
        private const val TAG = "WebDavUploader"
        private val OCTET_STREAM = "application/octet-stream".toMediaTypeOrNull()
    }

    override val protocol: UploadProtocolType = UploadProtocolType.WEBDAV

    override suspend fun upload(
        task: UploadTask,
        profile: UploadServerProfile,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)?
    ): UploadResult {
        val config = profile.config as? WebDavConfig
            ?: return invalidConfig("WebDAV profile config is invalid")
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
        val config = profile.config as? WebDavConfig
            ?: return invalidConfig("WebDAV profile config is invalid")
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
        config: WebDavConfig,
        sourceUri: String?,
        fileName: String?,
        resumeOffset: Long,
        validateOnly: Boolean,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)?
    ): UploadResult {
        if (config.host.isBlank()) {
            return invalidConfig("WebDAV host is empty")
        }

        val client = buildClient(config)
        val baseUrl = buildBaseUrl(config)

        return try {
            // 杩炴帴娴嬭瘯锛氬 remoteDir 鐩綍鍋?PROPFIND 楠岃瘉鍙揪鎬?
            val dirUrl = "$baseUrl${config.remoteDir.trimEnd('/')}/"
            val propfindResult = propfind(client, dirUrl, config)
            if (propfindResult != null) {
                return propfindResult
            }

            if (validateOnly) {
                Log.i(TAG, "WebDAV connection test success: url=$dirUrl")
                return UploadResult.Success(config.remoteDir)
            }

            val safeFileName = fileName ?: return invalidConfig("WebDAV upload file name is missing")
            val safeSourceUri = sourceUri ?: return invalidConfig("WebDAV upload source is missing")
            val remotePath = buildRemotePath(config.remoteDir, safeFileName)
            val partPath = "$remotePath.part"
            val partUrl = "$baseUrl$partPath"
            val finalUrl = "$baseUrl$remotePath"
            val uri = Uri.parse(safeSourceUri)
            val localSize = getUriLength(context, uri)
            if (localSize < 0) {
                return transferFailure("WebDAV upload source size is invalid", retryable = false)
            }

            // 纭畾瀹為檯缁紶璧峰浣嶇疆锛?
            val remotePartSize = getContentLength(client, partUrl, config)
            val actualOffset = if (remotePartSize > 0L) {
                minOf(resumeOffset, remotePartSize, localSize)
            } else {
                0L
            }

            Log.i(TAG, "WebDAV upload: file=$safeFileName, local=${localSize}B, " +
                "resumeOffset=${resumeOffset}B, remotePartSize=${remotePartSize}B, actualOffset=${actualOffset}B")

            // 1. PUT 涓婁紶鍒?.part 涓存椂鏂囦欢
            var putResult = putFile(client, partUrl, uri, localSize, actualOffset, config, onProgress)
            
            // 鍏煎鎬ч檷绾х瓥鐣ワ細濡傛灉甯?Range 鐨?PUT 閬亣 400 (Bad Request), 501 (Not Implemented) 鎴?405 鏃讹紝
            // 涓?actualOffset > 0锛屾垜浠竻闄ゅ凡涓婁紶閮ㄥ垎骞朵粠 0 寮€濮嬮噸鏂颁笂浼?
            if (putResult != null && actualOffset > 0L) {
                val errorCode = putResult.error.code
                val shouldFallback = errorCode == UploadErrorCode.INVALID_CONFIG ||
                        putResult.error.message.contains("400") ||
                        putResult.error.message.contains("501") ||
                        putResult.error.message.contains("405")
                
                if (shouldFallback) {
                    Log.w(TAG, "WebDAV Server does not support Range PUT. Falling back to full upload. Error: ${putResult.error.message}")
                    // 鍒犻櫎杩滅▼宸插瓨鍦ㄧ殑閮ㄥ垎鏂囦欢
                    runCatching {
                        val deleteRequest = Request.Builder()
                            .url(partUrl)
                            .delete()
                            .applyAuth(config)
                            .build()
                        client.newCall(deleteRequest).execute().use { }
                    }
                    // 浠?0 寮€濮嬮噸鏂板叏閲忎笂浼?
                    putResult = putFile(client, partUrl, uri, localSize, 0L, config, onProgress)
                }
            }

            if (putResult != null) {
                return putResult
            }

            // 2. 澶у皬鏍￠獙
            val remoteSize = getContentLength(client, partUrl, config)
            if (remoteSize >= 0 && remoteSize != localSize) {
                return transferFailure(
                    "WebDAV remote file size mismatch: path=$partPath, local=$localSize, remote=$remoteSize",
                    retryable = true
                )
            }

            // 3. MOVE 閲嶅懡鍚嶄负鏈€缁堟枃浠讹紙鍘熷瓙鎬э級
            val moveResult = moveFile(client, partUrl, finalUrl, config)
            if (moveResult != null) {
                return moveResult
            }

            Log.i(TAG, "WebDAV upload success: remotePath=$remotePath, size=$localSize")
            UploadResult.Success(remotePath)
        } catch (throwable: Throwable) {
            Log.e(
                TAG,
                "runTransfer failed: validateOnly=$validateOnly, host=${config.host}, " +
                    "port=${config.port}, remoteDir=${config.remoteDir}, " +
                    "type=${throwable.javaClass.name}, message=${throwable.message.orEmpty()}\n" +
                    throwable.stackTraceToString()
            )
            when (throwable) {
                is UnknownHostException -> connectionFailure("WebDAV host not found: ${config.host}")
                is SocketTimeoutException -> UploadResult.Failure(
                    UploadFailure(
                        code = UploadErrorCode.TIMEOUT,
                        message = "WebDAV timeout: ${throwable.message}",
                        retryable = true
                    )
                )
                else -> translateThrowable("WebDAV", throwable)
            }
        }
    }

    /**
     * PROPFIND 妫€娴嬬洰褰曞彲杈炬€э紝杩斿洖闈?null 琛ㄧず澶辫触銆?
     */
    private fun propfind(
        client: OkHttpClient,
        url: String,
        config: WebDavConfig
    ): UploadResult.Failure? {
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", null)
            .header("Depth", "0")
            .applyAuth(config)
            .build()
        val response = client.newCall(request).execute()
        response.use {
            return when {
                it.isSuccessful || it.code == 207 -> null // 207 Multi-Status 鏄?WebDAV 姝ｅ父鍝嶅簲
                it.code == 401 -> return authFailure("WebDAV authentication failed (401)")
                it.code == 403 -> return UploadResult.Failure(
                    UploadFailure(
                        code = UploadErrorCode.DIRECTORY_ACCESS_DENIED,
                        message = "WebDAV access denied (403): $url",
                        retryable = false
                    )
                )
                it.code == 404 -> {
                    // remoteDir 涓嶅瓨鍦ㄦ椂灏濊瘯鍒涘缓
                    val mkcolResult = mkcol(client, url, config)
                    mkcolResult // null 琛ㄧず鎴愬姛
                }
                else -> transferFailure(
                    "WebDAV PROPFIND failed: code=${it.code}, url=$url",
                    retryable = it.code >= 500
                )
            }
        }
    }

    /**
     * MKCOL 鍒涘缓鐩綍锛岃繑鍥為潪 null 琛ㄧず澶辫触銆?
     */
    private fun mkcol(
        client: OkHttpClient,
        url: String,
        config: WebDavConfig
    ): UploadResult.Failure? {
        val request = Request.Builder()
            .url(url)
            .method("MKCOL", null)
            .applyAuth(config)
            .build()
        val response = client.newCall(request).execute()
        response.use {
            return if (it.isSuccessful || it.code == 201 || it.code == 405 /* 宸插瓨鍦?*/) {
                null
            } else {
                UploadResult.Failure(
                    UploadFailure(
                        code = UploadErrorCode.DIRECTORY_ACCESS_DENIED,
                        message = "WebDAV MKCOL failed: code=${it.code}, url=$url",
                        retryable = false
                    )
                )
            }
        }
    }

    /**
     * PUT 涓婁紶鏂囦欢鍐呭锛岃繑鍥為潪 null 琛ㄧず澶辫触銆?
     */
    private fun putFile(
        client: OkHttpClient,
        url: String,
        uri: Uri,
        contentLength: Long,
        actualOffset: Long,
        config: WebDavConfig,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)?
    ): UploadResult.Failure? {
        val body = UriRequestBody(
            context = context,
            uri = uri,
            contentType = OCTET_STREAM,
            contentLength = contentLength,
            skipOffset = actualOffset,
            onProgress = onProgress
        )
        val request = Request.Builder()
            .url(url)
            .put(body)
            .apply {
                if (actualOffset > 0L) {
                    header("Content-Range", "bytes $actualOffset-${contentLength - 1}/$contentLength")
                }
            }
            .applyAuth(config)
            .build()
        val response = client.newCall(request).execute()
        response.use {
            return if (it.isSuccessful || it.code == 201 || it.code == 204) {
                null
            } else if (it.code == 401) {
                authFailure("WebDAV authentication failed on PUT (401)")
            } else {
                transferFailure(
                    "WebDAV PUT failed: code=${it.code}, url=$url",
                    retryable = it.code >= 500
                )
            }
        }
    }

    /**
     * MOVE 閲嶅懡鍚嶆枃浠讹紙.part 鈫?姝ｅ紡璺緞锛夛紝杩斿洖闈?null 琛ㄧず澶辫触銆?
     */
    private fun moveFile(
        client: OkHttpClient,
        sourceUrl: String,
        destUrl: String,
        config: WebDavConfig
    ): UploadResult.Failure? {
        val request = Request.Builder()
            .url(sourceUrl)
            .method("MOVE", null)
            .header("Destination", destUrl)
            .header("Overwrite", "T")
            .applyAuth(config)
            .build()
        val response = client.newCall(request).execute()
        response.use {
            return if (it.isSuccessful || it.code == 201 || it.code == 204) {
                null
            } else {
                transferFailure(
                    "WebDAV MOVE failed: code=${it.code}, from=$sourceUrl, to=$destUrl",
                    retryable = it.code >= 500
                )
            }
        }
    }

    /**
     * HEAD 璇锋眰鑾峰彇杩滅鏂囦欢澶у皬锛屽け璐ユ椂杩斿洖 -1銆?
     */
    private fun getContentLength(
        client: OkHttpClient,
        url: String,
        config: WebDavConfig
    ): Long {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .head()
                .applyAuth(config)
                .build()
            client.newCall(request).execute().use { response ->
                response.header("Content-Length")?.toLongOrNull() ?: -1L
            }
        }.getOrElse { -1L }
    }

    /** 鏋勯€?WebDAV 鏍?URL锛坔ttp:// 鎴?https://锛夈€?*/
    private fun buildBaseUrl(config: WebDavConfig): String {
        val scheme = if (config.useHttps) "https" else "http"
        val defaultPort = if (config.useHttps) 443 else 80
        return if (config.port == defaultPort) {
            "$scheme://${config.host}"
        } else {
            "$scheme://${config.host}:${config.port}"
        }
    }

    /** 鏋勯€?OkHttpClient锛屾寜闇€寮€鍚嚜绛惧悕璇佷功鏀寔銆?*/
    private fun buildClient(config: WebDavConfig): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(config.readTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .writeTimeout(config.readTimeoutMs.toLong(), TimeUnit.MILLISECONDS)

        if (config.useHttps && config.allowInsecureCert) {
            // 浠呭紑鍙?鍐呯綉鑷鍚嶅満鏅娇鐢紝涓嶅缓璁敓浜х幆澧冨紑鍚?
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, trustAll)
            builder.hostnameVerifier { _, _ -> true }
        }

        return builder.addInterceptor(com.wkq.iptc.upload.http.RetryInterceptor(maxRetry = 3)).build()
    }

    /** 涓?Request.Builder 娣诲姞 Basic Auth 澶达紙鐢ㄦ埛鍚嶅拰瀵嗙爜鍧囬潪绌烘椂鎵嶆坊鍔狅級銆?*/
    private fun Request.Builder.applyAuth(config: WebDavConfig): Request.Builder {
        if (config.username.isNotBlank() && config.password.isNotBlank()) {
            header("Authorization", Credentials.basic(config.username, config.password))
        }
        return this
    }
}




