package com.qopy.crypto

import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.XSalsa20Engine
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.macs.Poly1305
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.MessageDigest
import java.security.SecureRandom

object CryptoEngine {
    private val secureRandom = SecureRandom()

    fun generateKeyPair(): Pair<ByteArray, ByteArray> {
        val gen = X25519KeyPairGenerator()
        gen.init(X25519KeyGenerationParameters(secureRandom))
        val pair = gen.generateKeyPair()
        val privateKey = (pair.private as X25519PrivateKeyParameters).encoded
        val publicKey = (pair.public as X25519PublicKeyParameters).encoded
        return Pair(privateKey, publicKey)
    }

    fun generateNonce(): ByteArray {
        val nonce = ByteArray(24)
        secureRandom.nextBytes(nonce)
        return nonce
    }

    fun computeSha256(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(data)
    }

    private fun computeSharedKey(privateKey: ByteArray, peerPublicKey: ByteArray): ByteArray {
        val privParams = X25519PrivateKeyParameters(privateKey, 0)
        val pubParams = X25519PublicKeyParameters(peerPublicKey, 0)
        val agreement = X25519Agreement()
        agreement.init(privParams)
        val secret = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(pubParams, secret, 0)

        // HSalsa20 key derivation for NaCl box:
        // Key is derived using HSalsa20(zero_16, secret, sigma)
        return hsalsa20(ByteArray(16), secret)
    }

    private val SIGMA = byteArrayOf(
        'e'.code.toByte(), 'x'.code.toByte(), 'p'.code.toByte(), 'a'.code.toByte(),
        'n'.code.toByte(), 'd'.code.toByte(), ' '.code.toByte(), '3'.code.toByte(),
        '2'.code.toByte(), '-'.code.toByte(), 'b'.code.toByte(), 'y'.code.toByte(),
        't'.code.toByte(), 'e'.code.toByte(), ' '.code.toByte(), 'k'.code.toByte()
    )

    private fun hsalsa20(input: ByteArray, key: ByteArray): ByteArray {
        val x = IntArray(16)
        // Constants & key unpacking
        unpackLittleEndian(SIGMA, 0, x, 0)
        unpackLittleEndian(key, 0, x, 1)
        unpackLittleEndian(key, 4, x, 2)
        unpackLittleEndian(key, 8, x, 3)
        unpackLittleEndian(key, 12, x, 4)
        unpackLittleEndian(SIGMA, 4, x, 5)
        unpackLittleEndian(input, 0, x, 6)
        unpackLittleEndian(input, 4, x, 7)
        unpackLittleEndian(input, 8, x, 8)
        unpackLittleEndian(input, 12, x, 9)
        unpackLittleEndian(SIGMA, 8, x, 10)
        unpackLittleEndian(key, 16, x, 11)
        unpackLittleEndian(key, 20, x, 12)
        unpackLittleEndian(key, 24, x, 13)
        unpackLittleEndian(key, 28, x, 14)
        unpackLittleEndian(SIGMA, 12, x, 15)

        for (i in 0 until 10) {
            // Column rounds & diagonal rounds (Salsa20 quarter rounds)
            quarterRound(x, 0, 4, 8, 12)
            quarterRound(x, 5, 9, 13, 1)
            quarterRound(x, 10, 14, 2, 6)
            quarterRound(x, 15, 3, 7, 11)

            quarterRound(x, 0, 1, 2, 3)
            quarterRound(x, 5, 6, 7, 4)
            quarterRound(x, 10, 11, 8, 9)
            quarterRound(x, 15, 12, 13, 14)
        }

        val out = ByteArray(32)
        packLittleEndian(x[0], out, 0)
        packLittleEndian(x[5], out, 4)
        packLittleEndian(x[10], out, 8)
        packLittleEndian(x[15], out, 12)
        packLittleEndian(x[6], out, 16)
        packLittleEndian(x[7], out, 20)
        packLittleEndian(x[8], out, 24)
        packLittleEndian(x[9], out, 28)
        return out
    }

