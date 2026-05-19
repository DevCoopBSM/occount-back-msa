package devcoop.occount.member.infrastructure.otp

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface EmailOtpJpaRepository : JpaRepository<EmailOtpJpaEntity, String> {
    fun findByEmail(email: String): EmailOtpJpaEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EmailOtpJpaEntity e WHERE e.email = :email")
    fun findByEmailForUpdate(email: String): EmailOtpJpaEntity?

    fun deleteByEmail(email: String)
    fun findByEmailAndExpiresAtAfter(email: String, now: Instant): EmailOtpJpaEntity?
}
