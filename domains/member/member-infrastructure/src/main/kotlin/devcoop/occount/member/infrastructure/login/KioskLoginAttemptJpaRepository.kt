package devcoop.occount.member.infrastructure.login

import org.springframework.data.jpa.repository.JpaRepository

interface KioskLoginAttemptJpaRepository : JpaRepository<KioskLoginAttemptJpaEntity, String> {
    fun findByUserBarcode(userBarcode: String): KioskLoginAttemptJpaEntity?
}
