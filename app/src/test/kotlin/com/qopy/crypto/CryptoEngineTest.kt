package com.qopy.crypto

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CryptoEngineTest {

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
    private fun unhex(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            result[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }

    @Test
    fun testNaclEncryptDecryptRoundtrip() {
        val alice = CryptoEngine.generateKeyPair()
        val bob = CryptoEngine.generateKeyPair()

        val plaintext = "Hello from Android Qopy!".toByteArray(Charsets.UTF_8)
        val nonce = CryptoEngine.generateNonce()

        // Alice encrypts for Bob
        val ciphertext = CryptoEngine.encryptBox(alice.first, bob.second, nonce, plaintext)

        // Bob decrypts from Alice
        val decrypted = CryptoEngine.decryptBox(bob.first, alice.second, nonce, ciphertext)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun testHashVectorsConformance() {
        val fixturesFile = File("../../protocol/fixtures/hash_vectors.json")
        if (!fixturesFile.exists()) return

        val jsonStr = fixturesFile.readText()
        val array = JSONArray(jsonStr)

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val input = obj.getString("input").toByteArray(Charsets.UTF_8)
            val expectedHash = obj.getString("sha256_hex")

            val actualHash = hex(CryptoEngine.computeSha256(input))
            assertEquals("Hash mismatch for case $i", expectedHash, actualHash)
        }
    }

    @Test
    fun testPairingVectorsConformance() {
        val fixturesFile = File("../../protocol/fixtures/pairing_vectors.json")
        if (!fixturesFile.exists()) return

        val jsonStr = fixturesFile.readText()
        val array = JSONArray(jsonStr)

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val rawCode = obj.getString("raw_code")
            val expectedNorm = obj.getString("normalized_code")
            val sessionId = obj.getString("session_id")
            val devA = obj.getString("device_id_a")
            val devB = obj.getString("device_id_b")
            val pkA = unhex(obj.getString("public_key_a_hex"))
            val pkB = unhex(obj.getString("public_key_b_hex"))
            val certA = unhex(obj.getString("tls_cert_a_sha256_hex"))
            val certB = unhex(obj.getString("tls_cert_b_sha256_hex"))
            val expectedProof = obj.getString("expected_proof_hex")

            val norm = PairingCrypto.normalizePairingCode(rawCode)
            assertEquals("Normalized code mismatch", expectedNorm, norm)

            val transcriptHash = PairingCrypto.computeTranscriptHash(
                sessionId, devA, devB, pkA, pkB, certA, certB
            )
            val proof = hex(PairingCrypto.computePairingProof(norm, transcriptHash))
            assertEquals("Proof mismatch", expectedProof, proof)

            assertTrue(PairingCrypto.verifyPairingProof(norm, transcriptHash, unhex(proof)))
        }
    }
}
