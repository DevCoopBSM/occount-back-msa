package devcoop.occount.member.infrastructure.otp

import org.springframework.data.jpa.repository.JpaRepository

interface EmailOtpJpaRepository : JpaRepository<EmailOtpJpaEntity, String> {
    fun findByEmail(email: String): EmailOtpJpaEntity?
    fun deleteByEmail(email: String)
}
