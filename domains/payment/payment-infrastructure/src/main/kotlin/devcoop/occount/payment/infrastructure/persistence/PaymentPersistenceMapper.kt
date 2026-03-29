package devcoop.occount.payment.infrastructure.persistence

import devcoop.occount.payment.domain.PaymentLog
import devcoop.occount.point.domain.vo.CardInfo
import devcoop.occount.point.domain.vo.PointTransaction
import devcoop.occount.point.domain.vo.TransactionInfo

object PaymentPersistenceMapper {
    fun toDomain(entity: PaymentLogJpaEntity): PaymentLog {
        return PaymentLog(
            paymentId = entity.getPaymentId(),
            userId = entity.getUserId(),
            paymentDate = entity.getPaymentDate(),
            paymentType = entity.getPaymentType(),
            totalAmount = entity.getTotalAmount(),
            pointTransaction = entity.getPointTransaction()?.let(::toDomainPointTransaction),
            cardInfo = entity.getCardInfo()?.let(::toDomainCardInfo),
            transactionInfo = entity.getTransactionInfo()?.let(::toDomainTransactionInfo),
            eventType = entity.getEventType(),
            refundState = entity.getRefundState(),
            refundDate = entity.getRefundDate(),
            refundRequesterId = entity.getRefundRequesterId(),
        )
    }

    fun toEntity(domain: PaymentLog): PaymentLogJpaEntity {
        return PaymentLogJpaEntity(
            paymentId = domain.getPaymentId(),
            userId = domain.getUserId(),
            paymentDate = domain.getPaymentDate(),
            paymentType = domain.getPaymentType(),
            totalAmount = domain.getTotalAmount(),
            pointTransaction = domain.getPointTransaction()?.let(::toEntityPointTransaction),
            cardInfo = domain.getCardInfo()?.let(::toEntityCardInfo),
            transactionInfo = domain.getTransactionInfo()?.let(::toEntityTransactionInfo),
            eventType = domain.getEventType(),
            refundState = domain.getRefundState(),
            refundDate = domain.getRefundDate(),
            refundRequesterId = domain.getRefundRequesterId(),
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

    private fun toDomainCardInfo(entity: CardInfoJpaEmbeddable): CardInfo {
        return CardInfo(
            issuerCode = entity.getIssuerCode(),
            issuerName = entity.getIssuerName(),
            acquirerCode = entity.getAcquirerCode(),
            acquirerName = entity.getAcquirerName(),
            cardType = entity.getCardType(),
            cardCategory = entity.getCardCategory(),
            cardName = entity.getCardName(),
            cardBrand = entity.getCardBrand(),
        )
    }

    private fun toEntityCardInfo(domain: CardInfo): CardInfoJpaEmbeddable {
        return CardInfoJpaEmbeddable(
            issuerCode = domain.issuerCode(),
            issuerName = domain.issuerName(),
            acquirerCode = domain.acquirerCode(),
            acquirerName = domain.acquirerName(),
            cardType = domain.cardType(),
            cardCategory = domain.cardCategory(),
            cardName = domain.cardName(),
            cardBrand = domain.cardBrand(),
        )
    }

    private fun toDomainTransactionInfo(entity: TransactionInfoJpaEmbeddable): TransactionInfo {
        return TransactionInfo(
            transactionId = entity.getTransactionId(),
            approvalNumber = entity.getApprovalNumber(),
            cardNumber = entity.getCardNumber(),
            amount = entity.getAmount(),
            installmentMonths = entity.getInstallmentMonths(),
            approvalDate = entity.getApprovalDate(),
            approvalTime = entity.getApprovalTime(),
            terminalId = entity.getTerminalId(),
            merchantNumber = entity.getMerchantNumber(),
        )
    }

    private fun toEntityTransactionInfo(domain: TransactionInfo): TransactionInfoJpaEmbeddable {
        return TransactionInfoJpaEmbeddable(
            transactionId = domain.transactionId(),
            approvalNumber = domain.approvalNumber(),
            cardNumber = domain.cardNumber(),
            amount = domain.amount(),
            installmentMonths = domain.installmentMonths(),
            approvalDate = domain.approvalDate(),
            approvalTime = domain.approvalTime(),
            terminalId = domain.terminalId(),
            merchantNumber = domain.merchantNumber(),
        )
    }
}
