package devcoop.occount.member.infrastructure.persistence.contribution

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContributionTransactionPersistenceRepository : JpaRepository<ContributionTransactionJpaEntity, Long> {
    fun findAllByUserIdOrderByOccurredAtDesc(userId: Long): List<ContributionTransactionJpaEntity>
}
