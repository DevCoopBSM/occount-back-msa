package devcoop.occount.payment.infrastructure.wallet.persistence

import devcoop.occount.payment.domain.ChargeLog
import devcoop.occount.payment.domain.vo.PointTransaction
import devcoop.occount.payment.infrastructure.persistence.PointTransactionJpaEmbeddable

object ChargeLogPersistenceMapper {
    fun toDomain(entity: ChargeLogJpaEntity): ChargeLog {
        return ChargeLog(
            chargeId = entity.getChargeId(),
            userId = entity.getUserId(),
            chargeDate = entity.getChargeDate(),
            chargeAmount = entity.getChargeAmount(),
            paymentId = entity.getPaymentId(),
            pointTransaction = toDomainPointTransaction(entity.getPointTransaction()),
            reason = entity.getReason(),
        )
    }

    fun toEntity(domain: ChargeLog): ChargeLogJpaEntity {
        return ChargeLogJpaEntity(
            chargeId = domain.getChargeId(),
            userId = domain.getUserId(),
            paymentId = domain.getPaymentId(),
            chargeDate = domain.getChargeDate(),
            chargeAmount = domain.getChargeAmount(),
            pointTransaction = toEntityPointTransaction(domain.getPointTransaction()),
            reason = domain.getReason(),
        )
    }

    private fun toDomainPointTransaction(entity: PointTransactionJpaEmbeddable): PointTransaction {
        return PointTransaction(
            beforePoint = entity.getBeforePoint(),
            afterPoint = entity.getAfterPoint(),
        )
    }

    private fun toEntityPointTransaction(domain: PointTransaction): PointTransactionJpaEmbeddable {
        return PointTransactionJpaEmbeddable(
            beforePoint = domain.beforePoint(),
            afterPoint = domain.afterPoint(),
        )
    }
}
