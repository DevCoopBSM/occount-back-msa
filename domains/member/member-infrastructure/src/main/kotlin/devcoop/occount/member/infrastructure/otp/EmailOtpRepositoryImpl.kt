package devcoop.occount.member.infrastructure.otp

import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.output.EmailOtpRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class EmailOtpRepositoryImpl(
    private val emailOtpJpaRepository: EmailOtpJpaRepository,
) : EmailOtpRepository {

    override fun save(emailOtp: EmailOtp): EmailOtp {
        return emailOtpJpaRepository.save(emailOtp.toEntity()).toDomain()
    }

    override fun findByEmail(email: String): EmailOtp? {
        return emailOtpJpaRepository.findByEmail(email)?.toDomain()
    }

    override fun findByEmailForUpdate(email: String): EmailOtp? {
        return emailOtpJpaRepository.findByEmailForUpdate(email)?.toDomain()
    }

    override fun findValidByEmail(email: String): EmailOtp? {
        return emailOtpJpaRepository.findByEmailAndExpiresAtAfter(email, Instant.now())?.toDomain()
    }

    override fun deleteByEmail(email: String) {
        emailOtpJpaRepository.deleteByEmail(email)
    }

    private fun EmailOtp.toEntity() = EmailOtpJpaEntity(
        email = email,
        otpCode = otpCode,
        expiresAt = expiresAt,
        verified = verified,
        failCount = failCount,
        createdAt = createdAt,
    )

    private fun EmailOtpJpaEntity.toDomain() = EmailOtp(
        email = email,
        otpCode = otpCode,
        expiresAt = expiresAt,
        verified = verified,
        failCount = failCount,
        createdAt = createdAt,
    )
}
