package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.application.output.ContributionTransactionRepository
import devcoop.occount.member.domain.contribution.ContributionTransaction
import org.springframework.stereotype.Repository

@Repository
class ContributionTransactionRepositoryImpl(
    private val contributionTransactionPersistenceRepository: ContributionTransactionPersistenceRepository,
) : ContributionTransactionRepository {
    override fun save(transaction: ContributionTransaction): ContributionTransaction {
        return contributionTransactionPersistenceRepository
            .save(ContributionTransactionPersistenceMapper.toEntity(transaction))
            .let(ContributionTransactionPersistenceMapper::toDomain)
    }

    override fun findAllByUserId(userId: Long): List<ContributionTransaction> {
        return contributionTransactionPersistenceRepository.findAllByUserIdOrderByOccurredAtDesc(userId)
            .map(ContributionTransactionPersistenceMapper::toDomain)
    }
}
