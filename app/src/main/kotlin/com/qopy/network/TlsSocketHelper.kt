package com.qopy.network

import com.qopy.crypto.CryptoEngine
import com.qopy.crypto.NodeIdentity
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object TlsSocketHelper {
    fun createServerTlsSocket(identity: NodeIdentity, port: Int): SSLServerSocket {
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(identity.tlsKeyStore, identity.tlsKeyPassword)

        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(kmf.keyManagers, null, null)

        val serverSocket = sslContext.serverSocketFactory.createServerSocket(port) as SSLServerSocket
        serverSocket.enabledProtocols = arrayOf("TLSv1.3")
        serverSocket.needClientAuth = false
        return serverSocket
    }

    fun createClientTlsSocket(
        host: String,
        port: Int,
        pinnedFingerprint: ByteArray?,
        timeoutMs: Int = 10000
    ): SSLSocket {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                if (pinnedFingerprint != null) {
                    val cert = chain?.firstOrNull() ?: throw SecurityException("No server certificate presented")
                    val fingerprint = CryptoEngine.computeSha256(cert.encoded)
                    if (!MessageDigest.isEqual(fingerprint, pinnedFingerprint)) {
                        throw SecurityException("Pinned certificate fingerprint mismatch")
                    }
                }
                // Unpinned: accepted during initial pairing bootstrap
            }
        }

        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), null)

        val rawSocket = Socket()
        rawSocket.connect(InetSocketAddress(host, port), timeoutMs)
        rawSocket.tcpNoDelay = true

        val sslSocket = sslContext.socketFactory.createSocket(
            rawSocket,
            host,
            port,
            true
        ) as SSLSocket
        sslSocket.enabledProtocols = arrayOf("TLSv1.3")
        sslSocket.startHandshake()
        return sslSocket
    }
}
