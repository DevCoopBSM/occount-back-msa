package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.domain.contribution.ContributionDepositRequestStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContributionDepositRequestPersistenceRepository : JpaRepository<ContributionDepositRequestJpaEntity, Long> {
    fun findAllByOrderByRequestedAtDesc(pageable: Pageable): Page<ContributionDepositRequestJpaEntity>
    fun findAllByStatusOrderByRequestedAtDesc(
        status: ContributionDepositRequestStatus,
        pageable: Pageable,
    ): Page<ContributionDepositRequestJpaEntity>
    fun findAllByUserIdOrderByRequestedAtDesc(userId: Long): List<ContributionDepositRequestJpaEntity>
}
