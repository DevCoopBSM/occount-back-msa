package devcoop.occount.member.infrastructure.otp

import devcoop.occount.member.application.otp.OtpPurpose
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val purpose: OtpPurpose = OtpPurpose.REGISTER,

    @Column(nullable = false)
    val verified: Boolean = false,

    @Column(nullable = false)
    val failCount: Int = 0,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) {
    protected constructor() : this(
        email = "",
        otpCode = "",
        expiresAt = Instant.EPOCH,
    )
}
