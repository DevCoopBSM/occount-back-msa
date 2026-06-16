package devcoop.occount.investment.infrastructure.persistence

import devcoop.occount.investment.domain.investment.Investment

object InvestmentPersistenceMapper {
    fun toDomain(entity: InvestmentJpaEntity): Investment {
        return Investment(
            investmentId = entity.getInvestmentId(),
            userId = entity.getUserId(),
            paymentId = entity.getPaymentId(),
            amount = entity.getAmount(),
            type = entity.getType(),
            status = entity.getStatus(),
            depositDate = entity.getDepositDate(),
            conversionDate = entity.getConversionDate(),
            confirmMethod = entity.getConfirmMethod(),
        )
    }

    fun toEntity(domain: Investment): InvestmentJpaEntity {
        return InvestmentJpaEntity(
            investmentId = domain.investmentId,
            userId = domain.userId,
            paymentId = domain.paymentId,
            amount = domain.amount,
            type = domain.type,
            status = domain.status,
            depositDate = domain.depositDate,
            conversionDate = domain.conversionDate,
            confirmMethod = domain.confirmMethod,
        )
    }
}
