package devcoop.occount.member.infrastructure.persistence.contribution

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContributionAccountPersistenceRepository : JpaRepository<ContributionAccountJpaEntity, Long>
