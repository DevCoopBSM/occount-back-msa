package devcoop.occount.payment.infrastructure.persistence.chargelog

import devcoop.occount.payment.domain.wallet.ChargeLog
import devcoop.occount.payment.domain.wallet.PointTransaction

object ChargeLogPersistenceMapper {
    fun toDomain(entity: ChargeLogJpaEntity): ChargeLog {
        return ChargeLog(
            chargeId = entity.getChargeId(),
            userId = entity.getUserId(),
            chargeDate = entity.getChargeDate(),
            paymentId = entity.getPaymentId(),
            pointTransaction = toDomainPointTransaction(entity.getPointTransaction()),
            detailReason = entity.getReason(),
        )
    }

    fun toEntity(domain: ChargeLog): ChargeLogJpaEntity {
        return ChargeLogJpaEntity(
            chargeId = domain.chargeId,
            userId = domain.userId,
            paymentId = domain.paymentId,
            chargeDate = domain.chargeDate,
            pointTransaction = toEntityPointTransaction(domain.pointTransaction),
            reason = domain.detailReason,
        )
    }

    private fun toDomainPointTransaction(entity: PointTransactionJpaEmbeddable): PointTransaction {
        return PointTransaction(
            beforePoint = entity.getBeforePoint(),
            changeAmount = entity.getChangePoint(),
            afterPoint = entity.getAfterPoint(),
        )
    }

    private fun toEntityPointTransaction(domain: PointTransaction): PointTransactionJpaEmbeddable {
        return PointTransactionJpaEmbeddable(
            beforePoint = domain.beforePoint,
            changeAmount = domain.changeAmount,
            afterPoint = domain.afterPoint,
        )
    }
}
