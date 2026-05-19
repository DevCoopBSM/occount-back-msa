package devcoop.occount.member.infrastructure.otp

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "email_otp")
class EmailOtpJpaEntity(
    @Id
    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val otpCode: String,

    @Column(nullable = false)
    val expiresAt: Instant,

    @Column(nullable = false)
    val verified: Boolean = false,

    @Column(nullable = false)
    val failCount: Int = 0,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
