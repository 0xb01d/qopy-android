package com.qopy.sensitive

import java.util.regex.Pattern

data class SensitiveResult(
    val isSensitive: Boolean,
    val reason: String? = null
)

object SensitiveDetector {
    private val PRIVATE_KEY_PATTERN = Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----")
    private val TOKEN_PATTERN = Pattern.compile("(?i)\\b(ghp_[A-Za-z0-9_]{36}|gho_[A-Za-z0-9_]{36}|AKIA[0-9A-Z]{16}|eyJh[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+)\\b")
    private val PASSWORD_PATTERN = Pattern.compile("(?i)(?:^|[\\s_.-])(password|passwd|secret|apikey|api_key)\\s*[:=]\\s*\\S+")
    private val OTP_PATTERN = Pattern.compile("(?i)\\b(code|otp|2fa|verification|auth|pin)\\b[^\\d\\n]{1,15}\\b(\\d{6,8})\\b")
    private val CC_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b")

    fun isLuhnValid(digits: String): Boolean {
        val clean = digits.filter { it.isDigit() }.map { it.digitToInt() }
        if (clean.size < 13 || clean.size > 19) return false

        var sum = 0
        var double = false
        for (i in clean.indices.reversed()) {
            val digit = clean[i]
            if (double) {
                val d = digit * 2
                sum += if (d > 9) d - 9 else d
            } else {
                sum += digit
            }
            double = !double
        }
        return sum % 10 == 0
    }

    fun detect(text: String): SensitiveResult {
        val sample = if (text.length > 8192) text.substring(0, 8192) else text

        if (PRIVATE_KEY_PATTERN.matcher(sample).find()) {
            return SensitiveResult(true, "private_key")
        }
        if (TOKEN_PATTERN.matcher(sample).find()) {
            return SensitiveResult(true, "api_token")
        }
        if (PASSWORD_PATTERN.matcher(sample).find()) {
            return SensitiveResult(true, "password_assignment")
        }
        if (OTP_PATTERN.matcher(sample).find()) {
            return SensitiveResult(true, "otp_code")
        }

        val ccMatcher = CC_PATTERN.matcher(sample)
        while (ccMatcher.find()) {
            val candidate = ccMatcher.group()
            if (isLuhnValid(candidate)) {
                return SensitiveResult(true, "credit_card")
            }
        }

        return SensitiveResult(false, null)
    }

    fun maskPreview(text: String): String = "••••••••••••••••"
}
