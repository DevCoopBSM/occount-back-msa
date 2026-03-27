package devcoop.occount.payment.application.payment

import devcoop.occount.payment.application.dto.response.CardResult
import devcoop.occount.payment.application.dto.response.TransactionResult
import devcoop.occount.payment.domain.ChargeLog
import devcoop.occount.payment.domain.PaymentLog
import devcoop.occount.payment.domain.type.EventType
import devcoop.occount.payment.domain.type.PaymentType
import devcoop.occount.payment.domain.type.RefundState
import devcoop.occount.payment.domain.vo.PointTransaction

object PaymentMapper {
    fun toChargeLog(
        user: PaymentUserInfo,
        chargeAmount: Int,
        pointChange: PointBalanceChange,
        cardResult: CardResult?,
        transactionResult: TransactionResult?,
    ): ChargeLog {
        return ChargeLog(
            userId = user.userId,
            chargeAmount = chargeAmount,
            pointTransaction = toPointTransaction(pointChange),
            cardInfo = cardResult?.let(CardResult::toDomain),
            transactionInfo = transactionResult?.let(TransactionResult::toDomain),
            managedEmail = user.email,
            refundState = RefundState.NONE,
        )
    }

    fun toPointPaymentLog(
        user: PaymentUserInfo,
        paymentDetails: PaymentDetails,
        pointChange: PointBalanceChange,
    ): PaymentLog {
        return PaymentLog(
            userId = user.userId,
            paymentType = PaymentType.POINT,
            totalAmount = paymentDetails.totalAmount,
            pointTransaction = toPointTransaction(pointChange),
            managedEmail = user.email,
            eventType = EventType.NONE,
        )
    }

    fun toMixedPaymentLog(
        user: PaymentUserInfo,
        paymentDetails: PaymentDetails,
        pointChange: PointBalanceChange,
        cardResult: CardResult?,
        transactionResult: TransactionResult?,
    ): PaymentLog {
        return PaymentLog(
            userId = user.userId,
            paymentType = PaymentType.MIXED,
            totalAmount = paymentDetails.totalAmount,
            pointTransaction = toPointTransaction(pointChange),
            cardInfo = cardResult?.let(CardResult::toDomain),
            transactionInfo = transactionResult?.let(TransactionResult::toDomain),
            managedEmail = user.email,
            eventType = EventType.NONE,
        )
    }

    fun toPointTransaction(change: PointBalanceChange): PointTransaction {
        return PointTransaction(
            beforePoint = change.beforeBalance,
            transactionPoint = change.changedAmount,
            afterPoint = change.afterBalance,
        )
    }
}
