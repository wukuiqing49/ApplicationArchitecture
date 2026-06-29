package com.wkq.net.interceptor

import android.util.Log
import com.wkq.net.config.NetConst
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

object LoggingInterceptor {


    private const val MAX_LENGTH = 3000
    private const val MAX_BODY_LOG_BYTES = 256 * 1024L
    private val SENSITIVE_HEADER_NAMES = setOf(
        "authorization",
        "cookie",
        "set-cookie",
        "token",
        "access-token",
        "refresh-token",
        "x-token",
        "x-auth-token"
    )

    fun create(isDebug: Boolean): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()

            if (!isDebug) {
                return@Interceptor chain.proceed(request)
            }

            val startNs = System.nanoTime()
            logRequest(request)

            val response = try {
                chain.proceed(request)
            } catch (e: Exception) {
                Log.e(NetConst.TAG, "请求失败: ${e.message}", e)
                throw e
            }

            val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
            logResponse(response, tookMs)

            response
        }
    }

    private fun logRequest(request: Request) {
        val url = request.url
        val method = request.method
        val body = request.body
        val contentType = body?.contentType()?.toString().orEmpty()

        val sb = StringBuilder()
        sb.append("\n================ 请求开始 ================\n")
        sb.append("URL: ").append(url).append("\n")
        sb.append("Method: ").append(method).append("\n")
        sb.append("Content-Type: ").append(contentType).append("\n")

        if (request.headers.size > 0) {
            sb.append("Headers:\n")
            for (name in request.headers.names()) {
                sb.append("  ").append(name).append(": ")
                    .append(maskHeaderValue(name, request.header(name))).append("\n")
            }
        }

        if (url.querySize > 0) {
            sb.append("Query参数:\n")
            for (i in 0 until url.querySize) {
                sb.append("  ")
                    .append(url.queryParameterName(i))
                    .append(" = ")
                    .append(maskValueIfSensitive(url.queryParameterName(i), url.queryParameterValue(i)))
                    .append("\n")
            }
        }

        body?.let {
            val bodyContent = readRequestBody(it)
            if (bodyContent.isNotBlank()) {
                sb.append("Body:\n").append(bodyContent).append("\n")
            }
        }

        sb.append("================ 请求结束 ================\n")
        logLong(NetConst.TAG, sb.toString())
    }

    private fun logResponse(response: Response, tookMs: Long) {
        val responseBody = response.body
        val request = response.request

        val sb = StringBuilder()
        sb.append("\n================ 响应开始 ================\n")
        sb.append("URL: ").append(request.url).append("\n")
        sb.append("Code: ").append(response.code).append("\n")
        sb.append("Message: ").append(response.message).append("\n")
        sb.append("耗时: ").append(tookMs).append(" ms\n")

        if (response.headers.size > 0) {
            sb.append("Headers:\n")
            for (name in response.headers.names()) {
                sb.append("  ").append(name).append(": ")
                    .append(maskHeaderValue(name, response.header(name))).append("\n")
            }
        }

        val contentType = responseBody.contentType()?.toString().orEmpty()
        sb.append("Response Content-Type: ").append(contentType).append("\n")

        try {
            if (shouldSkipBodyLog(contentType, responseBody.contentLength())) {
                sb.append("Response Body: skipped, contentLength=")
                    .append(responseBody.contentLength())
                    .append("\n")
            } else {
                val source = responseBody.source()
                source.request(MAX_BODY_LOG_BYTES)
                val buffer = source.buffer.clone()
                val readLength = buffer.size.coerceAtMost(MAX_BODY_LOG_BYTES)

                val charset = responseBody.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                val bodyString = buffer.readString(readLength, charset)

                if (bodyString.isNotBlank()) {
                    sb.append("Response Body:\n")
                    sb.append(formatJsonIfNeeded(bodyString)).append("\n")
                    if (responseBody.contentLength() > MAX_BODY_LOG_BYTES) {
                        sb.append("Response Body 已截断，仅展示前 ")
                            .append(MAX_BODY_LOG_BYTES)
                            .append(" bytes\n")
                    }
                }
            }
        } catch (e: Exception) {
            sb.append("Response Body 读取失败: ").append(e.message).append("\n")
        }

        sb.append("================ 响应结束 ================\n")
        logLong(NetConst.TAG, sb.toString())
    }

    private fun readRequestBody(body: RequestBody): String {
        return try {
            when (body) {
                is FormBody -> {
                    val sb = StringBuilder()
                    sb.append("Form参数:\n")
                    for (i in 0 until body.size) {
                        sb.append("  ")
                            .append(body.name(i))
                            .append(" = ")
                            .append(maskValueIfSensitive(body.name(i), body.value(i)))
                            .append("\n")
                    }
                    sb.toString()
                }

                is MultipartBody -> {
                    val sb = StringBuilder()
                    sb.append("Multipart参数:\n")
                    body.parts.forEachIndexed { index, part ->
                        sb.append("  part[").append(index).append("]: ")
                            .append(part.headers).append("\n")
                    }
                    sb.toString()
                }

                else -> {
                    val buffer = Buffer()
                    body.writeTo(buffer)
                    val charset: Charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                    val raw = buffer.readString(charset)
                    formatJsonIfNeeded(raw)
                }
            }
        } catch (e: Exception) {
            "请求体读取失败: ${e.message}"
        }
    }

    private fun formatJsonIfNeeded(message: String): String {
        val trim = message.trim()
        return try {
            when {
                trim.startsWith("{") -> JSONObject(trim).toString(2)
                trim.startsWith("[") -> JSONArray(trim).toString(2)
                else -> message
            }
        } catch (_: Exception) {
            message
        }
    }

    private fun logLong(tag: String, message: String) {
        var start = 0
        val length = message.length
        while (start < length) {
            val end = (start + MAX_LENGTH).coerceAtMost(length)
            Log.d(tag, message.substring(start, end))
            start = end
        }
    }

    private fun shouldSkipBodyLog(contentType: String, contentLength: Long): Boolean {
        val lowerContentType = contentType.lowercase()
        if (contentLength > MAX_BODY_LOG_BYTES) return true
        return lowerContentType.startsWith("image/") ||
                lowerContentType.startsWith("video/") ||
                lowerContentType.startsWith("audio/") ||
                lowerContentType.contains("octet-stream") ||
                lowerContentType.contains("zip") ||
                lowerContentType.contains("pdf")
    }

    private fun maskHeaderValue(name: String, value: String?): String {
        return maskValueIfSensitive(name, value)
    }

    private fun maskValueIfSensitive(name: String, value: String?): String {
        if (value.isNullOrEmpty()) return value.orEmpty()
        val key = name.lowercase()
        val isSensitive = key in SENSITIVE_HEADER_NAMES ||
                key.contains("password") ||
                key.contains("secret") ||
                key.contains("token") ||
                key.contains("auth")
        return if (isSensitive) "***" else value
    }
}
