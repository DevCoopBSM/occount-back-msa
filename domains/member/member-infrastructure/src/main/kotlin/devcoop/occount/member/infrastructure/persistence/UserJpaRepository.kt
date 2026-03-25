package devcoop.occount.member.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    fun findByUserBarcode(userBarcode: String): UserJpaEntity?
    fun findByUserEmail(userEmail: String): UserJpaEntity?
    fun existsByUserEmail(userEmail: String): Boolean
}
