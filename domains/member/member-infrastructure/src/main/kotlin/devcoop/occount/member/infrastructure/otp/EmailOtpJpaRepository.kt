package devcoop.occount.member.infrastructure.otp

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface EmailOtpJpaRepository : JpaRepository<EmailOtpJpaEntity, String> {
    fun findByEmail(email: String): EmailOtpJpaEntity?
    fun deleteByEmail(email: String)
    fun findByEmailAndExpiresAtAfter(email: String, now: Instant): EmailOtpJpaEntity?
}
