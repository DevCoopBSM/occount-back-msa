package devcoop.occount.inquiry.domain.password

import java.time.LocalDateTime

class PasswordResetToken(
    private val id: Long = 0L,
    val email: String,
    val token: String,
    val used: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val expiresAt: LocalDateTime = LocalDateTime.now().plusMinutes(5),
) {
    fun getId(): Long = id

    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun use(): PasswordResetToken = PasswordResetToken(
        id = id,
        email = email,
        token = token,
        used = true,
        createdAt = createdAt,
        expiresAt = expiresAt,
    )
}
