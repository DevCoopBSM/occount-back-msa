package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.domain.contribution.ContributionTransaction

object ContributionTransactionPersistenceMapper {
    fun toDomain(entity: ContributionTransactionJpaEntity): ContributionTransaction {
        return ContributionTransaction(
            id = entity.id,
            userId = entity.userId,
            type = entity.type,
            amount = entity.amount,
            beforeBalance = entity.beforeBalance,
            afterBalance = entity.afterBalance,
            sourceRequestId = entity.sourceRequestId,
            occurredAt = entity.occurredAt,
        )
    }

    fun toEntity(domain: ContributionTransaction): ContributionTransactionJpaEntity {
        return ContributionTransactionJpaEntity(
            id = domain.id,
            userId = domain.userId,
            type = domain.type,
            amount = domain.amount,
            beforeBalance = domain.beforeBalance,
            afterBalance = domain.afterBalance,
            sourceRequestId = domain.sourceRequestId,
            occurredAt = domain.occurredAt,
        )
    }
}
