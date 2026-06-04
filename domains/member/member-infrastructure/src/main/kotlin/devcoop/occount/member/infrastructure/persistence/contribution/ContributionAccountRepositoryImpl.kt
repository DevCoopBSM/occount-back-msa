package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.application.output.ContributionAccountRepository
import devcoop.occount.member.domain.contribution.ContributionAccount
import org.springframework.stereotype.Repository

@Repository
class ContributionAccountRepositoryImpl(
    private val contributionAccountPersistenceRepository: ContributionAccountPersistenceRepository,
) : ContributionAccountRepository {
    override fun findByUserId(userId: Long): ContributionAccount? {
        return contributionAccountPersistenceRepository.findById(userId)
            .map(ContributionAccountPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun save(account: ContributionAccount): ContributionAccount {
        return contributionAccountPersistenceRepository
            .save(ContributionAccountPersistenceMapper.toEntity(account))
            .let(ContributionAccountPersistenceMapper::toDomain)
    }
}
