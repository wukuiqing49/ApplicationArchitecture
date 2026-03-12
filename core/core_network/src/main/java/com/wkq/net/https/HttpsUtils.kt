package com.wkq.net.https

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * Utility for managing SSL/TLS Certificates and Hostname verification.
 * Currently configured to trust all certificates for flexibility during development/testing.
 * For strict production requirements, a custom TrustManager pinning the server's cert can be substituted here.
 */
object HttpsUtils {

    /**
     * An X509TrustManager that trusts all certificates.
     */
    class UnSafeTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    /**
     * HostnameVerifier that accepts all hostnames.
     */
    class UnSafeHostnameVerifier : HostnameVerifier {
        override fun verify(hostname: String?, session: SSLSession?): Boolean = true
    }

    /**
     * Creates an SSLSocketFactory that trusts all certificates using the UnSafeTrustManager.
     */
    fun createSSLSocketFactory(): SSLSocketFactory {
        return try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(UnSafeTrustManager()), SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            throw RuntimeException("Failed to create SSLSocketFactory", e)
        }
    }
}
