package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.domain.contribution.ContributionDepositRequest

object ContributionDepositRequestPersistenceMapper {
    fun toDomain(entity: ContributionDepositRequestJpaEntity): ContributionDepositRequest {
        return ContributionDepositRequest(
            id = entity.id,
            userId = entity.userId,
            amount = entity.amount,
            depositorName = entity.depositorName,
            bankName = entity.bankName,
            proofImageUrl = entity.proofImageUrl,
            memo = entity.memo,
            status = entity.status,
            rejectionReason = entity.rejectionReason,
            requestedAt = entity.requestedAt,
            version = entity.version,
        )
    }

    fun toEntity(domain: ContributionDepositRequest): ContributionDepositRequestJpaEntity {
        return ContributionDepositRequestJpaEntity(
            id = domain.id,
            userId = domain.userId,
            amount = domain.amount,
            depositorName = domain.depositorName,
            bankName = domain.bankName,
            proofImageUrl = domain.proofImageUrl,
            memo = domain.memo,
            status = domain.status,
            rejectionReason = domain.rejectionReason,
            requestedAt = domain.requestedAt,
            version = domain.version,
        )
    }
}
