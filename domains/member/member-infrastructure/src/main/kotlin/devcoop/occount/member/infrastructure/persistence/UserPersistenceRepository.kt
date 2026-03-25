package devcoop.occount.member.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserPersistenceRepository: JpaRepository<UserJpaEntity, Long> {
    fun findByUserInfoUserBarcode(userBarcode: String): UserJpaEntity?
    fun findByUserAccountInfoUserEmail(userEmail: String): UserJpaEntity?
    fun existsByUserAccountInfoUserEmail(userEmail: String): Boolean
}
