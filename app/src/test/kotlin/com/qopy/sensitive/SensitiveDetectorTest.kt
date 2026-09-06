package com.qopy.sensitive

import org.junit.Assert.*
import org.junit.Test

class SensitiveDetectorTest {

    @Test
    fun testPrivateKeyDetection() {
        val text = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA...\n-----END RSA PRIVATE KEY-----"
        val res = SensitiveDetector.detect(text)
        assertTrue(res.isSensitive)
        assertEquals("private_key", res.reason)
    }

    @Test
    fun testTokenDetection() {
        val text = "ghp_123456789012345678901234567890123456"
        val res = SensitiveDetector.detect(text)
        assertTrue(res.isSensitive)
        assertEquals("api_token", res.reason)
    }

    @Test
    fun testPasswordAssignment() {
        val text = "db_password = SuperSecret123!"
        val res = SensitiveDetector.detect(text)
        assertTrue(res.isSensitive)
        assertEquals("password_assignment", res.reason)
    }

    @Test
    fun testOtpDetection() {
        val text = "Your verification code is 492817."
        val res = SensitiveDetector.detect(text)
        assertTrue(res.isSensitive)
        assertEquals("otp_code", res.reason)
    }

    @Test
    fun testSafeText() {
        val text = "Hello world from Android test!"
        val res = SensitiveDetector.detect(text)
        assertFalse(res.isSensitive)
    }
}
