package devcoop.occount.payment.application.shared

import devcoop.occount.payment.application.dto.response.CardResult
import devcoop.occount.payment.application.dto.response.TransactionResult
import devcoop.occount.payment.domain.payment.PaymentLog
import devcoop.occount.payment.domain.payment.EventType
import devcoop.occount.payment.domain.payment.PaymentType
import devcoop.occount.payment.domain.wallet.PointTransaction

object PaymentMapper {
    fun toPointPaymentLog(
        userId: Long,
        paymentDetails: PaymentDetails,
        pointTransaction: PointTransaction,
    ): PaymentLog {
        return PaymentLog(
            userId = userId,
            paymentType = PaymentType.POINT,
            totalAmount = paymentDetails.totalAmount,
            pointTransaction = pointTransaction,
            eventType = EventType.NONE,
        )
    }

    fun toMixedPaymentLog(
        userId: Long,
        paymentDetails: PaymentDetails,
        pointTransaction: PointTransaction,
        cardResult: CardResult?,
        transactionResult: TransactionResult?,
    ): PaymentLog {
        return PaymentLog(
            userId = userId,
            paymentType = PaymentType.MIXED,
            totalAmount = paymentDetails.totalAmount,
            pointTransaction = pointTransaction,
            cardInfo = cardResult?.let(CardResult::toDomain),
            transactionInfo = transactionResult?.let(TransactionResult::toDomain),
            eventType = EventType.NONE,
        )
    }
}
