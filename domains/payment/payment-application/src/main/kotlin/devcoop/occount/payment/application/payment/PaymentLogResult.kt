package devcoop.occount.payment.application.payment

import devcoop.occount.payment.application.dto.response.CardResult
import devcoop.occount.payment.application.dto.response.TransactionResult
import devcoop.occount.payment.domain.PaymentLog
import devcoop.occount.payment.domain.type.EventType
import devcoop.occount.payment.domain.type.PaymentType
import java.time.LocalDateTime

data class PaymentLogResult(
    val paymentId: Long,
    val userId: Long,
    val paymentDate: LocalDateTime,
    val paymentType: PaymentType,
    val totalAmount: Int,
    val pointTransaction: PointTransactionResult?,
    val cardInfo: CardResult?,
    val transactionInfo: TransactionResult?,
    val managedEmail: String?,
    val eventType: EventType?,
) {
    companion object {
        fun from(paymentLog: PaymentLog): PaymentLogResult {
            return PaymentLogResult(
                paymentId = paymentLog.getPaymentId(),
                userId = paymentLog.getUserId(),
                paymentDate = paymentLog.getPaymentDate(),
                paymentType = paymentLog.getPaymentType(),
                totalAmount = paymentLog.getTotalAmount(),
                pointTransaction = paymentLog.getPointTransaction()?.let(PointTransactionResult::from),
                cardInfo = paymentLog.getCardInfo()?.let(CardResult::from),
                transactionInfo = paymentLog.getTransactionInfo()?.let(TransactionResult::from),
                managedEmail = paymentLog.getManagedEmail(),
                eventType = paymentLog.getEventType(),
            )
        }
    }
}
