package com.qopy.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object PairingCrypto {
    private const val CROCKFORD_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private val random = SecureRandom()

    fun generatePairingCode(): String {
        val chars = CharArray(8)
        for (i in 0 until 8) {
            val idx = random.nextInt(CROCKFORD_ALPHABET.length)
            chars[i] = CROCKFORD_ALPHABET[idx]
        }
        return "${String(chars, 0, 4)}-${String(chars, 4, 4)}"
    }

    fun normalizePairingCode(code: String): String {
        val clean = StringBuilder()
        for (c in code.uppercase()) {
            when (c) {
                '-', ' ', '\t' -> continue
                'O' -> clean.append('0')
                'I', 'L' -> clean.append('1')
                else -> clean.append(c)
            }
        }
        return clean.toString()
    }

    fun computeTranscriptHash(
        sessionId: String,
        initiatorDeviceId: String,
        responderDeviceId: String,
        initiatorPublicKey: ByteArray,
        responderPublicKey: ByteArray,
        initiatorTlsCertSha256: ByteArray,
        responderTlsCertSha256: ByteArray
    ): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(sessionId.toByteArray(Charsets.UTF_8))
        md.update(initiatorDeviceId.toByteArray(Charsets.UTF_8))
        md.update(responderDeviceId.toByteArray(Charsets.UTF_8))
        md.update(initiatorPublicKey)
        md.update(responderPublicKey)
        md.update(initiatorTlsCertSha256)
        md.update(responderTlsCertSha256)
        return md.digest()
    }

    fun computePairingProof(normalizedCode: String, transcriptHash: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(normalizedCode.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        return mac.doFinal(transcriptHash)
    }

    fun verifyPairingProof(
        normalizedCode: String,
        transcriptHash: ByteArray,
        candidateProof: ByteArray
    ): Boolean {
        val expected = computePairingProof(normalizedCode, transcriptHash)
        return MessageDigest.isEqual(expected, candidateProof)
    }
}
