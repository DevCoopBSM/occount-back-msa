package devcoop.occount.inquiry.domain.email

import java.time.LocalDateTime

class EmailVerificationCode(
    private val id: Long = 0L,
    val email: String,
    val code: String,
    val status: EmailVerificationStatus = EmailVerificationStatus.PENDING,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val expiresAt: LocalDateTime = LocalDateTime.now().plusMinutes(5),
) {
    fun getId(): Long = id

    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun isVerified(): Boolean = status == EmailVerificationStatus.VERIFIED

    fun verify(): EmailVerificationCode = EmailVerificationCode(
        id = id,
        email = email,
        code = code,
        status = EmailVerificationStatus.VERIFIED,
        createdAt = createdAt,
        expiresAt = expiresAt,
    )

    fun expire(): EmailVerificationCode = EmailVerificationCode(
        id = id,
        email = email,
        code = code,
        status = EmailVerificationStatus.EXPIRED,
        createdAt = createdAt,
        expiresAt = expiresAt,
    )
}
