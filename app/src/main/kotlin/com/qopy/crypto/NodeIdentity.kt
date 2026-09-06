package com.qopy.crypto

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import java.util.UUID

data class NodeIdentity(
    val deviceId: String,
    val deviceName: String,
    val privateKey: ByteArray,
    val publicKey: ByteArray,
    val tlsCert: X509Certificate,
    val tlsCertSha256: ByteArray,
    val tlsKeyStore: KeyStore,
    val tlsKeyPassword: CharArray
) {
    companion object {
        private const val PREFS_NAME = "qopy_identity"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_PRIVATE_KEY = "private_key"
        private const val KEY_PUBLIC_KEY = "public_key"

        fun loadOrCreate(context: Context, defaultName: String): NodeIdentity {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            var devId = prefs.getString(KEY_DEVICE_ID, null)
            var devName = prefs.getString(KEY_DEVICE_NAME, null)
            var privHex = prefs.getString(KEY_PRIVATE_KEY, null)
            var pubHex = prefs.getString(KEY_PUBLIC_KEY, null)

            val privateKey: ByteArray
            val publicKey: ByteArray

            if (devId == null || privHex == null || pubHex == null) {
                devId = UUID.randomUUID().toString()
                devName = defaultName
                val pair = CryptoEngine.generateKeyPair()
                privateKey = pair.first
                publicKey = pair.second

                prefs.edit()
                    .putString(KEY_DEVICE_ID, devId)
                    .putString(KEY_DEVICE_NAME, devName)
                    .putString(KEY_PRIVATE_KEY, hex(privateKey))
                    .putString(KEY_PUBLIC_KEY, hex(publicKey))
                    .apply()
            } else {
                privateKey = unhex(privHex)
                publicKey = unhex(pubHex)
                if (devName == null) devName = defaultName
            }

            // Generate ephemeral TLS certificate for TLS 1.3 listener
            val (cert, keyStore, password) = generateSelfSignedTlsCert(devId)
            val certSha256 = CryptoEngine.computeSha256(cert.encoded)

            return NodeIdentity(
                deviceId = devId,
                deviceName = devName,
                privateKey = privateKey,
                publicKey = publicKey,
                tlsCert = cert,
                tlsCertSha256 = certSha256,
                tlsKeyStore = keyStore,
                tlsKeyPassword = password
            )
        }

        private fun generateSelfSignedTlsCert(deviceId: String): Triple<X509Certificate, KeyStore, CharArray> {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(256)
            val keyPair = kpg.generateKeyPair()

            val now = System.currentTimeMillis()
            val notBefore = Date(now - 1000L * 60)
            val notAfter = Date(now + 1000L * 60 * 60 * 24 * 365 * 10) // 10 years
            val serial = BigInteger(64, SecureRandom())
            val issuer = X500Name("CN=$deviceId")
            val subject = issuer

            val certBuilder = JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, keyPair.public
            )

            val signer = JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private)
            val certHolder = certBuilder.build(signer)
            val cert = JcaX509CertificateConverter().getCertificate(certHolder)

            val password = "qopy-tls-password".toCharArray()
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, password)
            keyStore.setKeyEntry("qopy-tls", keyPair.private, password, arrayOf(cert))

            return Triple(cert, keyStore, password)
        }

        private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
        private fun unhex(hex: String): ByteArray {
            val result = ByteArray(hex.length / 2)
            for (i in result.indices) {
                result[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            return result
        }
    }
}
