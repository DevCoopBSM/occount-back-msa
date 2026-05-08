package devcoop.occount.member.application.otp

import java.time.LocalDateTime

data class EmailOtp(
    val email: String,
    val otpCode: String,
    val expiresAt: LocalDateTime,
    val verified: Boolean = false,
) {
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun verify(): EmailOtp = copy(verified = true)
}
