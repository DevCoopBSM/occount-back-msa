package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequestStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContributionWithdrawalRequestPersistenceRepository : JpaRepository<ContributionWithdrawalRequestJpaEntity, Long> {
    fun findAllByOrderByRequestedAtDesc(pageable: Pageable): Page<ContributionWithdrawalRequestJpaEntity>
    fun findAllByStatusOrderByRequestedAtDesc(
        status: ContributionWithdrawalRequestStatus,
        pageable: Pageable,
    ): Page<ContributionWithdrawalRequestJpaEntity>
    fun findAllByUserIdOrderByRequestedAtDesc(userId: Long): List<ContributionWithdrawalRequestJpaEntity>
}
