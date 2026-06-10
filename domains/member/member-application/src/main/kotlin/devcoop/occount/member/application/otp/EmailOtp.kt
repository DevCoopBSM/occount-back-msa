package devcoop.occount.member.application.otp

import java.time.Instant

data class EmailOtp(
    val email: String,
    val otpCode: String,
    val expiresAt: Instant,
    val purpose: OtpPurpose = OtpPurpose.REGISTER,
    val verified: Boolean = false,
    val failCount: Int = 0,
    val createdAt: Instant,
) {
    fun isExpired(now: Instant = Instant.now()): Boolean = now.isAfter(expiresAt)

    fun verify(): EmailOtp = copy(verified = true)

    fun incrementFailCount(): EmailOtp = copy(failCount = failCount + 1)

    fun isLocked(): Boolean = failCount >= MAX_FAIL_COUNT

    fun isRecentlySent(now: Instant = Instant.now()): Boolean = now.isBefore(createdAt.plusSeconds(RESEND_COOLDOWN_SECONDS))

    companion object {
        const val MAX_FAIL_COUNT = 5
        const val RESEND_COOLDOWN_SECONDS = 60L
        const val OTP_TTL_SECONDS = 5 * 60L
    }
}
