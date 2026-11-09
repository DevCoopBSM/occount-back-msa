package devcoop.occount.member.infrastructure.persistence.contribution

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContributionAccountPersistenceRepository : JpaRepository<ContributionAccountJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ContributionAccountJpaEntity a where a.userId = :userId")
    fun findByUserIdForUpdate(@Param("userId") userId: Long): ContributionAccountJpaEntity?
}