    private fun quarterRound(x: IntArray, a: Int, b: Int, c: Int, d: Int) {
        x[b] = x[b] xor Integer.rotateLeft(x[a] + x[d], 7)
        x[c] = x[c] xor Integer.rotateLeft(x[b] + x[a], 9)
        x[d] = x[d] xor Integer.rotateLeft(x[c] + x[b], 13)
        x[a] = x[a] xor Integer.rotateLeft(x[d] + x[c], 18)
    }

    private fun unpackLittleEndian(src: ByteArray, srcOff: Int, dst: IntArray, dstOff: Int) {
        dst[dstOff] = (src[srcOff].toInt() and 0xFF) or
                ((src[srcOff + 1].toInt() and 0xFF) shl 8) or
                ((src[srcOff + 2].toInt() and 0xFF) shl 16) or
                ((src[srcOff + 3].toInt() and 0xFF) shl 24)
    }

    private fun packLittleEndian(value: Int, dst: ByteArray, dstOff: Int) {
        dst[dstOff] = value.toByte()
        dst[dstOff + 1] = (value ushr 8).toByte()
        dst[dstOff + 2] = (value ushr 16).toByte()
        dst[dstOff + 3] = (value ushr 24).toByte()
    }

    fun encryptBox(privateKey: ByteArray, peerPublicKey: ByteArray, nonce: ByteArray, plaintext: ByteArray): ByteArray {
        val sharedKey = computeSharedKey(privateKey, peerPublicKey)
        val engine = XSalsa20Engine()
        engine.init(true, ParametersWithIV(KeyParameter(sharedKey), nonce))

        // First 32 bytes of XSalsa20 keystream is used as Poly1305 key
        val polyKey = ByteArray(32)
        engine.processBytes(ByteArray(32), 0, 32, polyKey, 0)

        // Encrypt plaintext
        val ciphertext = ByteArray(plaintext.size)
        engine.processBytes(plaintext, 0, plaintext.size, ciphertext, 0)

        // Compute Poly1305 MAC over ciphertext
        val mac = Poly1305()
        mac.init(KeyParameter(polyKey))
        mac.update(ciphertext, 0, ciphertext.size)
        val tag = ByteArray(16)
        mac.doFinal(tag, 0)

        // Return Tag + Ciphertext (standard NaCl box wire format)
        val result = ByteArray(16 + ciphertext.size)
        System.arraycopy(tag, 0, result, 0, 16)
        System.arraycopy(ciphertext, 0, result, 16, ciphertext.size)
        return result
    }

    fun decryptBox(privateKey: ByteArray, peerPublicKey: ByteArray, nonce: ByteArray, ciphertextAndTag: ByteArray): ByteArray {
        if (ciphertextAndTag.size < 16) {
            throw IllegalArgumentException("Ciphertext too short")
        }

        val sharedKey = computeSharedKey(privateKey, peerPublicKey)
        val engine = XSalsa20Engine()
        engine.init(false, ParametersWithIV(KeyParameter(sharedKey), nonce))

        val polyKey = ByteArray(32)
        engine.processBytes(ByteArray(32), 0, 32, polyKey, 0)

        val tag = ByteArray(16)
        System.arraycopy(ciphertextAndTag, 0, tag, 0, 16)
        val ciphertext = ByteArray(ciphertextAndTag.size - 16)
        System.arraycopy(ciphertextAndTag, 16, ciphertext, 0, ciphertext.size)

        // Verify Poly1305 MAC
        val mac = Poly1305()
        mac.init(KeyParameter(polyKey))
        mac.update(ciphertext, 0, ciphertext.size)
        val expectedTag = ByteArray(16)
        mac.doFinal(expectedTag, 0)

        if (!MessageDigest.isEqual(tag, expectedTag)) {
            throw SecurityException("Poly1305 MAC verification failed")
        }

        val plaintext = ByteArray(ciphertext.size)
        engine.processBytes(ciphertext, 0, ciphertext.size, plaintext, 0)
        return plaintext
    }
}
