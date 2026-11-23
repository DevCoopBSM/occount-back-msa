package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.application.output.ContributionAccountRepository
import devcoop.occount.member.domain.contribution.ContributionAccount
import devcoop.occount.member.infrastructure.persistence.UserJpaRepository
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.domain.user.Role
import org.springframework.stereotype.Repository

@Repository
class ContributionAccountRepositoryImpl(
    private val contributionAccountPersistenceRepository: ContributionAccountPersistenceRepository,
    private val userJpaRepository: UserJpaRepository,
) : ContributionAccountRepository {
    override fun findByUserId(userId: Long): ContributionAccount? {
        return contributionAccountPersistenceRepository.findById(userId)
            .map(ContributionAccountPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun findByUserIdForUpdate(userId: Long): ContributionAccount? {
        userJpaRepository.findByIdForUpdate(userId) ?: throw UserNotFoundException()
        // A locking current read also sees accounts committed after a MySQL REPEATABLE READ snapshot.
        return contributionAccountPersistenceRepository.findByUserIdForUpdate(userId)
            ?.let(ContributionAccountPersistenceMapper::toDomain)
    }

    override fun promoteOwnerToMember(userId: Long) {
        userJpaRepository.promoteToMember(userId, Role.ROLE_USER, Role.ROLE_MEMBER)
    }

    override fun save(account: ContributionAccount): ContributionAccount {
        return contributionAccountPersistenceRepository
            .save(ContributionAccountPersistenceMapper.toEntity(account))
            .let(ContributionAccountPersistenceMapper::toDomain)
    }
}
