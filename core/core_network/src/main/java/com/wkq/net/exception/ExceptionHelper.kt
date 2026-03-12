package com.wkq.net.exception

import android.net.ParseException
import com.google.gson.JsonParseException
import org.json.JSONException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.CancellationException

/**
 * Helper object to centralize mapping of Network and JSON Exceptions into clear error codes and messages.
 */
object ExceptionHelper {
    
    // Custom error codes roughly corresponding to common issues
    const val ERROR_NETWORK_TIMEOUT = 1001
    const val ERROR_NETWORK_CONNECTION = 1002
    const val ERROR_NETWORK_UNKNOWN_HOST = 1003
    const val ERROR_CANCELED = 1004
    const val ERROR_JSON_PARSING = 2001
    const val ERROR_SSL = 3001
    const val ERROR_UNKNOWN = -1

    /**
     * Map a generic throwable into a structured code and message pair.
     * @param e Exception thrown during request.
     * @return Pair containing the mapped (errorCode, errorMessage).
     */
    fun handleException(e: Throwable): Pair<Int, String> {
        return when (e) {
            is SocketTimeoutException -> {
                Pair(ERROR_NETWORK_TIMEOUT, "Request Timeout: ${e.message}")
            }
            is ConnectException -> {
                Pair(ERROR_NETWORK_CONNECTION, "Failed to connect to the server: ${e.message}")
            }
            is UnknownHostException -> {
                Pair(ERROR_NETWORK_UNKNOWN_HOST, "Unknown Host, check network connection: ${e.message}")
            }
            is JsonParseException, is JSONException, is ParseException -> {
                Pair(ERROR_JSON_PARSING, "Data parsing error. Server returned badly formatted JSON.")
            }
            is SSLHandshakeException -> {
                Pair(ERROR_SSL, "SSL Certificate validation failed: ${e.message}")
            }
            is HttpException -> {
                val code = e.code()
                val msg = e.message()
                Pair(code, "HTTP Error $code: $msg")
            }
            is CancellationException -> {
                Pair(ERROR_CANCELED, "Request was canceled")
            }
            else -> {
                Pair(ERROR_UNKNOWN, e.message ?: "An unknown error occurred")
            }
        }
    }
}
