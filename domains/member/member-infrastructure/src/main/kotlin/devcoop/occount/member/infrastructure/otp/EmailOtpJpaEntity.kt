package devcoop.occount.member.infrastructure.otp

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "email_otp")
class EmailOtpJpaEntity(
    @Id
    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val otpCode: String,

    @Column(nullable = false)
    val expiresAt: LocalDateTime,

    @Column(nullable = false)
    val verified: Boolean = false,
)
