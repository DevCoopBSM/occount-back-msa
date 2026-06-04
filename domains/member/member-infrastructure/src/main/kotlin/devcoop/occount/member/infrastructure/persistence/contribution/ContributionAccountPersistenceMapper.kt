package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.domain.contribution.ContributionAccount

object ContributionAccountPersistenceMapper {
    fun toDomain(entity: ContributionAccountJpaEntity): ContributionAccount {
        return ContributionAccount(
            userId = entity.userId,
            balance = entity.balance,
            version = entity.version ?: 0L,
        )
    }

    fun toEntity(domain: ContributionAccount): ContributionAccountJpaEntity {
        return ContributionAccountJpaEntity(
            userId = domain.userId,
            balance = domain.balance,
            version = domain.version,
        )
    }
}
