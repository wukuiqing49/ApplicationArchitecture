package com.wkq.net.https

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * 管理 SSL/TLS 证书和主机名验证的工具。
 * 目前配置为信任所有证书，以便在开发/测试期间具有灵活性。
 * 对于严格的生产要求，可以在此处替换为针对服务器证书进行固定（Pinning）的自定义 TrustManager。
 */
object HttpsUtils {

    /**
     * 信任所有证书的 X509TrustManager。
     */
    class UnSafeTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    /**
     * 接受所有主机名验证的主机名验证器。
     */
    class UnSafeHostnameVerifier : HostnameVerifier {
        override fun verify(hostname: String?, session: SSLSession?): Boolean = true
    }

    /**
     * 使用 UnSafeTrustManager 创建一个信任所有证书的 SSLSocketFactory。
     */
    fun createSSLSocketFactory(trustManager: X509TrustManager = UnSafeTrustManager()): SSLSocketFactory {
        return try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            throw RuntimeException("创建 SSLSocketFactory 失败", e)
        }
    }
}
