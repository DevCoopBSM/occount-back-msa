package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequest

object ContributionWithdrawalRequestPersistenceMapper {
    fun toDomain(entity: ContributionWithdrawalRequestJpaEntity): ContributionWithdrawalRequest {
        return ContributionWithdrawalRequest(
            id = entity.id,
            userId = entity.userId,
            amount = entity.amount,
            bankName = entity.bankName,
            accountNumber = entity.accountNumber,
            accountHolder = entity.accountHolder,
            memo = entity.memo,
            status = entity.status,
            rejectionReason = entity.rejectionReason,
            requestedAt = entity.requestedAt,
            version = entity.version,
        )
    }

    fun toEntity(domain: ContributionWithdrawalRequest): ContributionWithdrawalRequestJpaEntity {
        return ContributionWithdrawalRequestJpaEntity(
            id = domain.id,
            userId = domain.userId,
            amount = domain.amount,
            bankName = domain.bankName,
            accountNumber = domain.accountNumber,
            accountHolder = domain.accountHolder,
            memo = domain.memo,
            status = domain.status,
            rejectionReason = domain.rejectionReason,
            requestedAt = domain.requestedAt,
            version = domain.version,
        )
    }
}
