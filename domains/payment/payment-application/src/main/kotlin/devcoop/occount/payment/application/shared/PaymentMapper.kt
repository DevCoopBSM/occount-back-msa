package devcoop.occount.payment.application.shared

import devcoop.occount.payment.application.dto.response.CardResult
import devcoop.occount.payment.application.dto.response.TransactionResult
import devcoop.occount.payment.domain.PaymentLog
import devcoop.occount.payment.domain.type.EventType
import devcoop.occount.payment.domain.type.PaymentType
import devcoop.occount.point.domain.vo.PointTransaction

object PaymentMapper {
    fun toPointPaymentLog(
        userId: Long,
        paymentDetails: PaymentDetails,
        pointChange: PointBalanceChange,
    ): PaymentLog {
        return PaymentLog(
            userId = userId,
            paymentType = PaymentType.POINT,
            totalAmount = paymentDetails.totalAmount,
            pointTransaction = toPointTransaction(pointChange),
            eventType = EventType.NONE,
        )
    }

    fun toMixedPaymentLog(
        userId: Long,
        paymentDetails: PaymentDetails,
        pointChange: PointBalanceChange,
        cardResult: CardResult?,
        transactionResult: TransactionResult?,
    ): PaymentLog {
        return PaymentLog(
            userId = userId,
            paymentType = PaymentType.MIXED,
            totalAmount = paymentDetails.totalAmount,
            pointTransaction = toPointTransaction(pointChange),
            cardInfo = cardResult?.let(CardResult::toDomain),
            transactionInfo = transactionResult?.let(TransactionResult::toDomain),
            eventType = EventType.NONE,
        )
    }

    fun toPointTransaction(change: PointBalanceChange): PointTransaction {
        return PointTransaction(
            beforePoint = change.beforeBalance,
            afterPoint = change.afterBalance,
        )
    }
}
